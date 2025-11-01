package com.example.startuppulse.common;

/**
 * Interface de callback genérica para operações assíncronas.
 * Com um único método, pode ser usada com expressões lambda.
 * @param <T> o tipo de dado esperado em caso de sucesso.
 */
public interface ResultCallback<T> {
    /**
     * Chamado quando a operação é concluída.
     * @param result O objeto Result contendo o estado de sucesso ou erro.
     */
    void onResult(Result<T> result);
}