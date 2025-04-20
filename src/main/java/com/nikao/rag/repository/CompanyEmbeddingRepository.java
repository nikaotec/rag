package com.nikao.rag.repository;

import com.nikao.rag.model.CompanyEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyEmbeddingRepository extends JpaRepository<CompanyEmbedding, Long> {
    Optional<CompanyEmbedding> findByHashAndCompanyId(int hash, Long companyId);
}