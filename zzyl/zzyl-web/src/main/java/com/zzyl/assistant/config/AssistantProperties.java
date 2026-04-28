package com.zzyl.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zzyl.ai")
public class AssistantProperties {

    private String baseUrl = "http://127.0.0.1:8000";

    private String internalApiKey = "zzyl-internal";

    private Integer connectTimeoutMillis = 3000;

    private Integer readTimeoutMillis = 30000;

    private Integer maxRetries = 1;
}
