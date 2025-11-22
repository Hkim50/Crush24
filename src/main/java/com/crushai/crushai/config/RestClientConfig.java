package com.crushai.crushai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("nominatimRestClient")
    public RestClient nominatimRestClient() {
        return RestClient.builder()
                .build();
    }
}
