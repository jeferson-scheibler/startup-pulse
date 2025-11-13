package com.example.startuppulse.data.models;

import java.io.Serializable;

/**
 * POJO para representar uma "Faísca" (Spark) vinda do Vórtex.
 * Esta é uma classe leve, contendo apenas dados públicos.
 */
public class Spark implements Serializable {

    private String id;
    private String text;
    private double lat;
    private double lng;
    private int votos;

    public Spark() {
        // Construtor vazio para desserialização
    }

    // Getters
    public String getId() { return id; }
    public String getText() { return text; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public int getVotos() { return votos; }

    // Setters (úteis para o mapeamento do Firebase)
    public void setId(String id) { this.id = id; }
    public void setText(String text) { this.text = text; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }
    public void setVotos(int votos) { this.votos = votos; }
}