package com.nikao.rag.repository;

import com.nikao.rag.model.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {
    Optional<Embedding> findByHash(int hash);
}
