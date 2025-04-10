package com.nikao.rag.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ElementCollection;
import java.util.List;

@Entity
public class Embedding {
    @Id
    private int hash;

    @ElementCollection
    private List<Float> vector;

    public Embedding(int hash, List<Float> vector) {
        this.hash = hash;
        this.vector = vector;
    }

    public int getHash() {
        return hash;
    }

    public List<Float> getVector() {
        return vector;
    }
}