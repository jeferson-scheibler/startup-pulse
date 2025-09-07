package com.example.startuppulse;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

/**
 * Representa uma métrica de tração da startup em um determinado ponto no tempo.
 * Ex: "Usuários Ativos Mensais", "Receita Mensal Recorrente (MRR)".
 */
public class Metrica implements Serializable {

    private String nome;        // Ex: "MRR"
    private double valor;       // Ex: 1500.00
    private String unidade;     // Ex: "BRL", "Usuários"
    private Date dataRegistro;

    public Metrica() {
        // Construtor vazio obrigatório para o Firestore
    }

    // --- Getters e Setters ---
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public Date getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(Date dataRegistro) { this.dataRegistro = dataRegistro; }
}