package com.example.startuppulse;

import java.io.Serializable;

public class CanvasEtapa implements Serializable {
    private String key;
    private String titulo;
    private String descricao;
    private int iconeResId;

    public CanvasEtapa(String key, String titulo, String descricao, int iconeResId) {
        this.key = key;
        this.titulo = titulo;
        this.descricao = descricao;
        this.iconeResId = iconeResId;
    }

    // Getters
    public String getKey() { return key; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public int getIconeResId() { return iconeResId; }
}