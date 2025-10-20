package com.example.startuppulse.data.models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User {

    private String nome;
    private String email;
    @PropertyName("foto_perfil")
    private String fotoUrl;
    private boolean isPremium;
    private String validadePlano;
    private int publicadasCount;
    private int seguindoCount;
    private long diasDeConta;
    private String plano;
    private Date dataExpiracaoPlano;
    private String purchaseToken;
    @ServerTimestamp
    private Date dataCriacao;

    // Getters
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    @PropertyName("foto_perfil")
    public String getFotoUrl() { return fotoUrl; }
    public boolean isPremium() { return isPremium; }
    public String getValidadePlano() { return validadePlano; }
    public int getPublicadasCount() { return publicadasCount; }
    public int getSeguindoCount() { return seguindoCount; }
    public long getDiasDeConta() { return diasDeConta; }

    // Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    @PropertyName("foto_perfil")
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public void setValidadePlano(String validadePlano) { this.validadePlano = validadePlano; }
    public void setPublicadasCount(int publicadasCount) { this.publicadasCount = publicadasCount; }
    public void setSeguindoCount(int seguindoCount) { this.seguindoCount = seguindoCount; }
    public void setDiasDeConta(long diasDeConta) { this.diasDeConta = diasDeConta; }

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }

    public Date getDataExpiracaoPlano() {
        return dataExpiracaoPlano;
    }

    public void setDataExpiracaoPlano(Date dataExpiracaoPlano) {
        this.dataExpiracaoPlano = dataExpiracaoPlano;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean isPro() {
        return "pro".equals(plano) && (dataExpiracaoPlano == null || dataExpiracaoPlano.after(new Date()));
    }
}