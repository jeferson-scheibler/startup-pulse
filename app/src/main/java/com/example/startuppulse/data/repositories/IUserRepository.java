package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;

import java.util.List;

/**
 * Interface para o repositório que gerencia os dados de perfil de usuário no Firestore.
 */
public interface IUserRepository {
    /**
     * Busca os dados de um perfil de usuário a partir do seu ID.
     * @param userId O ID do usuário a ser buscado.
     * @param callback Callback para retornar o resultado da operação.
     */
    void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback);

    /**
     * Atualiza o documento de usuário inteiro com base em um objeto User.
     * Isso sobrescreve o documento com os dados do objeto.
     * @param userId O ID do usuário a ser atualizado.
     * @param user O objeto User contendo os novos dados.
     * @param callback Callback para retornar o resultado da operação.
     */
    void updateUser(@NonNull String userId, @NonNull User user, @NonNull ResultCallback<Void> callback);

    /**
     * Busca todos os usuários que estão marcados como mentores (isMentor = true).
     * @param callback Callback para retornar o resultado da operação.
     */
    void getMentores(@NonNull ResultCallback<List<User>> callback);

    /**
     * Atualiza os dados de um perfil de usuário no Firestore.
     * Campos nulos ou vazios (exceto bio) serão ignorados.
     */
    void updateUserProfile(
            @NonNull String userId,
            @NonNull String newName,
            @Nullable String newBio,
            @Nullable String newPhotoUrl,
            @Nullable String newProfession,
            @Nullable String newLinkedinUrl,
            @Nullable List<String> newAreas,
            @NonNull ResultCallback<Void> callback
    );
    void updateFcmToken(@NonNull String userId, @Nullable String token, @NonNull ResultCallback<Void> callback);

    void updateProfileVisibility(@NonNull String userId, boolean isPublic, @NonNull ResultCallback<Void> callback);
}