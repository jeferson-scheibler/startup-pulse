package com.example.startuppulse.data.repositories;

import androidx.annotation.Nullable;
import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.Investor;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import java.util.List;
import java.util.Map;

/**
 * Interface para o repositório de dados de Investidores.
 * Define o contrato para injeção de dependência (Hilt).
 */
public interface IInvestorRepository {

    /**
     * Busca os detalhes de um único investidor pelo seu ID.
     */
    void getInvestorDetails(String investorId, ResultCallback<Investor> callback);

    /**
     * Busca a lista COMPLETA de todos os investidores.
     * Use com cautela, prefira a paginação.
     */
    void getInvestidores(ResultCallback<List<Investor>> callback);

    /**
     * Busca uma lista paginada de investidores, ordenada pelo nome.
     *
     * @param pageSize    O número de documentos a serem buscados.
     * @param lastVisible O DocumentSnapshot do último item da página anterior (null para a primeira página).
     * @param callback    O callback que será chamado com o InvestorPagingResult.
     */
    void getInvestidoresPaginados(int pageSize, @Nullable DocumentSnapshot lastVisible, @Nullable List<String> filterAreas, ResultCallback<InvestorPagingResult> callback);

    /**
     * Cria o documento inicial do investidor no Firestore (para verificação).
     */
    void createInvestorDocument(Investor investor, ResultCallback<Void> callback);

    /**
     * Ouve as mudanças em tempo real em um documento de investidor (para o fluxo de verificação).
     */
    void listenForInvestorVerification(String uid, EventListener<DocumentSnapshot> listener);

    /**
     * Para de ouvir as mudanças em um documento de investidor.
     */
    void stopListening(String uid);

    /**
     * Atualiza os campos públicos do perfil de um investidor após o onboarding.
     * @param uid O ID do investidor (documento).
     * @param profileData Um Map contendo apenas os campos a serem atualizados.
     * @param callback Callback de sucesso ou falha.
     */
    void updateProfileDetails(String uid, Map<String, Object> profileData, ResultCallback<Void> callback);
}