package com.nikao.rag.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Embedding {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob // ⬅️ ESSENCIAL para textos grandes
    @Column(name = "texto")
    private String texto;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Float> vector;

    private int hash;

    public Embedding() {}

    public Embedding(String texto, List<Float> vector) {
        this.texto = texto;
        this.vector = vector;
        this.hash = texto.hashCode();
    }


    // ✅ Getters e Setters obrigatórios
    public Long getId() {
        return id;
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
