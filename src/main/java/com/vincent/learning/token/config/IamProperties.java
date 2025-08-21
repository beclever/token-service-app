package com.vincent.learning.token.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "iam")
@Setter
@Getter
public class IamProperties {
    private String tlsEnable;
    private String host;
    private int port;
    private String clientId;
    private String clientCert;
    private String clientTrustCa;
    private String rootPath;
}
