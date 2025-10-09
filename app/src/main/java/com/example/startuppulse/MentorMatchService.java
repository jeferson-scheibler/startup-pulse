package com.example.startuppulse;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MentorMatchService {

    /**
     * Ordena uma lista de mentores com base em dois critérios principais:
     * 1. Afinidade de Área: Mentores que correspondem a mais áreas da ideia vêm primeiro.
     * 2. Proximidade Geográfica: Para mentores com a mesma afinidade, o mais próximo fisicamente vem primeiro.
     *
     * @param mentores A lista de mentores candidatos.
     * @param areasDaIdeia As áreas de especialização que a ideia necessita.
     * @param localizacaoUsuario A localização atual do utilizador.
     * @return Uma nova lista de mentores ordenada do melhor para o pior candidato.
     */
    public static List<Mentor> ordenarPorAfinidadeEProximidade(
            @NonNull List<Mentor> mentores,
            @NonNull List<String> areasDaIdeia,
            @Nullable Location localizacaoUsuario
    ) {
        // Usa o comparador do Java 8+ para uma ordenação complexa e profissional
        Comparator<Mentor> comparador = Comparator
                // Primeiro critério: ordenar por número de áreas em comum (do maior para o menor)
                .comparingInt((Mentor m) -> calcularPontosDeAfinidade(m, areasDaIdeia))
                .reversed()
                // Segundo critério (desempate): ordenar por distância (do menor para o maior)
                .thenComparingDouble((Mentor m) -> calcularDistancia(m, localizacaoUsuario));

        mentores.sort(comparador);
        return mentores;
    }

    /**
     * Calcula a "pontuação" de afinidade de um mentor com base nas áreas da ideia.
     * @return O número de áreas que o mentor e a ideia têm em comum.
     */
    private static int calcularPontosDeAfinidade(Mentor mentor, List<String> areasDaIdeia) {
        if (mentor.getAreas() == null || areasDaIdeia == null || areasDaIdeia.isEmpty()) {
            return 0;
        }
        int pontos = 0;
        for (String areaMentor : mentor.getAreas()) {
            if (areasDaIdeia.contains(areaMentor)) {
                pontos++;
            }
        }
        return pontos;
    }

    /**
     * Calcula a distância em metros entre um mentor e a localização do utilizador.
     * @return A distância em metros, ou um valor muito alto se a localização for desconhecida.
     */
    private static double calcularDistancia(Mentor mentor, @Nullable Location localizacaoUsuario) {
        if (localizacaoUsuario == null || mentor.getLatitude() == 0 || mentor.getLongitude() == 0) {
            // Se não tivermos a localização de um dos dois, coloca este mentor no fim da lista
            return Double.MAX_VALUE;
        }
        float[] resultados = new float[1];
        Location.distanceBetween(
                localizacaoUsuario.getLatitude(),
                localizacaoUsuario.getLongitude(),
                mentor.getLatitude(),
                mentor.getLongitude(),
                resultados
        );
        return resultados[0]; // Distância em metros
    }
}