package com.example.startuppulse;

import com.example.startuppulse.data.Avaliacao;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Representa uma submissão de avaliação completa por um mentor.
 * Este objeto é o que será armazenado na lista de avaliações da Ideia.
 */
public class AvaliacaoCompleta implements Serializable {

    private String mentorId;
    private String mentorNome;
    private Date timestamp; // Timestamp único no nível superior, resolvendo o problema do array.
    private List<Avaliacao> criteriosAvaliados; // Usa sua classe Avaliacao original.

    public AvaliacaoCompleta() {
        // Construtor vazio para o Firestore.
    }

    // Construtor de conveniência
    public AvaliacaoCompleta(FirebaseUser mentor, List<Avaliacao> criteriosAvaliados) {
        this.mentorId = mentor.getUid();
        this.mentorNome = mentor.getDisplayName();
        this.criteriosAvaliados = criteriosAvaliados;
        this.timestamp = new Date(); // Data é definida na criação do objeto.
    }

    // --- Getters e Setters ---
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getMentorNome() { return mentorNome; }
    public void setMentorNome(String mentorNome) { this.mentorNome = mentorNome; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    @PropertyName("avaliacoes")
    public List<Avaliacao> getCriteriosAvaliados() { return criteriosAvaliados; }
    
    @PropertyName("avaliacoes")
    public void setCriteriosAvaliados(List<Avaliacao> criteriosAvaliados) { this.criteriosAvaliados = criteriosAvaliados; }
}
