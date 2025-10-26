package com.example.startuppulse.data.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.List;

/**
 * Modelo de dados para um Mentor.
 * Os nomes das variáveis seguem a convenção em inglês para consistência de código.
 * A anotação @PropertyName garante o mapeamento correto com os campos do Firestore,
 * permitindo renomear as variáveis locais sem quebrar a lógica de banco de dados.
 */
public class Mentor implements Serializable {

    private String id; // ID local, não armazenado diretamente no documento principal
    private String name;
    private String headline; // Equivalente a "profissão" ou título
    private String bio;      // Uma biografia mais completa
    private String city;
    private String state;
    private String fotoUrl;
    private List<String> areasAtuacao;
    private String linkedinUrl;
    private boolean verified;

    // Campos que podem ser úteis, mas não foram implementados na UI ainda
    private String ownerId;
    private double latitude;
    private double longitude;
    private boolean activePublic;
    private String status = "ativo";

    private String bannerUrl;

    public Mentor() {
        // Construtor vazio necessário para o Firestore
    }

    // --- Getters e Setters com anotações do Firestore ---

    @Exclude // O ID do documento não é um campo dentro do próprio documento
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    @PropertyName("nome")
    public String getName() { return name; }
    @PropertyName("nome")
    public void setName(String name) { this.name = name; }
    @PropertyName("linkedinUrl")
    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    @PropertyName("profissao")
    public String getHeadline() { return headline; }
    @PropertyName("profissao")
    public void setHeadline(String headline) { this.headline = headline; }

    // Supondo que você possa adicionar um campo "bio" no futuro no Firestore
    @PropertyName("bio")
    public String getBio() { return bio; }
    @PropertyName("bio")
    public void setBio(String bio) { this.bio = bio; }

    @PropertyName("cidade")
    public String getCity() { return city; }
    @PropertyName("cidade")
    public void setCity(String city) { this.city = city; }

    @PropertyName("estado")
    public String getState() { return state; }
    @PropertyName("estado")
    public void setState(String state) { this.state = state; }

    @PropertyName("fotoUrl")
    public String getFotoUrl() { return fotoUrl; }
    @PropertyName("fotoUrl")
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    @PropertyName("areas")
    public List<String> getAreas() { return areasAtuacao; }
    @PropertyName("areas")
    public void setAreas(List<String> areas) { this.areasAtuacao = areas; }

    @PropertyName("verificado")
    public boolean isVerified() { return verified; }
    @PropertyName("verificado")
    public void setVerified(boolean verified) { this.verified = verified; }

    // --- Outros campos ---

    @PropertyName("ownerId")
    public String getOwnerId() { return ownerId; }
    @PropertyName("ownerId")
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    @PropertyName("latitude")
    public double getLatitude() { return latitude; }
    @PropertyName("latitude")
    public void setLatitude(double latitude) { this.latitude = latitude; }

    @PropertyName("longitude")
    public double getLongitude() { return longitude; }
    @PropertyName("longitude")
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @PropertyName("ativoPublico")
    public boolean isActivePublic() { return activePublic; }
    @PropertyName("ativoPublico")
    public void setActivePublic(boolean activePublic) { this.activePublic = activePublic; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}