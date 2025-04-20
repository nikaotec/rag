package com.nikao.rag.service;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Service
public class ImageGenerator {

    private final WebClient webClient;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ImageGenerator.class);

    public ImageGenerator(@Value("${huggingface.image.api.url}") String apiUrl,
                          @Value("${huggingface.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public Mono<byte[]> generateImage(String prompt) {
        Map<String, String> requestBody = Collections.singletonMap("inputs", prompt);

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(20))
                .doOnError(e -> logger.error("Error generating image", e))
                .onErrorResume(e -> Mono.empty());
    }
}
