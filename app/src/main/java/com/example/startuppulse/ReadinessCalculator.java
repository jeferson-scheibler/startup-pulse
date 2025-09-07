package com.example.startuppulse;

import android.text.TextUtils;

import java.util.List;
import java.util.Map;

// Classe utilitária para calcular o Readiness Score de uma Ideia.
public class ReadinessCalculator {

    // Define os pesos de cada critério para o cálculo do score.
    private static final int PESO_CANVAS = 35;
    private static final int PESO_EQUIPE = 20;
    private static final int PESO_METRICAS = 25;
    private static final int PESO_MENTOR = 15;
    private static final int PESO_PITCH_DECK = 5;

    public static ReadinessData calculate(Ideia ideia) {
        if (ideia == null) {
            // Retorna dados zerados se não houver ideia.
            return new ReadinessData(0, false, false, false, false, false);
        }

        // 1. Verifica o preenchimento do Canvas
        boolean canvasCompleto = isCanvasPreenchido(ideia);

        // 2. Verifica se a equipe está definida (lógica a ser implementada)
        // Por agora, vamos assumir 'false' até adicionarmos o campo.
        boolean equipeDefinida = false; // TODO: Implementar lógica real

        // 3. Verifica se as métricas iniciais foram inseridas (lógica a ser implementada)
        boolean metricasIniciais = false; // TODO: Implementar lógica real

        // 4. Verifica se um mentor foi associado à ideia
        boolean validadoPorMentor = !TextUtils.isEmpty(ideia.getMentorId());

        // 5. Verifica se um pitch deck foi anexado (lógica a ser implementada)
        boolean hasPitchDeck = false; // TODO: Implementar lógica real

        // Calcula o score final somando os pesos dos critérios completos.
        int score = 0;
        if (canvasCompleto) score += PESO_CANVAS;
        if (equipeDefinida) score += PESO_EQUIPE;
        if (metricasIniciais) score += PESO_METRICAS;
        if (validadoPorMentor) score += PESO_MENTOR;
        if (hasPitchDeck) score += PESO_PITCH_DECK;

        return new ReadinessData(score, canvasCompleto, equipeDefinida, metricasIniciais, validadoPorMentor, hasPitchDeck);
    }

    /**
     * Verifica se os campos essenciais do Canvas da Ideia estão preenchidos.
     * Um campo é considerado preenchido se a lista de post-its não estiver vazia.
     */
    private static boolean isCanvasPreenchido(Ideia ideia) {
        // --- CORREÇÃO APLICADA AQUI ---
        Map<String, List<PostIt>> canvas = ideia.getPostIts(); // Usando o método correto
        if (canvas == null) return false;

        // Verifica os 9 blocos do Business Model Canvas
        return !isPostItListEmpty(canvas.get("propostaValor")) &&
                !isPostItListEmpty(canvas.get("segmentoClientes")) &&
                !isPostItListEmpty(canvas.get("canais")) &&
                !isPostItListEmpty(canvas.get("relacionamentoClientes")) &&
                !isPostItListEmpty(canvas.get("fontesRenda")) &&
                !isPostItListEmpty(canvas.get("recursosPrincipais")) &&
                !isPostItListEmpty(canvas.get("atividadesChave")) &&
                !isPostItListEmpty(canvas.get("parceriasPrincipais")) &&
                !isPostItListEmpty(canvas.get("estruturaCustos"));
    }

    private static boolean isPostItListEmpty(List<PostIt> postIts) {
        return postIts == null || postIts.isEmpty();
    }
}