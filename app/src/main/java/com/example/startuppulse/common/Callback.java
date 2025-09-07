package com.example.startuppulse.common;

/**
 * Uma interface funcional genérica para lidar com o resultado de uma operação assíncrona.
 * @param <T> O tipo de dado esperado no resultado.
 */
@FunctionalInterface
public interface Callback<T> {
    /**
     * Método chamado quando a operação é concluída.
     * @param result O resultado da operação.
     */
    void onComplete(T result);
}