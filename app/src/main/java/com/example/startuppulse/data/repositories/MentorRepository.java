package com.example.startuppulse.data.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.ResultCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage; // <-- MUDANÇA: Importado o Storage
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MentorRepository extends BaseRepository implements IMentorRepository {

    private static final String MENTORES_COLLECTION = "mentores";

    // MUDANÇA: 'db' (FirebaseFirestore) já vem do seu BaseRepository.
    // Nós só precisamos injetar o FirebaseStorage.
    private final FirebaseStorage storage;

    @Inject
    public MentorRepository(FirebaseStorage storage) { // <-- MUDANÇA: Injetando FirebaseStorage
        super(); // Isso inicializa o 'db' do BaseRepository
        this.storage = storage; // Armazena a instância do Storage
    }

    // --- MÉTODOS DE LEITURA (BUSCA) ---

    /**
     * Busca um mentor pelo seu ID.
     * (Correto, já usava o 'db' do BaseRepository)
     */
    public void getMentorById(@NonNull String mentorId, @NonNull ResultCallback<Mentor> callback) {
        db.collection(MENTORES_COLLECTION).document(mentorId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Mentor mentor = snap.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(snap.getId());
                            callback.onResult(new Result.Success<>(mentor));
                        } else {
                            callback.onResult(new Result.Error<>(new Exception("Falha ao mapear os dados do mentor.")));
                        }
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Mentor não encontrado.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void updateMentorFieldsByOwnerId(String ownerId, Map<String, Object> updates, ResultCallback<Void> callback) {
        db.collection(MENTORES_COLLECTION) // <-- MUDANÇA
                .whereEqualTo("ownerId", ownerId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onResult(new Result.Success<>(null));
                        return;
                    }

                    WriteBatch batch = db.batch(); // <-- MUDANÇA
                    for (DocumentSnapshot mentorDoc : querySnapshot.getDocuments()) {
                        batch.update(mentorDoc.getReference(), updates);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                            .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));

                })
                .addOnFailureListener(e -> {
                    callback.onResult(new Result.Error<>(e));
                });
    }

    public void getAllMentores(String currentUserId, ResultCallback<List<Mentor>> callback) {
        db.collection("mentores")
                .whereNotEqualTo("ownerId", currentUserId)
                .whereEqualTo("ativoPublico", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Mentor> mentores = queryDocumentSnapshots.toObjects(Mentor.class);
                    callback.onResult(new Result.Success<>(mentores));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void findAllMentores(@Nullable String excludeUserId, @NonNull ResultCallback<List<Mentor>> callback) {
        Query query = db.collection(MENTORES_COLLECTION);
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

        Query query = db.collection(MENTORES_COLLECTION).whereArrayContainsAny("areas", areas);
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
        Query query = db.collection(MENTORES_COLLECTION).whereEqualTo("cidade", cidade);
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
        Query query = db.collection(MENTORES_COLLECTION).whereEqualTo("estado", estado);
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

    public void saveMentorProfile(@NonNull Mentor mentor, @NonNull ResultCallback<String> callback) {
        if (getCurrentUserId() == null) {
            callback.onResult(new Result.Error<>(new IllegalStateException("Usuário não autenticado.")));
            return;
        }

        mentor.setId(getCurrentUserId());

        db.collection(MENTORES_COLLECTION).document(getCurrentUserId())
                .set(mentor, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(getCurrentUserId())))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void updateMentorFields(@NonNull String mentorId, @Nullable Boolean verificado, @Nullable List<String> areas, @NonNull ResultCallback<Void> callback) {
        if (!mentorId.equals(getCurrentUserId())) {
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

        db.collection(MENTORES_COLLECTION).document(mentorId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void updateMentorProfile(@NonNull Mentor mentor,
                                    @Nullable Uri newAvatarUri,
                                    @Nullable Uri newBannerUri,
                                    @NonNull ResultCallback<Mentor> callback) {

        String userId = getCurrentUserId();
        if (userId == null || !userId.equals(mentor.getId())) {
            callback.onResult(new Result.Error<>(new SecurityException("Não autorizado para atualizar este perfil.")));
            return;
        }

        Task<Void> taskChain = Tasks.forResult(null);

        // 1. Se houver um novo Avatar, faz o upload e atualiza o objeto mentor
        if (newAvatarUri != null) {
            String avatarPath = MENTORES_COLLECTION + "/" + userId + "/avatar.jpg";
            taskChain = taskChain.continueWithTask(task -> uploadImage(newAvatarUri, avatarPath))
                    .onSuccessTask(uri -> {
                        // Atualiza o objeto 'mentor' em memória
                        mentor.setFotoUrl(uri.toString());
                        return Tasks.forResult(null);
                    });
        }

        // 2. Se houver um novo Banner, faz o upload e atualiza o objeto mentor
        if (newBannerUri != null) {
            String bannerPath = MENTORES_COLLECTION + "/" + userId + "/banner.jpg";
            taskChain = taskChain.continueWithTask(task -> uploadImage(newBannerUri, bannerPath))
                    .onSuccessTask(uri -> {
                        // Atualiza o objeto 'mentor' em memória
                        mentor.setBannerUrl(uri.toString()); // Assumindo que o campo é 'bannerUrl'
                        return Tasks.forResult(null);
                    });
        }

        // 3. Após a fila de uploads (se houver) terminar, salva o objeto MENTOR
        //    completo (com as novas URLs e novos textos) no Firestore.
        taskChain.continueWithTask(task ->
                        db.collection(MENTORES_COLLECTION).document(userId)
                                .set(mentor, SetOptions.merge())
                )
                .addOnSuccessListener(aVoid -> {
                    // --- 2. MUDANÇA NO RETORNO ---
                    // O 'mentor' agora contém as novas URLs.
                    // Retorna o objeto Mentor atualizado no sucesso.
                    callback.onResult(new Result.Success<>(mentor));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }


    // --- MÉTODO HELPER ---

    /**
     * Faz upload de um arquivo para o Firebase Storage e retorna a URL de download.
     */
    private Task<Uri> uploadImage(Uri fileUri, String storagePath) {
        // MUDANÇA: Agora 'storage' é a instância correta do FirebaseStorage injetada
        StorageReference ref = storage.getReference().child(storagePath);
        UploadTask uploadTask = ref.putFile(fileUri);

        return uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        });
    }
}