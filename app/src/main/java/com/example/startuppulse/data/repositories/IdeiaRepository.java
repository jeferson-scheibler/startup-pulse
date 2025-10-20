package com.example.startuppulse.data.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.Ideia;
import com.google.firebase.firestore.DocumentSnapshot;
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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório central para todas as operações de dados relacionadas a 'Ideias'.
 * Implementa IIdeiaRepository e abstrai o acesso ao Firestore, Storage e Auth.
 */
@Singleton
public class IdeiaRepository implements IIdeiaRepository {

    private final FirebaseFirestore firestore;
    private final IAuthRepository authRepository;
    private final IStorageRepository storageRepository;

    private static final String IDEIAS_COLLECTION = "ideias";
    private static final String PITCH_DECKS_FOLDER = "pitch_decks";

    @Inject
    public IdeiaRepository(FirebaseFirestore firestore, IAuthRepository authRepository, IStorageRepository storageRepository) {
        this.firestore = firestore;
        this.authRepository = authRepository;
        this.storageRepository = storageRepository;
    }

    // ============================================================
    // GERAÇÃO DE IDs
    // ============================================================

    @NonNull
    @Override
    public String getNewIdeiaId() {
        return firestore.collection(IDEIAS_COLLECTION).document().getId();
    }

    @Override
    @Nullable
    public String getCurrentUserId() {
        if (authRepository != null && authRepository.getCurrentUserId() != null) {
            return authRepository.getCurrentUserId();
        }
        return null;
    }

    // ============================================================
    // CRUD PRINCIPAL
    // ============================================================

    @Override
    public void saveIdeia(@NonNull Ideia ideia, @NonNull ResultCallback<Void> callback) {
        String userId = authRepository.getCurrentUserId();
        if (userId == null) {
            callback.onResult(new Result.Error<>(new IllegalStateException("Usuário não autenticado.")));
            return;
        }

        if (ideia.getId() == null || ideia.getId().isEmpty()) {
            callback.onResult(new Result.Error<>(new IllegalArgumentException("ID da ideia é inválido.")));
            return;
        }

        ideia.setOwnerId(userId);
        ideia.setTimestamp(new Date());

        firestore.collection(IDEIAS_COLLECTION).document(ideia.getId()).set(ideia)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void updateIdeia(@NonNull Ideia ideia, @NonNull ResultCallback<Void> callback) {
        if (ideia.getId() == null || ideia.getId().isEmpty()) {
            callback.onResult(new Result.Error<>(new IllegalArgumentException("ID da ideia para atualização é inválido.")));
            return;
        }
        saveIdeia(ideia, callback);
    }

    @Override
    public void deleteIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Void> callback) {
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).delete()
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // ============================================================
    // LEITURAS E LISTENERS
    // ============================================================

    @Override
    public void getIdeiasForOwner(@NonNull String ownerId, @NonNull ResultCallback<List<Ideia>> callback) {
        firestore.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Ideia> ideias = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Ideia ideia = document.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(document.getId());
                            ideias.add(ideia);
                        }
                    }
                    callback.onResult(new Result.Success<>(ideias));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }


    @Override
    public void getIdeiaById(@NonNull String ideiaId, @NonNull ResultCallback<Ideia> callback) {
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Ideia ideia = documentSnapshot.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(documentSnapshot.getId());
                            callback.onResult(new Result.Success<>(ideia));
                        } else {
                            callback.onResult(new Result.Error<>(new Exception("Falha ao mapear dados da ideia.")));
                        }
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Ideia não encontrada.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @NonNull
    @Override
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

    @NonNull
    @Override
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

    @Override
    public ListenerRegistration listenToDraftIdeias(@NonNull ResultCallback<List<Ideia>> callback) {
        String userId = authRepository.getCurrentUserId();
        if (userId == null) {
            callback.onResult(new Result.Success<>(new ArrayList<>()));
            return null;
        }

        return firestore.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", userId)
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

    // ============================================================
    // UPLOAD DE ARQUIVOS
    // ============================================================

    @Override
    public void uploadPitchDeck(@NonNull String ideiaId, @NonNull Uri fileUri, @NonNull ResultCallback<String> callback) {
        String fileName = "pitch_" + UUID.randomUUID().toString();
        storageRepository.uploadFile(PITCH_DECKS_FOLDER + "/" + ideiaId, fileName, fileUri, callback);
    }

    // ============================================================
    // PÚBLICAÇÃO, AVALIAÇÃO E POST-ITS
    // ============================================================

    @Override
    public void publicarIdeia(@NonNull String ideiaId, @Nullable String mentorId, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.EM_AVALIACAO.name());
        updates.put("timestamp", FieldValue.serverTimestamp());
        if (mentorId != null) updates.put("mentorId", mentorId);
        updates.put("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void unpublishIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.RASCUNHO.name());
        updates.put("mentorId", FieldValue.delete());

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void salvarAvaliacao(@NonNull String ideiaId, @NonNull List<Map<String, Object>> avaliacoes, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avaliacoes", avaliacoes);
        updates.put("avaliacaoStatus", "Avaliada");

        firestore.collection(IDEIAS_COLLECTION).document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void addPostitToIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt novoPostIt, @NonNull ResultCallback<Void> callback) {
        String key = "postIts." + etapaChave;
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(key, FieldValue.arrayUnion(novoPostIt))
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void updatePostitInIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitAntigo, @NonNull PostIt postitNovo, @NonNull ResultCallback<Void> callback) {
        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection(IDEIAS_COLLECTION).document(ideiaId), "postIts." + etapaChave, FieldValue.arrayRemove(postitAntigo));
        batch.update(firestore.collection(IDEIAS_COLLECTION).document(ideiaId), "postIts." + etapaChave, FieldValue.arrayUnion(postitNovo));
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void deletePostitFromIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitParaApagar, @NonNull ResultCallback<Void> callback) {
        String key = "postIts." + etapaChave;
        firestore.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(key, FieldValue.arrayRemove(postitParaApagar))
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}