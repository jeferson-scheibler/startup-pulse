package com.example.startuppulse.data.repositories;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório responsável pela coleção /mentores no Firestore.
 *
 * Responsável por:
 *  - CRUD de mentores
 *  - Upload de imagens (banner/avatar)
 *  - Consultas específicas (por área, cidade, estado, etc.)
 *
 * Agora atualizado para alinhar com o modelo modular User + Mentor.
 */
@Singleton
public class MentorRepository extends BaseRepository implements IMentorRepository {

    private static final String MENTORES_COLLECTION = "mentores";
    private final FirebaseStorage storage;

    @Inject
    public MentorRepository(FirebaseStorage storage) {
        super();
        this.storage = storage;
    }

    // -----------------------------------------------------
    // BUSCAS
    // -----------------------------------------------------

    /**
     * Busca um mentor pelo seu ID.
     */
    @Override
    public void getMentorById(@NonNull String mentorId, @NonNull ResultCallback<Mentor> callback) {
        db.collection(MENTORES_COLLECTION).document(mentorId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Mentor mentor = snap.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(snap.getId());
                            callback.onResult(new Result.Success<>(mentor));
                        } else {
                            callback.onResult(new Result.Error<>(new Exception("Falha ao mapear dados do mentor.")));
                        }
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Mentor não encontrado.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Retorna todos os mentores públicos (exceto o usuário atual).
     */
    @Override
    public void getAllMentores(@NonNull ResultCallback<List<User>> callback) {
        db.collection("usuarios")
                .whereEqualTo("isMentor", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> mentores = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setId(document.getId());
                            mentores.add(user);
                        }
                    }

                    // 🔹 Carrega os dados complementares de mentor (latitude, cidade, verificado, etc.)
                    enrichWithMentorData(mentores, callback);
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Associa os dados complementares do perfil de mentor (coleção "mentores") aos respectivos usuários.
     */
    private void enrichWithMentorData(@NonNull List<User> users, @NonNull ResultCallback<List<User>> callback) {
        if (users.isEmpty()) {
            callback.onResult(new Result.Success<>(users));
            return;
        }

        AtomicInteger remaining = new AtomicInteger(users.size());

        for (User user : users) {
            db.collection("mentores").document(user.getId())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            Mentor mentorData = snapshot.toObject(Mentor.class);
                            if (mentorData != null) {
                                mentorData.setId(snapshot.getId());
                                user.setMentorData(mentorData);
                            }
                        }

                        if (remaining.decrementAndGet() == 0) {
                            callback.onResult(new Result.Success<>(users));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w("MentorRepository", "Falha ao obter mentorData: " + e.getMessage());
                        if (remaining.decrementAndGet() == 0) {
                            callback.onResult(new Result.Success<>(users));
                        }
                    });
        }
    }


    @Override
    public void findAllMentores(@Nullable String excludeUserId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = db.collection(MENTORES_COLLECTION);
        if (excludeUserId != null && !excludeUserId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), excludeUserId);
        }

        query.get().addOnSuccessListener(qs -> {
            List<Mentor> mentores = new ArrayList<>();
            for (QueryDocumentSnapshot doc : qs) {
                Mentor mentor = doc.toObject(Mentor.class);
                mentor.setId(doc.getId());
                mentores.add(mentor);
            }
            callback.onResult(new Result.Success<>(mentores));
        }).addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }


    /**
     * Busca mentores por cidade.
     */
    @Override
    public void findMentoresByCity(@NonNull String cidade, @Nullable String excludeId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = db.collection(MENTORES_COLLECTION).whereEqualTo("cidade", cidade);
        if (excludeId != null && !excludeId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), excludeId);
        }

        query.get()
                .addOnSuccessListener(q -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : q) {
                        Mentor m = doc.toObject(Mentor.class);
                        m.setId(doc.getId());
                        mentores.add(m);
                    }
                    callback.onResult(new Result.Success<>(mentores));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Busca mentores por estado.
     */
    @Override
    public void findMentoresByState(@NonNull String estado, @Nullable String excludeId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = db.collection(MENTORES_COLLECTION).whereEqualTo("estado", estado);
        if (excludeId != null && !excludeId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), excludeId);
        }

        query.get()
                .addOnSuccessListener(q -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : q) {
                        Mentor m = doc.toObject(Mentor.class);
                        m.setId(doc.getId());
                        mentores.add(m);
                    }
                    callback.onResult(new Result.Success<>(mentores));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Busca mentores que tenham áreas de atuação compatíveis.
     */
    @Override
    public void findMentoresByAreas(
            @NonNull List<String> areas,
            @Nullable String excludeId,
            @NonNull ResultCallback<List<Mentor>> callback
    ) {
        if (areas.isEmpty()) {
            callback.onResult(new Result.Success<>(new ArrayList<>()));
            return;
        }

        db.collection(MENTORES_COLLECTION)
                .whereArrayContainsAny("areas", areas)
                .whereEqualTo("ativoPublico", true)
                .get()
                .addOnSuccessListener(q -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : q) {
                        Mentor m = doc.toObject(Mentor.class);
                        m.setId(doc.getId());

                        // Excluir o próprio usuário da lista, caso venha junto
                        if (excludeId != null && excludeId.equals(m.getId())) continue;

                        mentores.add(m);
                    }
                    callback.onResult(new Result.Success<>(mentores));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // -----------------------------------------------------
    // ATUALIZAÇÕES
    // -----------------------------------------------------

    /**
     * Atualiza campos do mentor usando ownerId (caso o documento não use o mesmo ID do usuário).
     */
    @Override
    public void updateMentorFieldsByOwnerId(@NonNull String ownerId, @NonNull Map<String, Object> updates, @NonNull ResultCallback<Void> callback) {
        db.collection(MENTORES_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onResult(new Result.Success<>(null));
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), updates);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                            .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Atualiza campos específicos do mentor autenticado (verificado, áreas, etc.).
     */
    @Override
    public void updateMentorFields(@NonNull String mentorId, @Nullable Boolean verificado, @Nullable List<String> areas, @NonNull ResultCallback<Void> callback) {
        if (!mentorId.equals(getCurrentUserId())) {
            callback.onResult(new Result.Error<>(new SecurityException("Não autorizado a atualizar este perfil.")));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (verificado != null) updates.put("verificado", verificado);
        if (areas != null) updates.put("areas", areas);

        if (updates.isEmpty()) {
            callback.onResult(new Result.Success<>(null));
            return;
        }

        db.collection(MENTORES_COLLECTION).document(mentorId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Salva ou atualiza o perfil completo do mentor.
     */
    @Override
    public void saveMentorProfile(@NonNull Mentor mentor, @NonNull ResultCallback<String> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onResult(new Result.Error<>(new IllegalStateException("Usuário não autenticado.")));
            return;
        }

        mentor.setId(userId);
        db.collection(MENTORES_COLLECTION).document(userId)
                .set(mentor, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(userId)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Atualiza perfil do mentor (dados e imagens).
     */
    /**
     * Atualiza apenas o banner do mentor (foto de capa).
     * A imagem de perfil pertence ao User.
     */
    @Override
    public void updateMentorProfile(@NonNull Mentor mentor,
                                    @Nullable Uri newBannerUri,
                                    @NonNull ResultCallback<Mentor> callback) {

        String userId = getCurrentUserId();
        if (userId == null || !userId.equals(mentor.getId())) {
            callback.onResult(new Result.Error<>(new SecurityException("Não autorizado a atualizar este perfil.")));
            return;
        }

        Task<Void> chain = Tasks.forResult(null);

        // Apenas banner
        if (newBannerUri != null) {
            String bannerPath = MENTORES_COLLECTION + "/" + userId + "/banner.jpg";
            chain = chain.continueWithTask(task -> uploadImage(newBannerUri, bannerPath))
                    .onSuccessTask(uri -> {
                        mentor.setBannerUrl(uri.toString());
                        return Tasks.forResult(null);
                    });
        }

        chain.continueWithTask(task ->
                db.collection(MENTORES_COLLECTION).document(userId)
                        .set(mentor, SetOptions.merge())
        ).addOnSuccessListener(aVoid ->
                callback.onResult(new Result.Success<>(mentor))
        ).addOnFailureListener(e ->
                callback.onResult(new Result.Error<>(e))
        );
    }

    // -----------------------------------------------------
    // HELPERS
    // -----------------------------------------------------

    /**
     * Faz upload de uma imagem para o Firebase Storage e retorna a URL pública.
     */
    private Task<Uri> uploadImage(Uri fileUri, String storagePath) {
        StorageReference ref = storage.getReference().child(storagePath);
        UploadTask uploadTask = ref.putFile(fileUri);
        return uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        });
    }
}
