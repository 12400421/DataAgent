package com.jiayi.dataagent.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("mysql")
public class AiModelConfiguration {

    @Bean
    public AiModelSettings aiModelSettings() {
        return new AiModelSettings(
                System.getenv("DATAAGENT_AI_API_KEY"),
                System.getenv("DATAAGENT_AI_MODEL"),
                System.getenv("DATAAGENT_AI_BASE_URL"));
    }

    @Bean("aiRestClientBuilder")
    public RestClient.Builder aiRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
