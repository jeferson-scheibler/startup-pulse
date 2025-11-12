package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.common.ResultCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementação do IPropulsorRepository.
 * Usa Firebase Cloud Functions para se comunicar com o backend do Propulsor (Gemini).
 */
@Singleton
public class PropulsorRepository implements IPropulsorRepository {

    private final FirebaseFunctions functions;
    private static final String TAG = "PropulsorRepository";

    @Inject
    public PropulsorRepository(FirebaseFunctions functions) {
        this.functions = functions;
    }

    @Override
    public void getAnalysis(String text, ResultCallback<String> callback) {
        // 1. Preparar os dados para enviar à função
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        // data.put("history", ...); // Opcional: Se quiséssemos manter histórico

        // 2. Chamar a Cloud Function 'chat_propulsor'
        functions.getHttpsCallable("chat_propulsor")
                .call(data)
                .addOnSuccessListener(result -> {
                    try {
                        // 3. Processar a resposta de sucesso
                        Map<String, Object> resultMap = (Map<String, Object>) result.getData();
                        String analysis = (String) resultMap.get("analysis_text");

                        if (analysis != null && !analysis.isEmpty()) {
                            callback.onResult(new Result.Success<>(analysis));
                        } else {
                            // Resposta veio, mas o texto está vazio (caso raro)
                            callback.onResult(new Result.Error<>(new Exception("A análise retornou vazia.")));
                        }
                    } catch (Exception e) {
                        // Erro ao "castar" a resposta
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> {
                    // 4. Processar a falha (ex: sem internet, função deu erro, não autenticado)
                    callback.onResult(new Result.Error<>(e));
                });
    }
}