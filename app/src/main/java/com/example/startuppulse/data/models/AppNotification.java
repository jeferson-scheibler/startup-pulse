package com.example.startuppulse.data.models;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Modelo de dados para uma notificação a ser salva no Firestore,
 * na subcoleção /usuarios/{userId}/notificacoes
 */
public class AppNotification {

    @Exclude // O ID será o do documento do Firestore
    private String id;

    private String title;
    private String body;
    private String channelId;
    private boolean isRead;

    @Nullable
    private String ideiaId; // Link opcional para uma ideia

    @ServerTimestamp // O Firestore definirá este tempo automaticamente
    private Date timestamp;

    // Construtor vazio necessário para o Firestore
    public AppNotification() {}

    // Construtor principal
    public AppNotification(String title, String body, String channelId, @Nullable String ideiaId) {
        this.title = title;
        this.body = body;
        this.channelId = channelId;
        this.ideiaId = ideiaId;
        this.isRead = false; // Novas notificações começam como não lidas
    }

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Nullable
    public String getIdeiaId() {
        return ideiaId;
    }

    public void setIdeiaId(@Nullable String ideiaId) {
        this.ideiaId = ideiaId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}