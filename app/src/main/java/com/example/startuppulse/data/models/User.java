package com.example.startuppulse.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {

    private String nome;
    private String email;
    private String bio;
    @PropertyName("foto_perfil")
    private String fotoUrl;
    private boolean isPremium;
    private String validadePlano;
    private int publicadasCount;
    private int seguindoCount;
    private long diasDeConta;
    private String plano;

    @PropertyName("data_fim")
    private Date dataExpiracaoPlano;
    private String purchaseToken;
    @ServerTimestamp
    private Date dataCriacao;
    private Timestamp ultimoAcesso;
    private Long diasAcessoTotal;

    private String profissao;
    private String linkedinUrl;
    private List<String> areasDeInteresse;
    private String status = "ativo";

    private boolean profilePublic = true;

    public User() {
        this.areasDeInteresse = new ArrayList<>();
        this.areasDeInteresse = new ArrayList<>();
        this.diasAcessoTotal = 0L;
    }

    // Getters
    public String getNome() { return nome; }
    public String getBio() {
        return bio;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmail() { return email; }
    @PropertyName("foto_perfil")
    public String getFotoUrl() { return fotoUrl; }

    public boolean isPremium() { return isPremium; }
    public String getValidadePlano() { return validadePlano; }
    public int getPublicadasCount() { return publicadasCount; }
    public int getSeguindoCount() { return seguindoCount; }
    public long getDiasDeConta() { return diasDeConta; }
    public boolean isProfilePublic() { return profilePublic; }
    public void setProfilePublic(boolean profilePublic) { this.profilePublic = profilePublic; }

    // Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setBio(String bio) { this.bio = bio; }
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

    public String getProfissao() {
        return profissao;
    }

    public void setProfissao(String profissao) {
        this.profissao = profissao;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public List<String> getAreasDeInteresse() {
        // Garante que nunca retorne nulo para evitar NullPointerException
        if (areasDeInteresse == null) {
            return new ArrayList<>();
        }
        return areasDeInteresse;
    }

    public void setAreasDeInteresse(List<String> areasDeInteresse) {
        this.areasDeInteresse = areasDeInteresse;
    }

    public Timestamp getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(Timestamp ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }

    public Long getDiasAcessoTotal() {
        // Garante que retorne 0 se for nulo
        return (diasAcessoTotal != null) ? diasAcessoTotal : 0L;
    }

    public void setDiasAcessoTotal(Long diasAcessoTotal) {
        this.diasAcessoTotal = diasAcessoTotal;
    }
}