package com.vincent.learning.token.service;

import com.vincent.learning.token.config.CertificationMonitor;
import com.vincent.learning.token.config.IamProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import java.io.File;
import javax.net.ssl.SSLException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Service
@Slf4j
public class WebClientFactory {
    @Value("${iam.client.connection.timeout.ms:5000}")
    private int iamClientConnectionTimeout;

    @Value("${iam.client.read.timeout.ms:5000}")
    private int iamClientReadTimeout;

    @Value("${iam.client.write.timeout.ms:5000}")
    private int iamClientWriteTimeout;

    @Value("${pm.codec.max-in-memory-size:1MB}")
    private DataSize maxInMemorySize;

    @Autowired private IamProperties iamProperties;

    @Autowired private CertificationMonitor certificationMonitor;

    private boolean isClientAu = false;
    private String clientPrivateKeyPath;

    private WebClient webClient;
    @Getter private boolean isRefreshTrigger;

    @PostConstruct
    public void init() throws SSLException {
        webClient = iamClient();
        if (isClientAu) {
            certificationMonitor.setMonitorPath(new File(clientPrivateKeyPath));
            certificationMonitor.startMonitor(this::refresh);
        }
    }

    public WebClient getWebClient() {
        return webClient;
    }

    public void refresh() {
        try {
            webClient = iamClient();
            isRefreshTrigger = true;
        } catch (Exception e) {
            log.error("fail to refresh webclient", e);
        }
    }

    public WebClient iamClient() throws SSLException {
        String iamUrl = iamProperties.getHost() + ":" + iamProperties.getPort();
        String protocol = "http://";
        String tlsEnable = iamProperties.getTlsEnable();
        if (!StringUtils.hasText(tlsEnable) || tlsEnable.equalsIgnoreCase("enabled")) {
            protocol = "https://";
        }
        iamUrl = protocol + iamUrl + iamProperties.getRootPath();

        if (protocol.equals("https://")) {
            String clientTrustCA = iamProperties.getClientTrustCa();
            String clientCert = iamProperties.getClientCert();
            if (StringUtils.hasText(clientTrustCA)) {
                if (StringUtils.hasText(clientCert) && clientCert.split(",").length > 1) {
                    String[] clientCertArray = clientCert.split(",");
                    SslContext sslContext =
                            SslContextBuilder.forClient()
                                    .keyManager(
                                            new File(clientCertArray[0]),
                                            new File(clientCertArray[1]))
                                    .trustManager(new File(clientTrustCA))
                                    .build();
                    isClientAu = true;
                    clientPrivateKeyPath = clientCertArray[1];
                    return getWebClient(sslContext, iamUrl, maxInMemorySize);
                } else {
                    SslContext sslContext =
                            SslContextBuilder.forClient()
                                    .trustManager(new File(clientTrustCA))
                                    .build();
                    return getWebClient(sslContext, iamUrl, maxInMemorySize);
                }
            }
        }

        return getWebClient(iamUrl, maxInMemorySize);
    }

    private WebClient getWebClient(String url, DataSize maxInMemorySize) throws SSLException {
        SslContext sslContext =
                SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
        return getWebClient(sslContext, url, maxInMemorySize);
    }

    private WebClient getWebClient(SslContext sslContext, String url, DataSize maxInMemorySize) {

        HttpClient httpClient =
                HttpClient.create()
                        .wiretap(true)
                        .secure(sslProviderBuilder -> sslProviderBuilder.sslContext(sslContext));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(
                        httpHeaders -> {
                            httpHeaders.add(
                                    HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                            httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                .baseUrl(url)
                .codecs(
                        configurer ->
                                configurer
                                        .defaultCodecs()
                                        .maxInMemorySize(
                                                Math.toIntExact(maxInMemorySize.toBytes())))
                .build();
    }
}
