package com.example.startuppulse.ui.avaliacao;

/**
 * Modelo de dados para a UI da tela de avaliação.
 * Representa um único critério a ser avaliado pelo mentor.
 */
public class CriterioAvaliacao {

    public final String nome;
    public final String descricao;
    public String feedback = "";
    public float nota = 5.0f; // Nota padrão

    public CriterioAvaliacao(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}