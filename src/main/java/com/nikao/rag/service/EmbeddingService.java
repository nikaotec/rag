package com.nikao.rag.service;

import com.nikao.rag.model.Embedding;
import com.nikao.rag.repository.EmbeddingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

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
                        .bodyToMono(new ParameterizedTypeReference<List<Float>>() {
                        })
                        .doOnNext(vec -> repository.save(new Embedding(text, vec)));
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

   // public Mono<List<ScoredChunk>> rankChunks(List<String> chunks, String prompt) {
    //     return embed(prompt)
    //             .flatMapMany(promptVector -> Flux.fromIterable(chunks)
    //                     .flatMap(chunk -> embed(chunk).map(
    //                             chunkVector -> new ScoredChunk(chunk, cosineSimilarity(
    //                                     promptVector.stream().mapToDouble(Float::doubleValue).toArray(),
    //                                     chunkVector.stream().mapToDouble(Float::doubleValue).toArray())))))
    //             .doOnNext(chunk -> {
    //                 System.out.println("🧩 CHUNK SCORE: " + String.format("%.4f", chunk.score()));
    //                 System.out.println(chunk.text().substring(0, Math.min(chunk.text().length(), 300)));
    //                 System.out.println("-----");
    //             })
    //             .filter(chunk -> chunk.score() > 0.5f) // mais permissivo
    //             .sort(Comparator.comparing(ScoredChunk::score).reversed())
    //             .take(5)
    //             .collectList();
    // }

    // public record ScoredChunk(String text, float score) {
    // }
}
