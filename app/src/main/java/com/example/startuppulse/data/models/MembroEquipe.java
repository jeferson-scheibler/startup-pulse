package com.example.startuppulse.data.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa um membro da equipe da startup.
 * Contém um ID único para melhor performance com ListAdapter/DiffUtil
 * e métodos equals/hashCode implementados.
 */
public class MembroEquipe implements Serializable {

    private String id;
    private String nome;
    private String funcao;
    private String linkedinUrl;
    private String fotoUrl;
    private String userId; // ADICIONADO: relaciona o membro ao usuário do Firebase Auth

    public MembroEquipe() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public MembroEquipe(String nome, String funcao, String linkedinUrl) {
        this(); // Garante que o ID esteja definido
        this.nome = nome;
        this.funcao = funcao;
        this.linkedinUrl = linkedinUrl;
    }

    /**
     * Construtor com userId (recomendado quando o membro é um usuário real)
     */
    public MembroEquipe(String nome, String funcao, String linkedinUrl, String userId) {
        this(nome, funcao, linkedinUrl);
        this.userId = userId;
    }

    // --- Getters e Setters ---
    public String getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getFuncao() { return funcao; }
    public void setFuncao(String funcao) { this.funcao = funcao; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembroEquipe that = (MembroEquipe) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(nome, that.nome) &&
                Objects.equals(funcao, that.funcao) &&
                Objects.equals(linkedinUrl, that.linkedinUrl) &&
                Objects.equals(fotoUrl, that.fotoUrl) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome, funcao, linkedinUrl, fotoUrl, userId);
    }
}
