package com.example.startuppulse;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.data.Mentor;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe utilitária responsável por encontrar os melhores mentores para uma ideia.
 * A lógica de "match" ordena os mentores com base na afinidade de suas áreas de atuação
 * e, como critério de desempate, pela proximidade geográfica.
 * Esta classe não pode ser instanciada.
 */
public final class MentorMatchService {

    /**
     * Construtor privado para impedir a instanciação da classe utilitária.
     */
    private MentorMatchService() {}

    /**
     * Ordena uma lista de mentores e retorna uma NOVA lista ordenada.
     * A ordenação prioriza a afinidade (maior número de áreas em comum, decrescente)
     * e, em seguida, a proximidade geográfica (menor distância, crescente).
     *
     * @param mentores Lista de mentores a ser ordenada. A lista original não é modificada.
     * @param areasDaIdeia Lista de áreas de atuação relevantes para a ideia.
     * @param localizacaoUsuario A localização atual do usuário para cálculo de distância. Pode ser nula.
     * @return Uma nova lista de mentores ordenada de acordo com os critérios.
     */
    public static List<Mentor> ordenarPorAfinidadeEProximidade(
            @NonNull List<Mentor> mentores,
            @NonNull List<String> areasDaIdeia,
            @Nullable Location localizacaoUsuario
    ) {
        if (mentores.isEmpty()) {
            return Collections.emptyList();
        }

        // Cria o comparador composto
        Comparator<Mentor> comparador = Comparator
                // Primeiro critério: Pontos de afinidade, do maior para o menor
                .comparingInt((Mentor m) -> calcularPontosDeAfinidade(m, areasDaIdeia))
                .reversed()
                // Segundo critério (desempate): Distância, da menor para a maior
                .thenComparingDouble((Mentor m) -> calcularDistancia(m, localizacaoUsuario));

        // Usa Stream para criar uma nova lista ordenada, sem modificar a original
        return mentores.stream()
                .sorted(comparador)
                .collect(Collectors.toList());
    }

    /**
     * Calcula a pontuação de afinidade contando o número de áreas em comum entre o mentor e a ideia.
     * A comparação é feita em minúsculas e ignora espaços em branco.
     */
    private static int calcularPontosDeAfinidade(Mentor mentor, List<String> areasDaIdeia) {
        if (mentor.getAreas() == null || areasDaIdeia == null || areasDaIdeia.isEmpty()) {
            return 0;
        }

        // Normaliza e converte as listas para HashSets para uma interseção eficiente (O(n+m))
        HashSet<String> areasNormalizadasMentor = mentor.getAreas().stream()
                .map(String::toLowerCase).map(String::trim).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> areasNormalizadasIdeia = areasDaIdeia.stream()
                .map(String::toLowerCase).map(String::trim).collect(Collectors.toCollection(HashSet::new));

        // Mantém apenas os elementos que existem em ambos os sets
        areasNormalizadasMentor.retainAll(areasNormalizadasIdeia);
        return areasNormalizadasMentor.size(); // O tamanho do set resultante é o número de áreas em comum
    }

    /**
     * Calcula a distância em metros entre o mentor e o usuário.
     * Retorna um valor muito alto se a localização não estiver disponível,
     * empurrando o mentor para o final da lista ordenada.
     */
    private static double calcularDistancia(Mentor mentor, @Nullable Location localizacaoUsuario) {
        if (localizacaoUsuario == null || mentor.getLatitude() == 0 || mentor.getLongitude() == 0) {
            return Double.MAX_VALUE; // Penalidade máxima para mentores sem localização
        }
        float[] resultados = new float[1];
        Location.distanceBetween(
                localizacaoUsuario.getLatitude(), localizacaoUsuario.getLongitude(),
                mentor.getLatitude(), mentor.getLongitude(),
                resultados
        );
        return resultados[0];
    }
}