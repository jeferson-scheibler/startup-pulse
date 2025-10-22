package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
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
            updates.put("foto_perfil", newPhotoUrl);
        }
        if (newProfession != null) { // Adicionado
            updates.put("profissao", newProfession);
        }
        if (newLinkedinUrl != null) { // Adicionado
            updates.put("linkedinUrl", newLinkedinUrl);
        }
        if (newAreas != null) { // Adicionado
            updates.put("areasDeInteresse", newAreas);
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
}