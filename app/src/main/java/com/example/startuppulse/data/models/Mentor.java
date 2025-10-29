package com.example.startuppulse.data.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.List;

/**
 * Modelo de dados para um Mentor.
 * Contém apenas informações específicas do perfil de mentor.
 * Informações compartilhadas (nome, foto, linkedin, etc.) são lidas do modelo User.java.
 */
public class Mentor implements Serializable {
    private String id;
    private String bio;
    private String city;
    private String state;
    private boolean verified;
    private double latitude;
    private double longitude;
    private boolean activePublic; // Disponibilidade do mentor
    private String bannerUrl;

    public Mentor() {
        // Construtor vazio necessário para o Firestore
    }

    // --- Getters e Setters com anotações do Firestore ---
    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }
    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }
    public String getBannerUrl() {
        return bannerUrl;
    }
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
    @PropertyName("verificado")
    public boolean isVerified() { return verified; }
    @PropertyName("verificado")
    public void setVerified(boolean verified) { this.verified = verified; }

    // --- Outros campos ---
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
}