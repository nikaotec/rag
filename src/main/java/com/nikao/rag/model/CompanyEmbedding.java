package com.nikao.rag.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.List;
import jakarta.persistence.ElementCollection;

@Entity
public class CompanyEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long embeddingId;

    @Column(name = "company_id")
    private Long companyId;

    @Lob
    @Column(name = "texto")
    private String texto;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(columnDefinition = "real[]")
    private List<Float> vector;

    private int hash;

    public CompanyEmbedding() {
    }

    public CompanyEmbedding(Long companyId, String texto, List<Float> vector) {
        this.companyId = companyId;
        this.texto = texto;
        this.vector = vector;
        this.hash = texto.hashCode();
    }

    public CompanyEmbedding(String texto, List<Float> vector) {
        this.texto = texto;
        this.vector = vector;
        this.hash = texto.hashCode();
    }

    public Long getEmbeddingId() {
        return embeddingId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
}