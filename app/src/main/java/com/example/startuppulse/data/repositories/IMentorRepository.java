package com.example.startuppulse.data.repositories;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;

import java.util.List;
import java.util.Map;

/**
 * Contrato para gerenciamento e consulta de mentores.
 *
 * Responsável por operações CRUD e consultas específicas
 * na coleção /mentores do Firestore.
 *
 * Mantém consistência com o MentorRepository:
 *  - Todos os métodos retornam via ResultCallback.
 *  - Atualizações usam SetOptions.merge() para preservar campos.
 *  - Uploads de imagem são tratados em updateMentorProfile().
 */
public interface IMentorRepository {

    // -----------------------------------------------------
    // LEITURA / CONSULTAS
    // -----------------------------------------------------

    /**
     * Obtém um mentor a partir de seu ID (documentId em /mentores).
     *
     * @param mentorId ID do mentor.
     * @param callback Callback com o resultado.
     */
    void getMentorById(@NonNull String mentorId, @NonNull ResultCallback<Mentor> callback);

    /**
     * Retorna todos os mentores, com opção de excluir um ID específico.
     * Usado em contextos administrativos ou para comparações.
     *
     * @param excludeUserId ID a ser excluído da consulta (pode ser null).
     * @param callback      Callback com o resultado da lista.
     */
    void findAllMentores(@Nullable String excludeUserId, @NonNull ResultCallback<List<Mentor>> callback);

    /**
     * Busca mentores que tenham pelo menos uma das áreas especificadas.
     *
     * @param areas   Lista de áreas de atuação.
     * @param ownerId ID a ser excluído da busca (opcional).
     * @param callback Callback com o resultado.
     */
    void findMentoresByAreas(@NonNull List<String> areas, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback);

    /**
     * Busca mentores por cidade.
     *
     * @param cidade   Nome da cidade.
     * @param ownerId  ID a ser excluído da busca (opcional).
     * @param callback Callback com o resultado.
     */
    void findMentoresByCity(@NonNull String cidade, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback);

    /**
     * Busca mentores por estado.
     *
     * @param estado   Sigla do estado.
     * @param ownerId  ID a ser excluído da busca (opcional).
     * @param callback Callback com o resultado.
     */
    void findMentoresByState(@NonNull String estado, @Nullable String ownerId, @NonNull ResultCallback<List<Mentor>> callback);

    // -----------------------------------------------------
    // CRIAÇÃO / ATUALIZAÇÃO
    // -----------------------------------------------------

    /**
     * Cria ou atualiza o perfil completo de um mentor no Firestore.
     * Usa o ID do usuário autenticado como documentId.
     *
     * @param mentor   Objeto Mentor a ser salvo.
     * @param callback Callback com o ID salvo.
     */
    void saveMentorProfile(@NonNull Mentor mentor, @NonNull ResultCallback<String> callback);

    /**
     * Atualiza campos específicos do mentor autenticado (como verificado ou áreas).
     *
     * @param mentorId   ID do mentor (deve corresponder ao usuário autenticado).
     * @param verificado Novo status de verificação (opcional).
     * @param areas      Novas áreas de atuação (opcional).
     * @param callback   Callback com o resultado.
     */
    void updateMentorFields(@NonNull String mentorId,
                            @Nullable Boolean verificado,
                            @Nullable List<String> areas,
                            @NonNull ResultCallback<Void> callback);

    /**
     * Atualiza o perfil completo do mentor, incluindo upload de imagens (avatar e banner).
     * Este método lida com upload para o Firebase Storage e atualiza o Firestore em sequência.
     *
     * @param mentor       Objeto Mentor atualizado.
     * @param newBannerUri Novo URI de banner (pode ser null).
     * @param callback     Callback com o objeto Mentor atualizado.
     */
    void updateMentorProfile(@NonNull Mentor mentor,
                             @Nullable Uri newBannerUri,
                             @NonNull ResultCallback<Mentor> callback);

    /**
     * Atualiza campos de um mentor baseado em ownerId (quando este campo é usado
     * para vincular o mentor a um usuário).
     *
     * @param ownerId  ID do dono do perfil de mentor.
     * @param updates  Mapa com os campos a atualizar.
     * @param callback Callback com o resultado.
     */
    void updateMentorFieldsByOwnerId(@NonNull String ownerId,
                                     @NonNull Map<String, Object> updates,
                                     @NonNull ResultCallback<Void> callback);

    void getAllMentores(@NonNull ResultCallback<List<User>> callback);
}