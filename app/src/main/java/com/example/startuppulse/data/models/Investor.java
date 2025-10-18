package com.example.startuppulse.data;

import java.io.Serializable;
import java.util.List;

public class Investor implements Serializable { // Serializable para passar entre activities
    private String id;
    private String nome;
    private String fotoUrl;
    private String bio;
    private String tese;
    private List<String> estagios; // Ex: "Anjo", "Pré-Seed"
    private List<String> areas;    // Ex: "Fintech", "SaaS"
    private String linkedinUrl;

    // Construtor vazio necessário para o Firestore
    public Investor() {}

    // Getters
    public String getId() { return id; }
    public String setId() { return id; }
    public String getNome() { return nome; }
    public String getFotoUrl() { return fotoUrl; }
    public String getBio() { return bio; }
    public String getTese() { return tese; }
    public List<String> getEstagios() { return estagios; }
    public List<String> getAreas() { return areas; }
    public String getLinkedinUrl() { return linkedinUrl; }
}