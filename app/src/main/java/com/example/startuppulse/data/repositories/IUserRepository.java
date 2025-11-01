package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.User;

import java.util.List;

/**
 * Interface do repositório responsável por interagir com os dados de usuário e mentor no Firestore.
 *
 * Responsabilidades:
 * - Gerenciar perfis de usuários (/usuarios)
 * - Gerenciar dados complementares de mentores (/mentores)
 * - Fornecer métodos de leitura e escrita consistentes
 */
public interface IUserRepository {

    /**
     * Obtém os dados de um usuário a partir do seu ID.
     * Caso o usuário seja um mentor (isMentor = true),
     * o repositório deve também carregar seus dados de mentor (mentorData).
     *
     * @param userId   ID do usuário.
     * @param callback Callback para retorno do resultado.
     */
    void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback);

    /**
     * Atualiza o perfil do usuário com base em um objeto User.
     * Utiliza merge no Firestore para preservar campos não enviados.
     *
     * @param userId   ID do usuário.
     * @param user     Objeto User atualizado.
     * @param callback Callback para retorno do resultado.
     */
    void updateUser(@NonNull String userId, @NonNull User user, @NonNull ResultCallback<Void> callback);

    /**
     * Atualiza os campos básicos de perfil do usuário.
     * Campos nulos ou vazios (exceto bio) são ignorados.
     *
     * @param userId         ID do usuário.
     * @param newName        Novo nome (obrigatório).
     * @param newBio         Nova biografia (opcional).
     * @param newPhotoUrl    Nova URL da foto (opcional).
     * @param newProfession  Nova profissão (opcional).
     * @param newLinkedinUrl Novo link do LinkedIn (opcional).
     * @param newAreas       Novas áreas de interesse (opcional).
     * @param callback       Callback para o resultado.
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

    /**
     * Atualiza ou remove o token FCM do usuário.
     *
     * @param userId   ID do usuário.
     * @param token    Novo token (ou null para remover).
     * @param callback Callback para o resultado.
     */
    void updateFcmToken(@NonNull String userId, @Nullable String token, @NonNull ResultCallback<Void> callback);

    /**
     * Atualiza a visibilidade pública do perfil de usuário.
     *
     * @param userId   ID do usuário.
     * @param isPublic Valor booleano indicando se o perfil deve ser público.
     * @param callback Callback para o resultado.
     */
    void updateProfileVisibility(@NonNull String userId, boolean isPublic, @NonNull ResultCallback<Void> callback);

    /**
     * Busca todos os usuários marcados como mentores (isMentor = true).
     * O repositório deve preencher os dados complementares de mentor (mentorData)
     * sempre que possível.
     *
     * @param callback Callback para retornar a lista de mentores.
     */
    void getMentores(@NonNull ResultCallback<List<User>> callback);
}
