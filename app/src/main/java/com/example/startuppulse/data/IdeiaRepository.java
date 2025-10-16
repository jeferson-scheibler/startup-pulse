package com.example.startuppulse.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório central para todas as operações de dados relacionadas a 'Ideias'.
 * Abstrai o acesso ao Firestore e fornece métodos consistentes baseados em ResultCallback.
 */
@Singleton
public class IdeiaRepository {

    private static final String IDEIAS_COLLECTION = "ideias";

    private final FirebaseFirestore firestore;
    private final String currentUserId;

    @Inject
    public IdeiaRepository(FirebaseFirestore firestore, FirebaseAuth auth) {
        this.firestore = firestore;
        this.currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    // --- MÉTODOS DE GERAÇÃO DE ID ---

    public String getNewIdeiaId() {
        return firestore.collection(IDEIAS_COLLECTION).document().getId();
    }

    // --- MÉTODOS DE LEITURA (LISTENERS E GETTERS) ---

    public ListenerRegistration listenToIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Ideia> callback) {
        return firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        callback.onResult(new Result.Error<>(e));
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Ideia ideia = snapshot.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(snapshot.getId());
                            callback.onResult(new Result.Success<>(ideia));
                        } else {
                            callback.onResult(new Result.Error<>(new Exception("Falha ao mapear dados da ideia.")));
                        }
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Ideia não encontrada.")));
                    }
                });
    }

    public ListenerRegistration listenToPublicIdeias(@NonNull ResultCallback<List<Ideia>> callback) {
        List<String> statusPublicos = Arrays.asList(
                Ideia.Status.EM_AVALIACAO.name(),
                Ideia.Status.AVALIADA_APROVADA.name(),
                Ideia.Status.AVALIADA_REPROVADA.name()
        );

        return firestore.collection(IDEIAS_COLLECTION)
                .whereIn("status", statusPublicos)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        callback.onResult(new Result.Error<>(e));
                        return;
                    }
                    List<Ideia> ideias = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            ideia.setId(doc.getId());
                            ideias.add(ideia);
                        }
                    }
                    callback.onResult(new Result.Success<>(ideias));
                });
    }

    public ListenerRegistration listenToDraftIdeias(@NonNull ResultCallback<List<Ideia>> callback) {
        if (currentUserId == null) {
            callback.onResult(new Result.Success<>(new ArrayList<>()));
            return null;
        }
        return firestore.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", currentUserId)
                .whereEqualTo("status", Ideia.Status.RASCUNHO.name())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        callback.onResult(new Result.Error<>(e));
                        return;
                    }
                    List<Ideia> rascunhos = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            ideia.setId(doc.getId());
                            rascunhos.add(ideia);
                        }
                    }
                    callback.onResult(new Result.Success<>(rascunhos));
                });
    }

    // --- MÉTODOS DE ESCRITA (CREATE, UPDATE, DELETE) ---

    public void saveIdeia(@NonNull Ideia ideia, @NonNull ResultCallback<Void> callback) {
        if (currentUserId == null) {
            callback.onResult(new Result.Error<>(new IllegalStateException("Usuário não autenticado.")));
            return;
        }
        if (ideia.getId() == null || ideia.getId().isEmpty()) {
            callback.onResult(new Result.Error<>(new IllegalArgumentException("ID da ideia é inválido.")));
            return;
        }

        // Garante que o ownerId e o timestamp sejam definidos/atualizados
        ideia.setOwnerId(currentUserId);
        ideia.setTimestamp(new Date());

        firestore.collection(IDEIAS_COLLECTION).document(ideia.getId()).set(ideia)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void deleteIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Void> callback) {
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).delete()
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void publicarIdeia(@NonNull String ideiaId, @Nullable String mentorId, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.EM_AVALIACAO.name());
        updates.put("timestamp", FieldValue.serverTimestamp());
        if (mentorId != null) {
            updates.put("mentorId", mentorId);
        }
        updates.put("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void unpublishIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.RASCUNHO.name());
        updates.put("mentorId", FieldValue.delete());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void salvarAvaliacao(@NonNull String ideiaId, @NonNull List<Map<String, Object>> avaliacoes, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avaliacoes", avaliacoes);
        updates.put("avaliacaoStatus", "Avaliada"); // Pode virar um Enum no futuro

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void vincularMentorAideia(@NonNull String ideiaId, @NonNull String mentorId, String log, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("mentorId", mentorId);
        updates.put("matchmakingLog", log);
        updates.put("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Busca todas as ideias de um determinado usuário (one-shot).
     * @param ownerId O ID do usuário dono das ideias.
     * @param callback O callback que será chamado com o resultado.
     */
    public void getIdeiasForOwner(@NonNull String ownerId, @NonNull ResultCallback<List<Ideia>> callback) {
        if (ownerId.isEmpty()) {
            callback.onResult(new Result.Success<>(new ArrayList<>()));
            return;
        }

        firestore.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    try {
                        List<Ideia> ideias = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            ideia.setId(doc.getId());
                            ideias.add(ideia);
                        }
                        callback.onResult(new Result.Success<>(ideias));
                    } catch (Exception e) {
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // --- MÉTODOS DE MANIPULAÇÃO DE POST-ITS ---

    public void addPostitToIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt novoPostIt, @NonNull ResultCallback<Void> callback) {
        String key = "postIts." + etapaChave;
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(key, FieldValue.arrayUnion(novoPostIt))
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void updatePostitInIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitAntigo, @NonNull PostIt postitNovo, @NonNull ResultCallback<Void> callback) {
        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection(IDEIAS_COLLECTION).document(ideiaId), "postIts." + etapaChave, FieldValue.arrayRemove(postitAntigo));
        batch.update(firestore.collection(IDEIAS_COLLECTION).document(ideiaId), "postIts." + etapaChave, FieldValue.arrayUnion(postitNovo));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void deletePostitFromIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitParaApagar, @NonNull ResultCallback<Void> callback) {
        String key = "postIts." + etapaChave;
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(key, FieldValue.arrayRemove(postitParaApagar))
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}