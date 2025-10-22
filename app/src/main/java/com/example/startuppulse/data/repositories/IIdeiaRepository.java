package com.example.startuppulse.data.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.Ideia;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;

/**
 * Interface de contrato para todas as operações de dados relacionadas a 'Ideias'.
 * Define a comunicação entre camadas, garantindo desacoplamento do Firestore
 * e padronizando os callbacks assíncronos.
 */
public interface IIdeiaRepository {

    // ------------------------------
    // Métodos utilitários
    // ------------------------------

    /**
     * Retorna o ID do usuário autenticado atual (ou null).
     */
    @Nullable
    String getCurrentUserId();

    /**
     * Gera um novo ID de documento para uma ideia.
     */
    @NonNull
    String getNewIdeiaId();

    // ------------------------------
    // CRUD PRINCIPAL
    // ------------------------------

    /**
     * Cria/Salva uma nova ideia no Firestore.
     * Implementações podem usar createIdea ou saveIdeia; ambos estão expostos para compatibilidade.
     */
    //void createIdeia(@NonNull Ideia ideia, @NonNull ResultCallback<Void> callback);

    /**
     * Salva (sobrescreve) a ideia fornecida.
     * Mantido por compatibilidade com código existente que chama saveIdeia.
     */
    void saveIdeia(@NonNull Ideia ideia, @NonNull ResultCallback<Void> callback);

    /**
     * Atualiza uma ideia existente (pode delegar a saveIdeia/createIdea).
     */
    void updateIdeia(@NonNull Ideia ideia, @NonNull ResultCallback<Void> callback);

    /**
     * Remove uma ideia pelo ID.
     */
    void deleteIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Void> callback);

    /**
     * Busca uma ideia específica pelo ID.
     */
    void getIdeiaById(@NonNull String ideiaId, @NonNull ResultCallback<Ideia> callback);

    /**
     * Busca todas as ideias de um determinado usuário (one-shot).
     */
    void getIdeiasForOwner(@NonNull String ownerId, @NonNull ResultCallback<List<Ideia>> callback);

    // ------------------------------
    // Leitura e escuta de dados
    // ------------------------------

    /**
     * Observa em tempo real as alterações de uma ideia específica.
     */
    @NonNull
    ListenerRegistration listenToIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Ideia> callback);

    /**
     * Observa em tempo real as ideias públicas (em avaliação ou avaliadas).
     */
    @NonNull
    ListenerRegistration listenToPublicIdeias(@NonNull ResultCallback<List<Ideia>> callback);

    /**
     * Observa em tempo real os rascunhos do usuário autenticado.
     */
    @Nullable
    ListenerRegistration listenToDraftIdeias(@NonNull ResultCallback<List<Ideia>> callback);

    void getPublicIdeasCountByUser(String userId, ResultCallback<Integer> callback);
    void getAvaliacoesRecebidasCount(String userId, ResultCallback<Integer> callback);

    // ------------------------------
    // Publicação / Avaliação
    // ------------------------------

    /**
     * Publica a ideia, definindo status e timestamps; opcionalmente vincula um mentor.
     */
    void publicarIdeia(@NonNull String ideiaId, @Nullable String mentorId, @NonNull ResultCallback<Void> callback);

    /**
     * Reverte a publicação da ideia (volta a rascunho).
     */
    void unpublishIdeia(@NonNull String ideiaId, @NonNull ResultCallback<Void> callback);

    /**
     * Salva uma avaliação (lista de mapas com respostas/nota) para a ideia.
     */
    void salvarAvaliacao(@NonNull String ideiaId, @NonNull List<Map<String, Object>> avaliacoes, @NonNull ResultCallback<Void> callback);

    // ------------------------------
    // Upload de arquivos
    // ------------------------------

    /**
     * Envia o arquivo de pitch deck para o armazenamento.
     * Retorna a URL pública após o upload.
     */
    void uploadPitchDeck(@NonNull String ideiaId, @NonNull Uri fileUri, @NonNull ResultCallback<String> callback);

    // ------------------------------
    // Manipulação de Post-Its
    // ------------------------------

    /**
     * Adiciona um novo post-it a uma etapa específica.
     */
    void addPostitToIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt novoPostIt, @NonNull ResultCallback<Void> callback);

    /**
     * Atualiza um post-it existente.
     */
    void updatePostitInIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitAntigo, @NonNull PostIt postitNovo, @NonNull ResultCallback<Void> callback);

    /**
     * Remove um post-it de uma ideia.
     */
    void deletePostitFromIdeia(@NonNull String ideiaId, @NonNull String etapaChave, @NonNull PostIt postitParaApagar, @NonNull ResultCallback<Void> callback);
}
