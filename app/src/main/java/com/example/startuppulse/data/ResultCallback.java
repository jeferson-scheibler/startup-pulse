package com.example.startuppulse.data;

import com.example.startuppulse.common.Result;

/**
 * Interface de callback genérica e padronizada para retornar o resultado de operações assíncronas.
 * Ela trabalha diretamente com a classe selada 'Result', encapsulando todos os estados possíveis
 * (Success, Error, Loading) em um único objeto.
 *
 * @param <T> o tipo de dado esperado em caso de sucesso.
 */
public interface ResultCallback<T> {
    /**
     * Chamado quando a operação é concluída, seja com sucesso ou com erro.
     * @param result O objeto Result contendo o estado final da operação.
     */
    void onResult(Result<T> result);
}