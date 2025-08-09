package com.example.startuppulse;

import java.io.Serializable;

public class Avaliacao implements Serializable {
    private String criterio;
    private double nota; // Usar double para compatibilidade com o Firestore
    private String feedback;

    // Construtor vazio necessário para o Firestore
    public Avaliacao() {}

    public Avaliacao(String criterio, double nota, String feedback) {
        this.criterio = criterio;
        this.nota = nota;
        this.feedback = feedback;
    }

    // Getters e Setters
    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }

    public double getNota() { return nota; }
    public void setNota(double nota) { this.nota = nota; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}