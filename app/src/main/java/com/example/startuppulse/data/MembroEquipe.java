package com.example.startuppulse.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa um membro da equipe da startup.
 * Contém um ID único para melhor performance com ListAdapter/DiffUtil
 * e métodos equals/hashCode implementados.
 */
public class MembroEquipe implements Serializable {

    // MODIFICAÇÃO 1: Adicionar um ID único e estável.
    // Isso é crucial para o DiffUtil saber se um item é o mesmo, mesmo que seus dados mudem.
    private String id;

    private String nome;
    private String funcao; // Ex: "CEO & Co-founder", "CTO", "Lead Designer"
    private String linkedinUrl;
    private String fotoUrl; // Opcional

    public MembroEquipe() {
        // Construtor vazio para o Firestore. Gera um ID se não houver um.
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * MODIFICAÇÃO 2: Construtor corrigido e completo.
     * @param nome Nome do membro.
     * @param funcao Função do membro.
     * @param linkedinUrl URL do perfil no LinkedIn.
     */
    public MembroEquipe(String nome, String funcao, String linkedinUrl) {
        this.id = UUID.randomUUID().toString(); // Garante que cada novo membro tenha um ID único.
        this.nome = nome;
        this.funcao = funcao;
        this.linkedinUrl = linkedinUrl;
    }

    // --- Getters e Setters ---
    public String getId() { return id; }
    // O ID é interno e não deve ser alterado publicamente, então não há setId().

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getFuncao() { return funcao; }
    public void setFuncao(String funcao) { this.funcao = funcao; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }


    /**
     * MODIFICAÇÃO 3: Implementação de equals() e hashCode().
     * Essencial para o DiffUtil.areContentsTheSame() e para o comportamento
     * correto em coleções como HashSets e HashMaps.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembroEquipe that = (MembroEquipe) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(nome, that.nome) &&
                Objects.equals(funcao, that.funcao) &&
                Objects.equals(linkedinUrl, that.linkedinUrl) &&
                Objects.equals(fotoUrl, that.fotoUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome, funcao, linkedinUrl, fotoUrl);
    }
}