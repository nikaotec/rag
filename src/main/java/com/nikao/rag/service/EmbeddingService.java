package com.nikao.rag.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nikao.rag.model.Embedding; // Ensure this is the correct package for the Embedding class
import com.nikao.rag.repository.EmbeddingRepository; // Import the EmbeddingRepository

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmbeddingService {

    private final WebClient embeddingClient;
    private final EmbeddingRepository repository;

    public EmbeddingService(@Value("${huggingface.embedding.api.url}") String url,
            @Value("${huggingface.api.key}") String key, 
            EmbeddingRepository repository) {
                this.repository = repository;
        this.embeddingClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();
    }

    public Mono<List<Float>> embed(String text) {
        int hash = text.hashCode();

        return Mono.defer(() -> {
            Optional<Embedding> cached = repository.findByHash(hash);
            if (cached.isPresent()) {
                return Mono.just(cached.get().getVector());
            }

        // logger.info("Enviando para Hugging Face: {}", Map.of("inputs", List.of(text)));

        return embeddingClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("inputs", List.of(text)))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Erro 4xx: " + body))))
                .onStatus(status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Erro 5xx: " + body))))
                .bodyToMono(new ParameterizedTypeReference<List<List<Float>>>() {
                })
                .map(vec -> vec.get(0)) // Extract the first list of floats
                .doOnNext(vec -> repository.save(new Embedding(hash, vec)))
                .onErrorResume(e -> {
                    // Handle errors gracefully
                    return Mono.empty();
                });
        });
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
