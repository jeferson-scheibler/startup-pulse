package com.example.startuppulse;

import java.util.*;

public class MentorMatchService {

    public static List<Mentor> ordenarPorAfinidadeELocal(
            List<Mentor> mentores,
            List<String> areasNecessarias,
            String cidadeAutor,
            String estadoAutor
    ) {
        if (mentores == null) mentores = Collections.emptyList();
        Set<String> alvo = new HashSet<>();
        if (areasNecessarias != null) {
            for (String a : areasNecessarias) if (a != null) alvo.add(a.trim().toLowerCase(Locale.ROOT));
        }

        // score por interseção de áreas
        Map<String, Integer> score = new HashMap<>();
        for (Mentor m : mentores) {
            int s = 0;
            List<String> areas = m.getAreas();
            if (areas != null) {
                for (String a : areas) {
                    if (a != null && alvo.contains(a.trim().toLowerCase(Locale.ROOT))) s++;
                }
            }
            score.put(m.getId(), s);
        }
        Comparator<Mentor> cmpScore = (a,b) -> Integer.compare(score.getOrDefault(b.getId(),0), score.getOrDefault(a.getId(),0));

        // partições por local (prioridade: cidade > estado > outros)
        List<Mentor> cidade = new ArrayList<>(), estado = new ArrayList<>(), outros = new ArrayList<>();
        for (Mentor m : mentores) {
            boolean mesmaCidade = eq(m.getCidade(), cidadeAutor);
            boolean mesmoEstado = eq(m.getEstado(), estadoAutor);
            if (mesmaCidade) cidade.add(m);
            else if (mesmoEstado) estado.add(m);
            else outros.add(m);
        }
        cidade.sort(cmpScore);
        estado.sort(cmpScore);
        outros.sort(cmpScore);

        List<Mentor> out = new ArrayList<>(cidade.size() + estado.size() + outros.size());
        out.addAll(cidade); out.addAll(estado); out.addAll(outros);
        return out;
    }

    private static boolean eq(String a, String b) {
        return (a == null && b == null) || (a != null && b != null && a.equalsIgnoreCase(b));
    }
}
