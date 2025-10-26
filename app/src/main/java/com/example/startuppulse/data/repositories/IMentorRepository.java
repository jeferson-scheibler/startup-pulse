package com.example.startuppulse.data.repositories;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.Mentor;

import java.util.List;
import java.util.Map;

/**
 * Contrato para gerenciamento e consulta de mentores.
 * ESTA VERSÃO CORRIGIDA usa ResultCallback, assim como a implementação.
 */
public interface IMentorRepository {

    // --- MÉTODOS DE LEITURA (BUSCA) ---

    void getMentorById(@NonNull String mentorId, @NonNull ResultCallback<Mentor> callback);

    void getAllMentores(String currentUserId, ResultCallback<List<Mentor>> callback);

    void findAllMentores(@Nullable String excludeUserId, @NonNull ResultCallback<List<Mentor>> callback);

    void findMentoresByAreas(@NonNull List<String> areas, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback);

    void findMentoresByCity(@NonNull String cidade, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback);

    void findMentoresByState(@NonNull String estado, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback);

    // --- MÉTODOS DE ESCRITA (CRIAÇÃO, ATUALIZAÇÃO) ---

    void saveMentorProfile(@NonNull Mentor mentor, @NonNull ResultCallback<String> callback);

    void updateMentorFields(@NonNull String mentorId, @Nullable Boolean verificado, @Nullable List<String> areas, @NonNull ResultCallback<Void> callback);

    void updateMentorProfile(@NonNull Mentor mentor,
                             @Nullable Uri newAvatarUri,
                             @Nullable Uri newBannerUri,
                             @NonNull ResultCallback<Mentor> callback);

    // Este é o método que o EditarPerfilViewModel usa para sincronizar
    void updateMentorFieldsByOwnerId(String ownerId, Map<String, Object> updates, ResultCallback<Void> callback);

    // As outras assinaturas de método que retornavam Task<> e tinham parâmetros
    // errados (como 'Object o') foram removidas por não existirem na implementação.
}