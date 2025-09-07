package com.example.startuppulse;

// Classe para encapsular os dados de prontidão de uma ideia.
public class ReadinessData {

    private final int score;
    private final boolean isCanvasCompleto;
    private final boolean isEquipeDefinida;
    private final boolean isMetricasIniciais;
    private final boolean isValidadoPorMentor;
    private final boolean hasPitchDeck;

    public ReadinessData(int score, boolean isCanvasCompleto, boolean isEquipeDefinida, boolean isMetricasIniciais, boolean isValidadoPorMentor, boolean hasPitchDeck) {
        this.score = score;
        this.isCanvasCompleto = isCanvasCompleto;
        this.isEquipeDefinida = isEquipeDefinida;
        this.isMetricasIniciais = isMetricasIniciais;
        this.isValidadoPorMentor = isValidadoPorMentor;
        this.hasPitchDeck = hasPitchDeck;
    }

    // Getters para todos os campos...
    public int getScore() { return score; }
    public boolean isCanvasCompleto() { return isCanvasCompleto; }
    public boolean isEquipeDefinida() { return isEquipeDefinida; }
    public boolean isMetricasIniciais() { return isMetricasIniciais; }
    public boolean isValidadoPorMentor() { return isValidadoPorMentor; }
    public boolean hasPitchDeck() { return hasPitchDeck; }
}