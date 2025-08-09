package com.example.startuppulse;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe centralizada para todas as interações com o Cloud Firestore.
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static final String USUARIOS_COLLECTION = "usuarios";
    private static final String IDEIAS_COLLECTION = "ideias";
    private static final String MENTORES_COLLECTION = "mentores";
    private final FirebaseFirestore db;

    //<editor-fold desc="Listeners e Callbacks (Interfaces)">

    // Listener para buscar um único usuário
    public interface UsuarioListener {
        void onUsuarioCarregado(DocumentSnapshot snapshot);
        void onError(Exception e);
    }

    // Listener para listas de ideias (Rascunhos, Publicadas, etc.)
    public interface IdeiasListener {
        void onIdeiasCarregadas(List<Ideia> ideias);
        void onError(Exception e);
    }

    // Listener para buscar uma única ideia (one-time fetch)
    public interface IdeiaUnicaListener {
        void onIdeiaCarregada(Ideia ideia);
        void onError(Exception e);
    }

    // Callback para operações simples de sucesso/falha (ex: delete, update)
    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Callback para quando uma nova ideia é adicionada, retornando seu ID
    public interface AddIdeiaListener {
        void onSuccess(String ideiaId);
        void onFailure(Exception e);
    }
    // Listener para listas de mentores
    public interface MentorListener {
        void onMentorEncontrado(Mentor mentor);
        void onNenhumMentorEncontrado();
        void onError(Exception e);
    }
    public interface MentoresListener {
        void onMentoresCarregados(List<Mentor> mentores);
        void onError(Exception e);
    }
    public interface AddMentorListener {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }
    //</editor-fold>

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    //<editor-fold desc="Métodos de Usuário">
    public void salvarUsuario(String userId, String nome, String email, String fotoUrl, String bio) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nome", nome);
        usuario.put("email", email);
        usuario.put("foto_perfil", fotoUrl);
        usuario.put("bio", bio);
        usuario.put("isPremium", false);

        db.collection(USUARIOS_COLLECTION).document(userId)
                .set(usuario, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuário salvo/atualizado com sucesso com ID: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Erro ao salvar usuário", e));
    }

    public void buscarUsuario(String usuarioId, @NonNull final UsuarioListener listener) {
        if (usuarioId == null || usuarioId.isEmpty()) {
            listener.onError(new IllegalArgumentException("ID do usuário não pode ser nulo."));
            return;
        }
        db.collection(USUARIOS_COLLECTION).document(usuarioId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onUsuarioCarregado(documentSnapshot);
                    } else {
                        listener.onUsuarioCarregado(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erro ao buscar usuário", e);
                    listener.onError(e);
                });
    }
    public void updatePremiumStatus(String userId, boolean isPremium) {
        if (userId == null || userId.isEmpty()) return;

        db.collection(USUARIOS_COLLECTION).document(userId)
                .update("isPremium", isPremium)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Status Premium atualizado para: " + isPremium))
                .addOnFailureListener(e -> Log.w(TAG, "Erro ao atualizar status Premium", e));
    }
    //</editor-fold>

    //<editor-fold desc="Métodos de Ideia">
    public String getNewIdeiaId() {
        return db.collection(IDEIAS_COLLECTION).document().getId();
    }

    /**
     * Adiciona uma nova ideia usando um ID pré-gerado.
     * Este metodo foi atualizado para usar .set() com um ID específico.
     */
    public void addIdeia(Ideia ideia, final AddIdeiaListener listener) {
        // Usa o ID que já foi gerado e definido no objeto Ideia
        db.collection(IDEIAS_COLLECTION).document(ideia.getId())
                .set(ideia)
                .addOnSuccessListener(aVoid -> listener.onSuccess(ideia.getId()))
                .addOnFailureListener(listener::onFailure);
    }

    /**
     * NOVO E ESSENCIAL: Anexa um listener em tempo real a uma ideia.
     * Necessário para a validação dos post-its na CanvasIdeiaActivity.
     */
    public ListenerRegistration listenToIdeia(String ideiaId, final IdeiaUnicaListener listener) {
        final DocumentReference docRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
        return docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) { listener.onError(e); return; }
            if (snapshot != null && snapshot.exists()) {
                Ideia ideia = snapshot.toObject(Ideia.class);
                if (ideia != null) {
                    ideia.setId(snapshot.getId());
                    listener.onIdeiaCarregada(ideia);
                }
            } else {
                listener.onError(new Exception("A ideia não foi encontrada."));
            }
        });
    }

    /**
     * ESSENCIAL: Adiciona um post-it a uma etapa específica da ideia.
     */
    public void addPostitToIdeia(String ideiaId, String etapaChave, String texto, String cor, final FirestoreCallback callback) {
        DocumentReference ideiaRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
        String key = "postIts." + etapaChave;
        PostIt novoPostIt = new PostIt(texto, cor);
        novoPostIt.setTimestamp(new Date());
        ideiaRef.update(key, FieldValue.arrayUnion(novoPostIt))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    public void updatePostitInIdeia(String ideiaId, String etapaChave, PostIt postitAntigo, String novoTexto, String novaCor, final FirestoreCallback callback) {
        DocumentReference ideiaRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
        String key = "postIts." + etapaChave;
        db.runTransaction(transaction -> {
                    transaction.update(ideiaRef, key, FieldValue.arrayRemove(postitAntigo));
                    PostIt postitNovo = new PostIt(novoTexto, novaCor);
                    postitNovo.setTimestamp(postitAntigo.getTimestamp());
                    postitNovo.setLastModified(new Date());
                    transaction.update(ideiaRef, key, FieldValue.arrayUnion(postitNovo));
                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void deletePostitFromIdeia(String ideiaId, String etapaChave, PostIt postitParaApagar, final FirestoreCallback callback) {
        DocumentReference ideiaRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
        String key = "postIts." + etapaChave;

        // Remove o objeto PostIt diretamente. É mais seguro e legível.
        ideiaRef.update(key, FieldValue.arrayRemove(postitParaApagar))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * ESSENCIAL: Atualiza um documento de ideia inteiro.
     * Usado para salvar o rascunho.
     */
    public void updateIdeia(String ideiaId, Ideia ideia, final FirestoreCallback callback, Context context) {
        db.collection(IDEIAS_COLLECTION).document(ideiaId).set(ideia)
                .addOnSuccessListener(aVoid -> {
                    Boolean isOnline = NetworkManager.getInstance(context).isNetworkAvailable().getValue();
                    String feedback = (isOnline != null && isOnline) ? "Progresso salvo e sincronizado!" : "Progresso salvo offline.";
                    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Progresso salvo offline.", Toast.LENGTH_SHORT).show();
                    callback.onFailure(e);
                });
    }

    /**
     * Busca os rascunhos de um usuário específico.
     * CORRIGIDO: usa "ownerId" para consistência com o modelo Ideia.java.
     */
    public void getMeusRascunhos(String ownerId, IdeiasListener listener) {
        if (ownerId == null || ownerId.isEmpty()) {
            listener.onIdeiasCarregadas(new ArrayList<>());
            return;
        }

        db.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", ownerId) // CORRIGIDO
                .whereEqualTo("status", "RASCUNHO")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ideia> rascunhos = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Ideia ideia = doc.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(doc.getId());
                            rascunhos.add(ideia);
                        }
                    }
                    listener.onIdeiasCarregadas(rascunhos);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar rascunhos", e);
                    listener.onError(e);
                });
    }

    /**
     * Exclui uma ideia do Firestore.
     */
    public void excluirIdeia(String ideiaId, final FirestoreCallback callback) {
        if (ideiaId == null || ideiaId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("ID da ideia nulo"));
            return;
        }
        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    public void getIdeiasPublicadas(IdeiasListener listener) {
        db.collection(IDEIAS_COLLECTION)
                .whereEqualTo("status", "PUBLICADA") // A condição chave!
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ideia> ideias = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Ideia ideia = doc.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(doc.getId());
                            ideias.add(ideia);
                        }
                    }
                    listener.onIdeiasCarregadas(ideias);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar ideias publicadas", e);
                    listener.onError(e);
                });
    }
    /**
     * NOVO: Altera o status de uma ideia para "RASCUNHO" e remove o mentor.
     */
    public void unpublishIdeia(String ideiaId, final FirestoreCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "RASCUNHO");
        updates.put("mentorId", FieldValue.delete()); // Remove o campo do mentor

        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    /**
     * Salva a avaliação completa de uma ideia e atualiza o seu status.
     */
    public void salvarAvaliacao(String ideiaId, List<Map<String, Object>> avaliacoes, final FirestoreCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avaliacoes", avaliacoes);
        updates.put("avaliacaoStatus", "Avaliada");

        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    //</editor-fold>

    //<editor-fold desc="Mentores">
    public void addMentorWithId(String mentorId, Mentor mentor, final AddMentorListener listener) {
        // Garante que o objeto Mentor tenha o seu próprio ID preenchido
        mentor.setId(mentorId);

        db.collection(MENTORES_COLLECTION).document(mentorId)
                .set(mentor) // Usa .set() para especificar o ID do documento
                .addOnSuccessListener(aVoid -> listener.onSuccess(mentorId))
                .addOnFailureListener(listener::onFailure);
    }

    public void getMentoresPublicados(MentoresListener listener) {
        db.collection(MENTORES_COLLECTION).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            mentores.add(mentor);
                        }
                    }
                    listener.onMentoresCarregados(mentores);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Encontra o primeiro mentor disponível numa cidade específica.
     */
    public void findMentorByCity(String cidade, String authorId, MentorListener listener) {
        db.collection("mentores")
                .whereEqualTo("cidade", cidade)
                .whereNotEqualTo(com.google.firebase.firestore.FieldPath.documentId(), authorId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            listener.onMentorEncontrado(mentor);
                        }
                    } else {
                        listener.onNenhumMentorEncontrado();
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * NOVO: Encontra o primeiro mentor disponível num estado específico.
     */
    public void findMentorByState(String estado, String authorId, MentorListener listener) {
        db.collection("mentores")
                .whereEqualTo("estado", estado)
                .whereNotEqualTo(com.google.firebase.firestore.FieldPath.documentId(), authorId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            listener.onMentorEncontrado(mentor);
                        }
                    } else {
                        listener.onNenhumMentorEncontrado();
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Atualiza o status de uma ideia para "PUBLICADA" e associa um mentor.
     */
    public void publicarIdeia(String ideiaId, @Nullable String mentorId, final FirestoreCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "PUBLICADA");
        updates.put("timestamp", FieldValue.serverTimestamp()); // Atualiza a data para a da publicação
        if (mentorId != null) {
            updates.put("mentorId", mentorId);
        }

        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    /**
     * NOVO: Busca um mentor específico pelo seu ID de documento.
     * Usado na tela de status da ideia para carregar os detalhes do mentor.
     */
    public void findMentorById(String mentorId, MentorListener listener) {
        if (mentorId == null || mentorId.isEmpty()) {
            listener.onError(new IllegalArgumentException("O ID do mentor não pode ser nulo."));
            return;
        }

        db.collection(MENTORES_COLLECTION).document(mentorId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Mentor mentor = documentSnapshot.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(documentSnapshot.getId()); // Garante que o objeto tem o ID
                            listener.onMentorEncontrado(mentor);
                        } else {
                            listener.onError(new Exception("Falha ao converter os dados do mentor."));
                        }
                    } else {
                        listener.onNenhumMentorEncontrado();
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    //</editor-fold>
}