package com.example.startuppulse;

import java.io.Serializable;

/**
 * Representa um membro da equipe da startup.
 * Construtor vazio é necessário para o Firestore.
 */
public class MembroEquipe implements Serializable {

    private String nome;
    private String funcao; // Ex: "CEO & Co-founder", "CTO", "Lead Designer"
    private String linkedinUrl;
    private String fotoUrl; // Opcional

    public MembroEquipe() {
        // Construtor vazio obrigatório para o Firestore
    }

    // --- Getters e Setters ---
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getFuncao() { return funcao; }
    public void setFuncao(String funcao) { this.funcao = funcao; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}