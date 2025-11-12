package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.ResultCallback;

/**
 * Interface para o repositório da feature "Propulsor".
 * Define o contrato para interagir com a IA de análise de faíscas.
 */
public interface IPropulsorRepository {

    /**
     * Envia um texto (faísca) para a Cloud Function 'chat_propulsor' e
     * retorna a análise de mercado da IA.
     *
     * @param text A faísca de ideia do usuário.
     * @param callback O callback que será invocado com o Result<String> contendo
     * a análise ou um erro.
     */
    void getAnalysis(String text, ResultCallback<String> callback);
}