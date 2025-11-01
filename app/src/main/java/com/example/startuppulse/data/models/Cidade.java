package com.example.startuppulse.data.models;
public class Cidade {
    private int id;
    private String nome;
    public String getNome() { return nome; }
    @Override public String toString() { return nome; } // Importante para o ArrayAdapter
}