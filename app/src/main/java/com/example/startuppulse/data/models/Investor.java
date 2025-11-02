package com.example.startuppulse.data.models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Investor implements Serializable { // Serializable para passar entre activities
    // --- Identificação ---
    private String id;              // ID do Documento no Firestore
    private String investorType;    // NOVO: "INDIVIDUAL" ou "FIRM" (Pessoa Física ou Jurídica)
    private String nome;            // Nome do Anjo ou Nome Fantasia da Empresa
    private String fotoUrl;         // Foto do Anjo ou Logo da Empresa
    private String status;          // NOVO: "PENDING_APPROVAL", "ACTIVE", "REJECTED" (Para nosso controle)

    // --- Contato e Links ---
    private String emailContato;    // NOVO: Email principal (pode ser o de login ou não)
    private String linkedinUrl;     //
    private String siteUrl;         // NOVO: Site oficial (para empresas)

    // --- Tese de Investimento ---
    private String bio;             //
    private String tese;            //
    private List<String> estagios;  // Ex: "Anjo", "Pré-Seed"
    private List<String> areas;     // Ex: "Fintech", "SaaS"
    private String ticketMedio;     // NOVO: (Ex: "R$ 50k - R$ 250k")

    // --- Dados de Verificação (Sensíveis) ---
    // Idealmente, isso ficaria em uma sub-coleção "private"
    private String cpf;             // NOVO: (Se for "INDIVIDUAL")
    private String cnpj;            // NOVO: (Se for "FIRM")
    private String razaoSocial;     // NOVO: (Se for "FIRM")
    private Map<String, Object> apiVerificationData; // NOVO: JSON bruto da API (para auditoria)
    private Date createdAt;         // NOVO: Data de cadastro
    private String rejectionReason;

    // Construtor vazio necessário para o Firestore
    public Investor() {}

    // Getters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getFotoUrl() { return fotoUrl; }
    public String getBio() { return bio; }
    public String getTese() { return tese; }
    public List<String> getEstagios() { return estagios; }
    public List<String> getAreas() { return areas; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getTicketMedio() { return ticketMedio; }
    public void setTicketMedio(String ticketMedio) { this.ticketMedio = ticketMedio; }
    public String getInvestorType() { return investorType; }
    public void setInvestorType(String investorType) { this.investorType = investorType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmailContato() { return emailContato; }
    public void setEmailContato(String emailContato) { this.emailContato = emailContato; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public Map<String, Object> getApiVerificationData() { return apiVerificationData; }
    public void setApiVerificationData(Map<String, Object> apiVerificationData) { this.apiVerificationData = apiVerificationData; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Investor investor = (Investor) o;
        // Comparação completa para 'areContentsTheSame'
        return Objects.equals(id, investor.id) &&
                Objects.equals(investorType, investor.investorType) &&
                Objects.equals(nome, investor.nome) &&
                Objects.equals(fotoUrl, investor.fotoUrl) &&
                Objects.equals(status, investor.status) &&
                Objects.equals(emailContato, investor.emailContato) &&
                Objects.equals(linkedinUrl, investor.linkedinUrl) &&
                Objects.equals(siteUrl, investor.siteUrl) &&
                Objects.equals(bio, investor.bio) &&
                Objects.equals(tese, investor.tese) &&
                Objects.equals(ticketMedio, investor.ticketMedio) &&
                Objects.equals(razaoSocial, investor.razaoSocial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, investorType, nome, fotoUrl, status, emailContato,
                linkedinUrl, siteUrl, bio, tese, ticketMedio, razaoSocial);
    }
}