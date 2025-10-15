package com.example.startuppulse.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.Mentor;
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
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MentorRepository {

    private static final String TAG = "MentorRepository";
    private static final String MENTORES_COLLECTION = "mentores";

    private final FirebaseFirestore firestore;
    private final String currentUserId;

    // --- Interfaces de Callback ---
    public interface MentorsCallback { void onResult(Result<List<Mentor>> result); }
    public interface MentorCallback { void onResult(Result<Mentor> result); }
    public interface CompletionCallback { void onComplete(Result<Void> result); }
    public interface StringCallback { void onComplete(Result<String> result); }


    @Inject
    public MentorRepository(FirebaseFirestore firestore, FirebaseAuth auth) {
        this.firestore = firestore;
        this.currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    // --- MÉTODOS DE LEITURA (BUSCA) ---

    public void findMentorById(@NonNull String mentorId, @NonNull MentorCallback callback) {
        firestore.collection(MENTORES_COLLECTION).document(mentorId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Mentor mentor = snap.toObject(Mentor.class);
                        if (mentor != null) mentor.setId(snap.getId());
                        callback.onResult(Result.ok(mentor));
                    } else {
                        callback.onResult(Result.ok(null)); // Mentor não encontrado
                    }
                })
                .addOnFailureListener(e -> callback.onResult(Result.err(e)));
    }

    public void findAllMentores(@Nullable String excludeUserId, @NonNull MentorsCallback callback) {
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
            callback.onResult(Result.ok(mentores));
        }).addOnFailureListener(e -> callback.onResult(Result.err(e)));
    }

    public void findMentoresByAreas(@NonNull List<String> areas, @Nullable String ownerId, @NonNull MentorsCallback callback) {
        if (areas.isEmpty()) {
            callback.onResult(Result.ok(new ArrayList<>()));
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
            callback.onResult(Result.ok(mentores));
        }).addOnFailureListener(e -> callback.onResult(Result.err(e)));
    }

    public void findMentoresByCity(@NonNull String cidade, @Nullable String ownerId, @NonNull MentorsCallback callback) {
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
            callback.onResult(Result.ok(mentores));
        }).addOnFailureListener(e -> callback.onResult(Result.err(e)));
    }

    public void findMentoresByState(@NonNull String estado, @Nullable String ownerId, @NonNull MentorsCallback callback) {
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
            callback.onResult(Result.ok(mentores));
        }).addOnFailureListener(e -> callback.onResult(Result.err(e)));
    }

    // --- MÉTODOS DE ESCRITA (CRIAÇÃO, ATUALIZAÇÃO) ---

    public void saveMentorProfile(@NonNull Mentor mentor, @NonNull StringCallback callback) {
        if (currentUserId == null) {
            callback.onComplete(Result.err(new IllegalStateException("Usuário não autenticado.")));
            return;
        }

        // Garante que o ID do mentor seja o mesmo do usuário logado
        mentor.setId(currentUserId);

        firestore.collection(MENTORES_COLLECTION).document(currentUserId)
                .set(mentor, SetOptions.merge()) // Usa merge para não sobrescrever campos não intencionais
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(currentUserId)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    public void updateMentorFields(@NonNull String mentorId, @Nullable Boolean verificado, @Nullable List<String> areas, @NonNull CompletionCallback callback) {
        if (!Objects.equals(currentUserId, mentorId)) {
            // Adicionando uma camada de segurança, embora as regras do Firestore já devam bloquear isso.
            callback.onComplete(Result.err(new SecurityException("Não autorizado.")));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (verificado != null) updates.put("verificado", verificado);
        if (areas != null) updates.put("areas", areas);

        if (updates.isEmpty()) {
            callback.onComplete(Result.ok(null)); // Nenhuma alteração a ser feita
            return;
        }

        firestore.collection(MENTORES_COLLECTION).document(mentorId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }
}