package com.nikao.rag.service;

import com.nikao.rag.model.CompanyEmbedding;
import com.nikao.rag.repository.CompanyEmbeddingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class EmbeddingService {
    private final WebClient embeddingClient;
    private final CompanyEmbeddingRepository repository;

    public EmbeddingService(@Value("${huggingface.embedding.api.url}") String url,
            @Value("${huggingface.api.key}") String key,
            CompanyEmbeddingRepository repository) {
        this.repository = repository;
        this.embeddingClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();
    }
    
    public Mono<List<Float>> embed(String text, Long companyId) {    
        int hash = text.hashCode();

        return Mono.defer(() -> {
           Optional<CompanyEmbedding> cached = repository.findByHashAndCompanyId(hash, companyId);
           if (cached.isPresent()) {
                return Mono.just(cached.get().getVector());
            } else {
                return embeddingClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("inputs", text))
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.is4xxClientError(),
                                res -> res.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(new RuntimeException("Erro 4xx: " + body))))
                        .onStatus(httpStatus -> httpStatus.is5xxServerError(),
                                res -> res.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(new RuntimeException("Erro 5xx: " + body))))
                        .bodyToMono(new ParameterizedTypeReference<List<Float>>() {} )
                        .doOnNext(vec -> repository.save(new CompanyEmbedding(text, vec, hash, companyId)));
            }
        });
    }

    public double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
