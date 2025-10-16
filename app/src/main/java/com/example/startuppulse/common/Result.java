// app/src/main/java/com/example/startuppulse/common/Result.java

package com.example.startuppulse.common;

import com.google.firebase.auth.FirebaseUser;

/**
 * Uma classe selada que encapsula os três estados possíveis de uma operação assíncrona:
 * Success (Sucesso), Error (Erro) e Loading (Carregando).
 * O uso de classes aninhadas estáticas permite um padrão similar a "sealed classes" do Kotlin,
 * garantindo type-safety no tratamento dos resultados.
 *
 * @param <T> o tipo do dado em caso de sucesso.
 */
public abstract class Result<T> {

    // Construtor privado para garantir que apenas as classes internas possam herdar.
    private Result() {}

    // Classe interna para representar o estado de Sucesso.
    public static final class Success<T> extends Result<T> {
        public final T data;

        public Success(T data) {
            this.data = data;
        }
    }

    // Classe interna para representar o estado de Erro.
    public static final class Error<T> extends Result<T> {
        public final Exception error;

        public Error(Throwable error) {
            this.error = (Exception) error;
        }
    }

    // Classe interna para representar o estado de Carregamento.
    public static final class Loading<T> extends Result<T> {
        // Esta classe não precisa de campos, sua própria existência representa o estado.
    }
}