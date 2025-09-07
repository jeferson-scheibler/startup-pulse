package com.example.startuppulse;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ideia implements Serializable {

    private String id;
    private String nome;
    private String descricao;
    private String ownerId;
    private String autorNome;
    private boolean autorIsPremium;
    private String status;
    private String mentorId;
    private String avaliacaoStatus;
    private List<AvaliacaoCompleta> avaliacoes;
    private List<String> areasNecessarias;

    private Date timestamp;
    private Map<String, List<PostIt>> postIts;

    private List<MembroEquipe> equipe;
    private List<Metrica> metricas;
    private String pitchDeckUrl;

    public Ideia() {
        postIts = new HashMap<>();
        avaliacoes = new ArrayList<>();
        areasNecessarias = new ArrayList<>();
        this.equipe = new ArrayList<>();
        this.metricas = new ArrayList<>();
        this.status = "RASCUNHO";
        this.avaliacaoStatus = "Pendente";
    }

    // --- Getters e Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getAutorNome() { return autorNome; }
    public void setAutorNome(String autorNome) { this.autorNome = autorNome; }
    public boolean isAutorIsPremium() { return autorIsPremium; }
    public void setAutorIsPremium(boolean autorIsPremium) { this.autorIsPremium = autorIsPremium; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
    public String getAvaliacaoStatus() { return avaliacaoStatus; }
    public void setAvaliacaoStatus(String avaliacaoStatus) { this.avaliacaoStatus = avaliacaoStatus; }
    public List<AvaliacaoCompleta> getAvaliacoes() { return avaliacoes; }
    public void setAvaliacoes(List<AvaliacaoCompleta> avaliacoes) { this.avaliacoes = avaliacoes; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public Map<String, List<PostIt>> getPostIts() { return postIts; }
    public void setPostIts(Map<String, List<PostIt>> postIts) { this.postIts = postIts; }
    public List<String> getAreasNecessarias() { return areasNecessarias; }
    public List<MembroEquipe> getEquipe() {
        return equipe;
    }
    public void setEquipe(List<MembroEquipe> equipe) {
        this.equipe = equipe;
    }
    public List<Metrica> getMetricas() {
        return metricas;
    }
    public void setMetricas(List<Metrica> metricas) {
        this.metricas = metricas;
    }
    public String getPitchDeckUrl() {
        return pitchDeckUrl;
    }
    public void setPitchDeckUrl(String pitchDeckUrl) {
        this.pitchDeckUrl = pitchDeckUrl;
    }
    public void setAreasNecessarias(List<String> areasNecessarias) {
        this.areasNecessarias = areasNecessarias != null ? areasNecessarias : new ArrayList<>();
    }
    public List<PostIt> getPostItsPorChave(String etapaChave) {
        if (postIts == null) {
            return new ArrayList<>();
        }
        List<PostIt> result = postIts.get(etapaChave);
        return result != null ? result : new ArrayList<>();
    }
}