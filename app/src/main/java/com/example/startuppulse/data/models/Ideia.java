package com.example.startuppulse.data.models;

import com.example.startuppulse.data.Avaliacao;
import com.example.startuppulse.data.MembroEquipe;
import com.example.startuppulse.data.Metrica;
import com.example.startuppulse.data.PostIt;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ideia implements Serializable {

    /**
     * Enum para representar os diferentes estágios do ciclo de vida de uma ideia.
     * Usar um Enum em vez de Strings previne erros e torna o código mais claro.
     */
    public enum Status {
        RASCUNHO,         // Apenas o dono pode ver e editar
        EM_AVALIACAO,     // Publicada e aguardando o feedback do mentor
        AVALIADA_APROVADA,// Avaliada com nota positiva, desbloqueia a fase de tração
        AVALIADA_REPROVADA // Avaliada com nota baixa, necessita de revisão
    }

    private String id;
    private String nome;
    private String descricao;
    private String ownerId;
    private String autorNome;
    private boolean autorIsPremium;
    private String mentorId;
    private String avaliacaoStatus;
    private List<Avaliacao> avaliacoes; // << ALTERAÇÃO CRÍTICA APLICADA AQUI
    private List<String> areasNecessarias;
    private String matchmakingLog;
    private Status status;

    @ServerTimestamp
    private Date timestamp;
    private Map<String, List<PostIt>> postIts;
    private List<MembroEquipe> equipe;
    private List<Metrica> metricas;
    private String pitchDeckUrl;
    private boolean prontaParaInvestidores = false;
    private Date ultimaBuscaMentorTimestamp;

    public Ideia() {
        this.postIts = new HashMap<>();
        this.avaliacoes = new ArrayList<>();
        this.areasNecessarias = new ArrayList<>();
        this.equipe = new ArrayList<>();
        this.metricas = new ArrayList<>();

        this.status = Status.RASCUNHO;

        this.avaliacaoStatus = "Pendente";
    }

    // --- Getters e Setters ---

    // Getter e Setter para o novo Enum de Status
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

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
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
    public String getAvaliacaoStatus() { return avaliacaoStatus; }
    public void setAvaliacaoStatus(String avaliacaoStatus) { this.avaliacaoStatus = avaliacaoStatus; }
    public List<Avaliacao> getAvaliacoes() { return avaliacoes; } // << ALTERAÇÃO CRÍTICA APLICADA AQUI
    public void setAvaliacoes(List<Avaliacao> avaliacoes) { this.avaliacoes = avaliacoes; } // << ALTERAÇÃO CRÍTICA APLICADA AQUI
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public Map<String, List<PostIt>> getPostIts() { return postIts; }
    public void setPostIts(Map<String, List<PostIt>> postIts) { this.postIts = postIts; }
    public List<String> getAreasNecessarias() { return areasNecessarias; }
    public void setAreasNecessarias(List<String> areasNecessarias) { this.areasNecessarias = areasNecessarias; }
    public List<MembroEquipe> getEquipe() { return equipe; }
    public void setEquipe(List<MembroEquipe> equipe) { this.equipe = equipe; }
    public List<Metrica> getMetricas() { return metricas; }
    public void setMetricas(List<Metrica> metricas) { this.metricas = metricas; }
    public String getPitchDeckUrl() { return pitchDeckUrl; }
    public void setPitchDeckUrl(String pitchDeckUrl) { this.pitchDeckUrl = pitchDeckUrl; }
    public boolean isProntaParaInvestidores() {
        return prontaParaInvestidores;
    }
    public Date getUltimaBuscaMentorTimestamp() {
        return ultimaBuscaMentorTimestamp;
    }

    public void setUltimaBuscaMentorTimestamp(Date ultimaBuscaMentorTimestamp) {
        this.ultimaBuscaMentorTimestamp = ultimaBuscaMentorTimestamp;
    }
    public void setProntaParaInvestidores(boolean prontaParaInvestidores) {
        this.prontaParaInvestidores = prontaParaInvestidores;
    }
    public List<PostIt> getPostItsPorChave(String etapaChave) {
        if (postIts == null) {
            return new ArrayList<>();
        }
        List<PostIt> result = postIts.get(etapaChave);
        return result != null ? result : new ArrayList<>();
    }

    public String getMatchmakingLog() { return matchmakingLog; }
    public void setMatchmakingLog(String matchmakingLog) { this.matchmakingLog = matchmakingLog; }
}
