package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementação do IUserRepository para interagir com a coleção de usuários no Firestore.
 */
@Singleton
public class UserRepository implements IUserRepository {

    private static final String USERS_COLLECTION = "usuarios";
    private final FirebaseFirestore firestore;

    @Inject
    public UserRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback) {
        firestore.collection(USERS_COLLECTION).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onResult(new Result.Success<>(user));
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Usuário não encontrado no Firestore.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // Atualize a implementação do método
    @Override
    public void updateUserProfile(@NonNull String userId, @NonNull String newName, @Nullable String newBio, @Nullable String newPhotoUrl, @Nullable String newProfession, @Nullable String newLinkedinUrl, @Nullable List<String> newAreas, @NonNull ResultCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();

        if (newName != null && !newName.isEmpty()) {
            updates.put("nome", newName);
        }
        if (newBio != null) {
            updates.put("bio", newBio);
        }
        if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) {
            updates.put("fotoUrl", newPhotoUrl);
        }
        if (newProfession != null) { // Adicionado
            updates.put("profissao", newProfession);
        }
        if (newLinkedinUrl != null) { // Adicionado
            updates.put("linkedinUrl", newLinkedinUrl);
        }
        if (newAreas != null) { // Adicionado
            updates.put("areas", newAreas);
        }

        if (updates.isEmpty()) {
            callback.onResult(new Result.Success<>(null));
            return;
        }

        firestore.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void updateProfileVisibility(@NonNull String userId, boolean isPublic, @NonNull ResultCallback<Void> callback) {
        firestore.collection(USERS_COLLECTION).document(userId)
                .update("profilePublic", isPublic) // Use o nome exato do campo no Firestore
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
    /**
     * Atualiza (ou remove) o token FCM no documento do usuário.
     * @param userId ID do usuário.
     * @param token O novo token, ou null/vazio para remover o token existente.
     * @param callback Callback para o resultado da operação.
     */
    @Override
    public void updateFcmToken(@NonNull String userId, @Nullable String token, @NonNull ResultCallback<Void> callback) {
        if (userId.isEmpty()) {
            callback.onResult(new Result.Error<>(new IllegalArgumentException("User ID cannot be empty.")));
            return;
        }

        // Decide se atualiza com o novo token ou remove o campo
        Object tokenValue = (token != null && !token.isEmpty()) ? token : FieldValue.delete();

        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", tokenValue) // Usa update para modificar apenas este campo
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

//    @Override
//    public void getUserData(@NonNull String userId, @NonNull ResultCallback<User> callback) {
//        firestore.collection(USERS_COLLECTION).document(userId).get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot != null && documentSnapshot.exists()) {
//                        User user = documentSnapshot.toObject(User.class);
//                        callback.onResult(new Result.Success<>(user));
//                    } else {
//                        callback.onResult(new Result.Error<>(new Exception("Usuário não encontrado no Firestore.")));
//                    }
//                })
//                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
//    }

    @Override
    public void updateUser(@NonNull String userId, @NonNull User user, @NonNull ResultCallback<Void> callback) {
        // Usamos .set(user) para salvar o objeto inteiro
        // O Firestore usará os @PropertyName para mapear os campos
        firestore.collection(USERS_COLLECTION).document(userId)
                .set(user) // Salva o objeto User completo
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void getMentores(@NonNull ResultCallback<List<User>> callback) {
        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("isMentor", true) // A consulta principal
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> mentores = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setId(document.getId()); // Seta o UID no objeto
                            mentores.add(user);
                        }
                    }
                    callback.onResult(new Result.Success<>(mentores));
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}