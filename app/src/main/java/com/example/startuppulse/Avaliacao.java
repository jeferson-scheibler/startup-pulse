package com.example.startuppulse;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Avaliacao implements Serializable {
    private String criterio;
    private double nota;
    private String feedback;

    // Construtor vazio necessário para o Firestore
    public Avaliacao() {}

    public Avaliacao(String criterio, double nota, String feedback) {
        this.criterio = criterio;
        this.nota = nota;
        this.feedback = feedback;
    }

    // Getters e Setters com anotações para garantir o mapeamento correto
    @PropertyName("criterio")
    public String getCriterio() { return criterio; }

    @PropertyName("criterio")
    public void setCriterio(String criterio) { this.criterio = criterio; }

    @PropertyName("nota")
    public double getNota() { return nota; }

    @PropertyName("nota")
    public void setNota(double nota) { this.nota = nota; }

    @PropertyName("justificativa")
    public String getFeedback() { return feedback; }

    @PropertyName("justificativa")
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
