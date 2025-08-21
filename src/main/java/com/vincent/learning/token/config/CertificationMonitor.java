package com.vincent.learning.token.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Data
@Slf4j
public class CertificationMonitor {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private int checkIntervalMillis = 1000;
    private File monitorPath;

    public void startMonitor(Runnable callback) {
        if (!monitorPath.isFile()) {
            log.info("As monitor path is not file, certification monitor will not start.");
            return;
        }

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path lcPath = monitorPath.getParentFile().toPath();
            List<WatchKey> keys = new ArrayList<>();
            registerRecursive(lcPath, watcher, keys);
            this.scheduler.scheduleAtFixedRate(
                    () -> {
                        AtomicBoolean isChange = new AtomicBoolean(false);
                        keys.forEach(
                                key -> {
                                    if (!key.pollEvents().isEmpty()) {
                                        isChange.set(true);
                                    }
                                });
                        if (isChange.get()) {
                            log.info("Certification is changed");
                            callback.run();
                        }
                    },
                    this.checkIntervalMillis,
                    this.checkIntervalMillis,
                    TimeUnit.MILLISECONDS);

            log.info("Certification monitor is start");
        } catch (IOException ex) {
            log.error("Cannot monitor target path, failed to start certification monitor.", ex);
        }
    }

    private void registerRecursive(final Path root, WatchService watcher, List<WatchKey> keys)
            throws IOException {
        Files.walkFileTree(
                root,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        keys.add(
                                dir.register(
                                        watcher,
                                        StandardWatchEventKinds.ENTRY_CREATE,
                                        StandardWatchEventKinds.ENTRY_MODIFY));
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
