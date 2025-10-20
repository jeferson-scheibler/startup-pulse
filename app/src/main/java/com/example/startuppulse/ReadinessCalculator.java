package com.example.startuppulse;

import android.text.TextUtils;

import com.example.startuppulse.data.Avaliacao;
import com.example.startuppulse.data.CanvasEtapa;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.PostIt;

import java.util.List;
import java.util.Map;

/**
 * Classe utilitária para calcular o Readiness Score de uma Ideia,
 * indicando o seu nível de prontidão para ser apresentada a investidores.
 */
public class ReadinessCalculator {

    // --- PESOS ATUALIZADOS ---
    // A lógica de negócios foi refinada para melhor refletir o que os investidores valorizam.
    private static final int PESO_CANVAS = 15;        // Reduzido de 35. O Canvas é importante, mas é uma ferramenta interna.
    private static final int PESO_EQUIPE = 25;        // Aumentado de 20. Investidores apostam na equipe.
    private static final int PESO_METRICAS = 25;      // Mantido. Tração inicial é fundamental.
    private static final int PESO_MENTOR = 15;        // Mantido. Validação externa reduz o risco.
    private static final int PESO_PITCH_DECK = 20;    // Aumentado de 5. O Pitch Deck é a porta de entrada para o investidor.

    private static final double NOTA_MINIMA_MENTOR = 7.5;

    public static ReadinessData calculate(Ideia ideia) {
        if (ideia == null) {
            return new ReadinessData(0, false, false, false, false, false);
        }

        boolean canvasCompleto = isCanvasPreenchido(ideia);
        boolean equipeDefinida = ideia.getEquipe() != null && !ideia.getEquipe().isEmpty();
        boolean metricasIniciais = ideia.getMetricas() != null && !ideia.getMetricas().isEmpty();
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
     * Verifica se uma ideia foi validada por um mentor.
     * A validação requer que a ideia tenha sido avaliada e que a média das notas
     * seja igual ou superior à nota mínima definida.
     */
    private static boolean isIdeiaValidadaPorMentor(Ideia ideia) {
        if (ideia == null || !"Avaliada".equals(ideia.getAvaliacaoStatus())) {
            return false;
        }

        List<Avaliacao> avaliacoes = ideia.getAvaliacoes();
        if (avaliacoes == null || avaliacoes.isEmpty()) {
            return false;
        }

        double somaNotas = 0;
        for (Avaliacao criterio : avaliacoes) {
            somaNotas += criterio.getNota();
        }

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