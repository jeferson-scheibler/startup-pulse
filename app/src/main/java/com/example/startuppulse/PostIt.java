package com.example.startuppulse;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class PostIt implements Serializable {
    private String texto;
    private String cor;

    @ServerTimestamp
    private Date timestamp;
    private Date lastModified;

    // Construtor vazio é essencial para o Firestore
    public PostIt() {}

    public PostIt(String texto, String cor) {
        this.texto = texto;
        this.cor = cor;
    }

    // --- Getters ---
    public String getTexto() { return texto; }
    public String getCor() { return cor; }
    public Date getTimestamp() { return timestamp; }
    public Date getLastModified() { return lastModified; }

    // --- Setters ---
    public void setTexto(String texto) { this.texto = texto; }
    public void setCor(String cor) { this.cor = cor; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
}