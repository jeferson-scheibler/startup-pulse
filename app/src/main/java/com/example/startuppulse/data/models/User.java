package com.example.startuppulse.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Modelo de dados para um Usuário. (AJUSTADO)
 * Contém todas as informações compartilhadas e de usuário.
 * A flag "isMentor" indica se existe um documento correspondente na coleção /mentores.
 */
public class User {
    @Exclude
    private String id;
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
    private String profissao; // Equivalente ao "headline" do mentor
    private String linkedinUrl;
    @PropertyName("areas")
    private List<String> areasAtuacao;
    private String status = "ativo";
    private boolean profilePublic = true;
    @PropertyName("fcmToken")
    private String fcmToken;

    // CAMPO ADICIONADO: Flag de conexão com o perfil de Mentor
    @PropertyName("isMentor")
    private boolean isMentor;

    @Exclude
    private Mentor mentorData;

    public User() {
        // Corrigido: removida inicialização duplicada de areasAtuacao
        this.areasAtuacao = new ArrayList<>();
        this.diasAcessoTotal = 0L;
        this.isMentor = false; // Valor padrão
    }

    // --- Getters e Setters (Existentes) ---

    @Exclude
    public String getId() {
        return id;
    }
    @Exclude
    public void setId(String id) {
        this.id = id;
    }
    public String getNome() { return nome; }
    public String getBio() {
        return bio;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmail() { return email; }
    @PropertyName("fotoUrl")
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
    @PropertyName("fotoUrl")
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public void setValidadePlano(String validadePlano) { this.validadePlano = validadePlano; }
    public void setPublicadasCount(int publicadasCount) { this.publicadasCount = publicadasCount; }
    public void setSeguindoCount(int seguindoCount) { this.seguindoCount = seguindoCount; }
    public void setDiasDeConta(long diasDeConta) { this.diasDeConta = diasDeConta; }

    @Exclude
    public Mentor getMentorData() {
        return mentorData;
    }

    @Exclude
    public void setMentorData(Mentor mentorData) {
        this.mentorData = mentorData;
    }

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

    @PropertyName("areas")
    public List<String> getAreasDeInteresse() {
        // Garante que nunca retorne nulo para evitar NullPointerException
        if (areasAtuacao == null) {
            return new ArrayList<>();
        }
        return areasAtuacao;
    }
    @PropertyName("areas")
    public void setAreasDeInteresse(List<String> areasDeInteresse) {
        this.areasAtuacao = areasDeInteresse;
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
    @PropertyName("fcmToken")
    public String getFcmToken() {
        return fcmToken;
    }

    @PropertyName("fcmToken")
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    // --- Getter e Setter para o novo campo ISMENTOR ---

    @PropertyName("isMentor")
    public boolean isMentor() {
        return isMentor;
    }

    @PropertyName("isMentor")
    public void setMentor(boolean mentor) {
        isMentor = mentor;
    }
}