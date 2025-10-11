package com.example.startuppulse;

import java.io.Serializable;

public class Mentor implements Serializable {

    private String id;
    private String ownerId;
    private String nome;
    private String profissao;
    private String cidade;
    private String estado;
    private String imagem;

    private double latitude;
    private double longitude;

    private boolean verificado;                 // badge
    private java.util.List<String> areas;       // áreas de atuação

    private boolean ativoPublico;

    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean v) { this.verificado = v; }

    public java.util.List<String> getAreas() { return areas; }
    public void setAreas(java.util.List<String> a) { this.areas = a; }


    public Mentor() {}

    // --- Getters e Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getProfissao() { return profissao; }
    public void setProfissao(String profissao) { this.profissao = profissao; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    // --- NOVO GETTER E SETTER ---
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getImagem() { return imagem; }
    public void setImagem(String imagem) { this.imagem = imagem; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public boolean isAtivoPublico() {
        return ativoPublico;
    }

    public void setAtivoPublico(boolean ativoPublico) {
        this.ativoPublico = ativoPublico;
    }
}