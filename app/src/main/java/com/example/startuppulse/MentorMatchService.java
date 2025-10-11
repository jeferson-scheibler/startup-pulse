package com.example.startuppulse;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class MentorMatchService {

    public static List<Mentor> ordenarPorAfinidadeEProximidade(
            @NonNull List<Mentor> mentores, @NonNull List<String> areasDaIdeia, @Nullable Location localizacaoUsuario
    ) {
        Comparator<Mentor> comparador = Comparator
                .comparingInt((Mentor m) -> calcularPontosDeAfinidade(m, areasDaIdeia))
                .reversed()
                .thenComparingDouble((Mentor m) -> calcularDistancia(m, localizacaoUsuario));
        mentores.sort(comparador);
        return mentores;
    }

    private static int calcularPontosDeAfinidade(Mentor mentor, List<String> areasDaIdeia) {
        if (mentor.getAreas() == null || areasDaIdeia == null || areasDaIdeia.isEmpty()) return 0;

        // CORREÇÃO: Comparação insensível a maiúsculas
        HashSet<String> areasNormalizadasMentor = mentor.getAreas().stream()
                .map(String::toLowerCase).map(String::trim).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> areasNormalizadasIdeia = areasDaIdeia.stream()
                .map(String::toLowerCase).map(String::trim).collect(Collectors.toCollection(HashSet::new));

        areasNormalizadasMentor.retainAll(areasNormalizadasIdeia);
        return areasNormalizadasMentor.size();
    }

    private static double calcularDistancia(Mentor mentor, @Nullable Location localizacaoUsuario) {
        if (localizacaoUsuario == null || mentor.getLatitude() == 0 || mentor.getLongitude() == 0) {
            return Double.MAX_VALUE;
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