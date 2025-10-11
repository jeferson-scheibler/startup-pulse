package com.example.startuppulse;

import android.text.TextUtils;
import java.util.List;
import java.util.Map;

/**
 * Classe utilitária para calcular o Readiness Score de uma Ideia,
 * indicando o seu nível de prontidão para ser apresentada a investidores.
 */
public class ReadinessCalculator {

    // Define os pesos de cada critério para o cálculo do score.
    private static final int PESO_CANVAS = 35;
    private static final int PESO_EQUIPE = 20;
    private static final int PESO_METRICAS = 25;
    private static final int PESO_MENTOR = 15;
    private static final int PESO_PITCH_DECK = 5;

    // --- NOVO: Define a nota mínima para a validação do mentor ---
    private static final double NOTA_MINIMA_MENTOR = 7.5;

    public static ReadinessData calculate(Ideia ideia) {
        if (ideia == null) {
            return new ReadinessData(0, false, false, false, false, false);
        }

        boolean canvasCompleto = isCanvasPreenchido(ideia);
        boolean equipeDefinida = ideia.getEquipe() != null && !ideia.getEquipe().isEmpty();
        boolean metricasIniciais = ideia.getMetricas() != null && !ideia.getMetricas().isEmpty();

        // --- LÓGICA DE VALIDAÇÃO DO MENTOR CORRIGIDA ---
        boolean validadoPorMentor = isIdeiaValidadaPorMentor(ideia);

        boolean hasPitchDeck = !TextUtils.isEmpty(ideia.getPitchDeckUrl());

        int score = 0;
        if (canvasCompleto) score += PESO_CANVAS;
        if (equipeDefinida) score += PESO_EQUIPE;
        if (metricasIniciais) score += PESO_METRICAS;
        if (validadoPorMentor) score += PESO_MENTOR;
        if (hasPitchDeck) score += PESO_PITCH_DECK;

        return new ReadinessData(score, canvasCompleto, equipeDefinida, metricasIniciais, validadoPorMentor, hasPitchDeck);
    }

    /**
     * LÓGICA CORRIGIDA: Verifica se uma ideia foi validada por um mentor.
     * A validação requer que a ideia tenha sido avaliada e que a média das notas
     * seja igual ou superior à nota mínima definida.
     */
    private static boolean isIdeiaValidadaPorMentor(Ideia ideia) {
        if (ideia == null || !"Avaliada".equals(ideia.getAvaliacaoStatus())) {
            return false;
        }

        // Acessa a lista de avaliações, que agora é uma lista simples de critérios.
        List<Avaliacao> avaliacoes = ideia.getAvaliacoes();
        if (avaliacoes == null || avaliacoes.isEmpty()) {
            return false;
        }

        // Calcula a soma das notas diretamente da lista de avaliações.
        double somaNotas = 0;
        for (Avaliacao criterio : avaliacoes) {
            somaNotas += criterio.getNota();
        }

        // A média é calculada sobre o tamanho da lista de avaliações.
        double media = somaNotas / avaliacoes.size();

        return media >= NOTA_MINIMA_MENTOR;
    }

    private static boolean isCanvasPreenchido(Ideia ideia) {
        Map<String, List<PostIt>> canvas = ideia.getPostIts();
        if (canvas == null) return false;
        return !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_PROPOSTA_VALOR)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_SEGMENTO_CLIENTES)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_CANAIS)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_FONTES_RENDA)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_ATIVIDADES_CHAVE)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS)) &&
                !isPostItListEmpty(canvas.get(CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS));
    }

    private static boolean isPostItListEmpty(List<PostIt> postIts) {
        return postIts == null || postIts.isEmpty();
    }
}
