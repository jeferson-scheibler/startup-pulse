package com.example.startuppulse.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MentorRepository {

    private static final String MENTORES_COLLECTION = "mentores";

    private final FirebaseFirestore firestore;
    private final String currentUserId;

    @Inject
    public MentorRepository(FirebaseFirestore firestore, FirebaseAuth auth) {
        this.firestore = firestore;
        this.currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    // --- MÉTODOS DE LEITURA (BUSCA) ---

    /**
     * Busca um mentor pelo seu ID.
     * Alinhado com a nova classe Result e o callback genérico.
     */
    public void getMentorById(@NonNull String mentorId, @NonNull ResultCallback<Mentor> callback) {
        firestore.collection(MENTORES_COLLECTION).document(mentorId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Mentor mentor = snap.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(snap.getId());
                            callback.onResult(new Result.Success<>(mentor));
                        } else {
                            // Erro de desserialização do objeto
                            callback.onResult(new Result.Error<>(new Exception("Falha ao mapear os dados do mentor.")));
                        }
                    } else {
                        // Trata "não encontrado" como um erro explícito para a UI
                        callback.onResult(new Result.Error<>(new Exception("Mentor não encontrado.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void findAllMentores(@Nullable String excludeUserId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = firestore.collection(MENTORES_COLLECTION);
        if (excludeUserId != null && !excludeUserId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), excludeUserId);
        }
        query.get().addOnSuccessListener(querySnapshot -> {
            List<Mentor> mentores = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                Mentor mentor = doc.toObject(Mentor.class);
                mentor.setId(doc.getId());
                mentores.add(mentor);
            }
            callback.onResult(new Result.Success<>(mentores));
        }).addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void findMentoresByAreas(@NonNull List<String> areas, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback) {
        if (areas.isEmpty()) {
            callback.onResult(new Result.Success<>(new ArrayList<>()));
            return;
        }

        Query query = firestore.collection(MENTORES_COLLECTION).whereArrayContainsAny("areas", areas);
        if (ownerId != null && !ownerId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), ownerId);
        }

        query.get().addOnSuccessListener(q -> {
            List<Mentor> mentores = new ArrayList<>();
            for (QueryDocumentSnapshot document : q) {
                Mentor mentor = document.toObject(Mentor.class);
                mentor.setId(document.getId());
                mentores.add(mentor);
            }
            callback.onResult(new Result.Success<>(mentores));
        }).addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void findMentoresByCity(@NonNull String cidade, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = firestore.collection(MENTORES_COLLECTION).whereEqualTo("cidade", cidade);
        if (ownerId != null && !ownerId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), ownerId);
        }
        query.get().addOnSuccessListener(q -> {
            List<Mentor> mentores = new ArrayList<>();
            for (QueryDocumentSnapshot doc : q) {
                Mentor mentor = doc.toObject(Mentor.class);
                mentor.setId(doc.getId());
                mentores.add(mentor);
            }
            callback.onResult(new Result.Success<>(mentores));
        }).addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void findMentoresByState(@NonNull String estado, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = firestore.collection(MENTORES_COLLECTION).whereEqualTo("estado", estado);
        if (ownerId != null && !ownerId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), ownerId);
        }
        query.get().addOnSuccessListener(q -> {
            List<Mentor> mentores = new ArrayList<>();
            for (QueryDocumentSnapshot doc : q) {
                Mentor mentor = doc.toObject(Mentor.class);
                mentor.setId(doc.getId());
                mentores.add(mentor);
            }
            callback.onResult(new Result.Success<>(mentores));
        }).addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // --- MÉTODOS DE ESCRITA (CRIAÇÃO, ATUALIZAÇÃO) ---

    public void saveMentorProfile(@NonNull Mentor mentor, @NonNull ResultCallback<String> callback) {
        if (currentUserId == null) {
            callback.onResult(new Result.Error<>(new IllegalStateException("Usuário não autenticado.")));
            return;
        }

        mentor.setId(currentUserId);

        firestore.collection(MENTORES_COLLECTION).document(currentUserId)
                .set(mentor, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(currentUserId)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void updateMentorFields(@NonNull String mentorId, @Nullable Boolean verificado, @Nullable List<String> areas, @NonNull ResultCallback<Void> callback) {
        if (!mentorId.equals(currentUserId)) {
            callback.onResult(new Result.Error<>(new SecurityException("Não autorizado para atualizar este perfil.")));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (verificado != null) updates.put("verificado", verificado);
        if (areas != null) updates.put("areas", areas);

        if (updates.isEmpty()) {
            callback.onResult(new Result.Success<>(null)); // Nenhuma alteração
            return;
        }

        firestore.collection(MENTORES_COLLECTION).document(mentorId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}