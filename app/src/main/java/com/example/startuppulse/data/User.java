package com.example.startuppulse.data;

public class User {

    private String nome;
    private String email;
    private String fotoUrl;
    private boolean isPremium;
    private String validadePlano; // String já formatada "dd/MM/yyyy"
    private int publicadasCount;
    private int seguindoCount;
    private long diasDeConta;

    // Getters
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getFotoUrl() { return fotoUrl; }
    public boolean isPremium() { return isPremium; }
    public String getValidadePlano() { return validadePlano; }
    public int getPublicadasCount() { return publicadasCount; }
    public int getSeguindoCount() { return seguindoCount; }
    public long getDiasDeConta() { return diasDeConta; }

    // Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public void setValidadePlano(String validadePlano) { this.validadePlano = validadePlano; }
    public void setPublicadasCount(int publicadasCount) { this.publicadasCount = publicadasCount; }
    public void setSeguindoCount(int seguindoCount) { this.seguindoCount = seguindoCount; }
    public void setDiasDeConta(long diasDeConta) { this.diasDeConta = diasDeConta; }
}