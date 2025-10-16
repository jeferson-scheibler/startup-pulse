package com.example.startuppulse.data;
public class Estado {
    private int id;
    private String sigla;
    private String nome;
    public String getSigla() { return sigla; }
    public String getNome() { return nome; }
    @Override public String toString() { return nome; } // Importante para o ArrayAdapter
}