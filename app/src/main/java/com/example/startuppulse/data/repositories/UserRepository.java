package com.example.startuppulse.data.repositories;

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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório responsável pelas operações com a coleção de usuários (User) e mentores (Mentor) no Firestore.
 *
 * Agora compatível com o novo modelo:
 *  - /usuarios → informações compartilhadas (User)
 *  - /mentores → informações específicas de mentor (Mentor)
 *
 * Boas práticas aplicadas:
 *  - Evita sobrescrever dados críticos (usa SetOptions.merge() quando apropriado)
 *  - Garante que IDs sejam sempre preenchidos no retorno
 *  - Trata nulos de forma segura em callbacks
 */
@Singleton
public class UserRepository implements IUserRepository {

    private static final String USERS_COLLECTION = "usuarios";
    private static final String MENTORES_COLLECTION = "mentores";

    private final FirebaseFirestore firestore;

    @Inject
    public UserRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    // -----------------------------------------------------
    // PERFIL DO USUÁRIO
    // -----------------------------------------------------
    @Override
    public void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback) {
        firestore.collection(USERS_COLLECTION).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user == null) user = new User();

                        // Compatibilidade com antigas keys no Firestore
                        // foto_perfil -> fotoUrl (ou equivalente em seu model)
                        if ((user.getFotoUrl() == null || user.getFotoUrl().isEmpty())
                                && documentSnapshot.contains("foto_perfil")) {
                            String foto = documentSnapshot.getString("foto_perfil");
                            if (foto != null) user.setFotoUrl(foto);
                        }

                        // Exemplo: campo antigo 'pro' (boole), mapeie para seu campo atual (ex: plano / isPro)
                        if (documentSnapshot.contains("pro")) {
                            try {
                                Boolean pro = documentSnapshot.getBoolean("pro");
                                if (pro != null) {
                                    // ajuste conforme seu model (ex.: setPro, setPlano, setPremium)
                                    user.setPremium(pro); // substitua por seu setter real se diferente
                                }
                            } catch (Exception ignored) { }
                        }

                        // Garante que o ID do documento esteja preenchido no objeto User
                        user.setId(documentSnapshot.getId());

                        callback.onResult(new Result.Success<>(user));
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Usuário não encontrado no Firestore.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }


    /**
     * Atualiza campos básicos do perfil do usuário.
     */
    @Override
    public void updateUserProfile(@NonNull String userId,
                                  @NonNull String newName,
                                  @Nullable String newBio,
                                  @Nullable String newPhotoUrl,
                                  @Nullable String newProfession,
                                  @Nullable String newLinkedinUrl,
                                  @Nullable List<String> newAreas,
                                  @NonNull ResultCallback<Void> callback) {

        Map<String, Object> updates = new HashMap<>();

        if (newName != null && !newName.isEmpty()) updates.put("nome", newName);
        if (newBio != null) updates.put("bio", newBio);
        if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) updates.put("fotoUrl", newPhotoUrl);
        if (newProfession != null) updates.put("profissao", newProfession);
        if (newLinkedinUrl != null) updates.put("linkedinUrl", newLinkedinUrl);
        if (newAreas != null) updates.put("areas", newAreas);

        if (updates.isEmpty()) {
            callback.onResult(new Result.Success<>(null));
            return;
        }

        firestore.collection(USERS_COLLECTION).document(userId)
                .set(updates, SetOptions.merge()) // Evita sobrescrever outros campos
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Atualiza a visibilidade pública do perfil.
     */
    @Override
    public void updateProfileVisibility(@NonNull String userId, boolean isPublic, @NonNull ResultCallback<Void> callback) {
        firestore.collection(USERS_COLLECTION).document(userId)
                .update("profilePublic", isPublic)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Atualiza (ou remove) o token FCM do usuário.
     */
    @Override
    public void updateFcmToken(@NonNull String userId, @Nullable String token, @NonNull ResultCallback<Void> callback) {
        if (userId.isEmpty()) {
            callback.onResult(new Result.Error<>(new IllegalArgumentException("User ID não pode estar vazio.")));
            return;
        }

        Object tokenValue = (token != null && !token.isEmpty()) ? token : FieldValue.delete();

        firestore.collection(USERS_COLLECTION).document(userId)
                .update("fcmToken", tokenValue)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Atualiza ou substitui todo o documento do usuário.
     * Usa merge para preservar campos não modificados.
     */
    @Override
    public void updateUser(@NonNull String userId, @NonNull User user, @NonNull ResultCallback<Void> callback) {
        firestore.collection(USERS_COLLECTION).document(userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // -----------------------------------------------------
    // MENTORES
    // -----------------------------------------------------
    /**
     * Retorna todos os usuários com flag isMentor = true.
     * Caso deseje incluir dados de mentor, use loadMentorData() em sequência.
     */
    @Override
    public void getMentores(@NonNull ResultCallback<List<User>> callback) {
        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("isMentor", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> mentores = new ArrayList<>();
                    List<String> mentorIds = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setId(document.getId());
                            mentores.add(user);
                            mentorIds.add(document.getId());
                        }
                    }

                    if (mentores.isEmpty()) {
                        // Retorna a lista vazia imediatamente
                        callback.onResult(new Result.Success<>(mentores));
                        return;
                    }

                    // Enriquecer com dados da coleção /mentores (se existir)
                    enrichWithMentorData(mentores, mentorIds, callback);
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Enriquecer a lista de Users (mentores) com os dados complementares da coleção /mentores.
     * Faz consultas em batches (whereIn de até 10 ids por query).
     */
    private void enrichWithMentorData(List<User> users, List<String> mentorIds, ResultCallback<List<User>> callback) {
        if (mentorIds == null || mentorIds.isEmpty()) {
            callback.onResult(new Result.Success<>(users));
            return;
        }

        // Particiona os ids em batches de 10 (limite do whereIn)
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        final int batchSize = 10;
        for (int i = 0; i < mentorIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, mentorIds.size());
            List<String> sub = mentorIds.subList(i, end);
            Task<QuerySnapshot> t = firestore.collection("mentores")
                    .whereIn(FieldPath.documentId(), sub)
                    .get();
            tasks.add(t);
        }

        // Executa todas as tasks em paralelo e processa os resultados
        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    // Mapeia id -> Mentor document
                    Map<String, Mentor> mentorMap = new HashMap<>();
                    for (Object res : results) {
                        QuerySnapshot qs = (QuerySnapshot) res;
                        for (DocumentSnapshot d : qs.getDocuments()) {
                            Mentor m = d.toObject(Mentor.class);
                            if (m != null) {
                                m.setId(d.getId());
                                mentorMap.put(d.getId(), m);
                            }
                        }
                    }

                    // Associa mentorData aos users
                    for (User u : users) {
                        if (u == null) continue;
                        Mentor m = mentorMap.get(u.getId());
                        if (m != null) {
                            u.setMentorData(m); // garanta que User tem esse setter
                        }
                    }

                    callback.onResult(new Result.Success<>(users));
                })
                .addOnFailureListener(e -> {
                    // Falha em obter dados de mentores — ainda retornamos os users (sem mentorData) ou falhamos conforme preferir
                    // Aqui optamos por retornar com sucesso parcial (users sem mentorData)
                    callback.onResult(new Result.Success<>(users));
                });
    }


    /**
     * Carrega o documento de mentor correspondente e vincula ao User.
     */
    private void loadMentorData(@NonNull User user, @NonNull ResultCallback<User> callback) {
        firestore.collection(MENTORES_COLLECTION)
                .document(user.getId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            user.setMentorData(mentor);
                        }
                    }
                    callback.onResult(new Result.Success<>(user));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Enriquecer lista de usuários mentores com dados específicos da coleção /mentores.
     */
    private void enrichWithMentorData(@NonNull List<User> mentores, @NonNull ResultCallback<List<User>> callback) {
        if (mentores.isEmpty()) {
            callback.onResult(new Result.Success<>(mentores));
            return;
        }

        List<User> enriched = new ArrayList<>();
        final int[] processed = {0};

        for (User user : mentores) {
            firestore.collection(MENTORES_COLLECTION)
                    .document(user.getId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Mentor mentor = doc.toObject(Mentor.class);
                            if (mentor != null) {
                                mentor.setId(doc.getId());
                                user.setMentorData(mentor);
                            }
                        }
                        enriched.add(user);
                        if (++processed[0] == mentores.size()) {
                            callback.onResult(new Result.Success<>(enriched));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (++processed[0] == mentores.size()) {
                            callback.onResult(new Result.Success<>(enriched));
                        }
                    });
        }
    }
}