package com.example.startuppulse.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IdeiaRepository {

    private static final String TAG = "IdeiaRepository";
    private static final String IDEIAS_COLLECTION = "ideias";

    private final FirebaseFirestore firestore;
    private final String currentUserId;

    // --- Interfaces de Callback para consistência ---
    public interface IdeiasCallback { void onResult(List<Ideia> ideias); }
    public interface IdeiaCallback { void onResult(Ideia ideia); }
    public interface CompletionCallback { void onComplete(boolean success); }
    public interface ListenerCallback<T> { void onUpdate(Result<T> result); }


    @Inject
    public IdeiaRepository(FirebaseFirestore firestore, FirebaseAuth auth) {
        this.firestore = firestore;
        this.currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    // --- MÉTODOS DE LEITURA (LISTENERS EM TEMPO REAL) ---

    public ListenerRegistration listenToIdeia(@NonNull String ideiaId, @NonNull ListenerCallback<Ideia> callback) {
        return firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen to Ideia failed.", e);
                        callback.onUpdate(Result.err(e));
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Ideia ideia = snapshot.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(snapshot.getId());
                            callback.onUpdate(Result.ok(ideia));
                        }
                    } else {
                        callback.onUpdate(Result.ok(null)); // Documento não existe
                    }
                });
    }

    public ListenerRegistration listenToPublicIdeias(@NonNull ListenerCallback<List<Ideia>> callback) {
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
                        Log.w(TAG, "Listen to public ideas failed.", e);
                        callback.onUpdate(Result.err(e));
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
                    callback.onUpdate(Result.ok(ideias));
                });
    }

    public ListenerRegistration listenToDraftIdeias(@NonNull ListenerCallback<List<Ideia>> callback) {
        if (currentUserId == null) {
            callback.onUpdate(Result.ok(new ArrayList<>()));
            return null;
        }
        return firestore.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", currentUserId)
                .whereEqualTo("status", Ideia.Status.RASCUNHO.name())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen to draft ideas failed.", e);
                        callback.onUpdate(Result.err(e));
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
                    callback.onUpdate(Result.ok(rascunhos));
                });
    }


    // --- MÉTODOS DE ESCRITA (CRIAÇÃO, ATUALIZAÇÃO, EXCLUSÃO) ---

    public void saveIdeia(@NonNull Ideia ideia, @NonNull CompletionCallback callback) {
        if (currentUserId == null) {
            callback.onComplete(false);
            return;
        }

        // Garante que o ownerId e o timestamp sejam definidos
        if (ideia.getOwnerId() == null) ideia.setOwnerId(currentUserId);
        ideia.setTimestamp(new Date());

        if (ideia.getId() == null || ideia.getId().isEmpty()) {
            // Cria uma nova ideia
            firestore.collection(IDEIAS_COLLECTION).add(ideia)
                    .addOnSuccessListener(docRef -> callback.onComplete(true))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao criar nova ideia", e);
                        callback.onComplete(false);
                    });
        } else {
            // Atualiza uma ideia existente
            firestore.collection(IDEIAS_COLLECTION).document(ideia.getId()).set(ideia)
                    .addOnSuccessListener(aVoid -> callback.onComplete(true))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao atualizar ideia", e);
                        callback.onComplete(false);
                    });
        }
    }

    public void deleteIdeia(@NonNull String ideiaId, @NonNull CompletionCallback callback) {
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).delete()
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao excluir ideia", e);
                    callback.onComplete(false);
                });
    }

    public void publicarIdeia(@NonNull String ideiaId, @Nullable String mentorId, @NonNull CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.EM_AVALIACAO.name());
        updates.put("timestamp", FieldValue.serverTimestamp());
        if (mentorId != null) {
            updates.put("mentorId", mentorId);
        }
        updates.put("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void unpublishIdeia(@NonNull String ideiaId, @NonNull CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.RASCUNHO.name());
        updates.put("mentorId", FieldValue.delete());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void salvarAvaliacao(@NonNull String ideiaId, @NonNull List<Map<String, Object>> avaliacoes, @NonNull CompletionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avaliacoes", avaliacoes);
        updates.put("avaliacaoStatus", "Avaliada"); // Considerar usar um Enum aqui também no futuro

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }


    // --- MÉTODOS DE MANIPULAÇÃO DE POST-ITS ---

    public void addPostitToIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt novoPostIt, @NonNull CompletionCallback callback) {
        String key = "postIts." + etapaChave;
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(key, FieldValue.arrayUnion(novoPostIt))
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void updatePostitInIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitAntigo, @NonNull PostIt postitNovo, @NonNull CompletionCallback callback) {
        firestore.runTransaction(transaction -> {
                    // Esta operação é complexa e requer uma transação para garantir a atomicidade
                    transaction.update(firestore.collection(IDEIAS_COLLECTION).document(ideiaId), "postIts." + etapaChave, FieldValue.arrayRemove(postitAntigo));
                    transaction.update(firestore.collection(IDEIAS_COLLECTION).document(ideiaId), "postIts." + etapaChave, FieldValue.arrayUnion(postitNovo));
                    return null;
                })
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void deletePostitFromIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitParaApagar, @NonNull CompletionCallback callback) {
        String key = "postIts." + etapaChave;
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(key, FieldValue.arrayRemove(postitParaApagar))
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }
}