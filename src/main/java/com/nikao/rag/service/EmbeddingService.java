package com.nikao.rag.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmbeddingService {

    private final WebClient embeddingClient;

    public EmbeddingService(@Value("${huggingface.embedding.api.url}") String url,
            @Value("${huggingface.api.key}") String key) {
        this.embeddingClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();
    }

    public Mono<List<Float>> embed(String text) {
        return embeddingClient.post()
                .bodyValue(Map.of("inputs", List.of(text)))
                // ✅ Correção aqui: "inputs", não "parameters"
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Erro 4xx: " + errorBody))))
                .bodyToMono(new ParameterizedTypeReference<List<List<Float>>>() {
                })
                .map(response -> response.get(0)); // retorna o vetor de embedding
    }

    private float cosineSimilarity(List<Float> a, List<Float> b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public Mono<List<ScoredChunk>> rankChunks(List<String> chunks, String prompt) {
        return embed(prompt)
                .flatMapMany(promptEmbedding -> Flux.fromIterable(chunks)
                        .flatMap(chunk -> embed(chunk)
                                .map(chunkEmbedding -> {
                                    float similarity = cosineSimilarity(promptEmbedding, chunkEmbedding);
                                    return new ScoredChunk(chunk, similarity);
                                })))
                .sort(Comparator.comparing(ScoredChunk::score).reversed())
                .collectList();
    }

    public record ScoredChunk(String text, float score) {
    }
}
