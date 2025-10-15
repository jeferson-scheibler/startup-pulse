package com.example.startuppulse.data;

import java.io.Serializable;
import java.util.Date;

public class PostIt implements Serializable {
    private String id;
    private String texto;
    private String cor;
    private Date timestamp;
    private Date lastModified;

    public PostIt(String texto, String cor, Date date) {}

    public PostIt() {
        // Construtor vazio necessário para a desserialização do Firestore.
    }

    public PostIt(String texto, String cor) {
        this.texto = texto;
        this.cor = cor;
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public String getTexto() { return texto; }
    public String getCor() { return cor; }
    public Date getTimestamp() { return timestamp; }
    public Date getLastModified() { return lastModified; }

    // --- SETTERS ---
    public void setId(String id) {
        this.id = id;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}