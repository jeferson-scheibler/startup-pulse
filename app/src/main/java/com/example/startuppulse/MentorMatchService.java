package com.example.startuppulse;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Serviço utilitário para ranquear mentores de acordo com:
 * 1️⃣ Afinidade de áreas (maior primeiro)
 * 2️⃣ Proximidade geográfica (menor distância primeiro)
 *
 * Usa User (dados compartilháveis) que podem conter dados de mentor
 * via user.getMentorData().
 *
 * Log detalhado adicionado para rastrear cada etapa do processo.
 */
public final class MentorMatchService {

    private static final String TAG = "MentorMatchService";

    private MentorMatchService() {
        // Classe utilitária, sem instância
    }

    /**
     * Ordena mentores por afinidade e distância, retornando nova lista ordenada.
     *
     * @param users Lista de usuários (mentores potenciais).
     * @param areasDaIdeia Áreas associadas à ideia.
     * @param localizacaoUsuario Localização usada como referência (pode ser null).
     * @return Lista ordenada de mentores.
     */
    @NonNull
    public static List<User> ordenarPorAfinidadeEProximidade(
            @NonNull List<User> users,
            @NonNull List<String> areasDaIdeia,
            @Nullable Location localizacaoUsuario
    ) {
        if (users.isEmpty()) {
            Log.w(TAG, "Nenhum mentor recebido para ordenação.");
            return Collections.emptyList();
        }

        Log.d(TAG, "Iniciando ordenação de " + users.size() + " mentores. Localização disponível? " + (localizacaoUsuario != null));
        Log.d(TAG, "Áreas da ideia: " + areasDaIdeia);

        List<User> copia = new ArrayList<>(users);

        // Comparador composto: afinidade (desc) + distância (asc)
        Comparator<User> comparador = (u1, u2) -> {
            int afinidade1 = calcularPontosDeAfinidade(u1, areasDaIdeia);
            int afinidade2 = calcularPontosDeAfinidade(u2, areasDaIdeia);
            int cmpAfinidade = Integer.compare(afinidade2, afinidade1); // maior primeiro
            if (cmpAfinidade != 0) return cmpAfinidade;

            double distancia1 = calcularDistancia(u1, localizacaoUsuario);
            double distancia2 = calcularDistancia(u2, localizacaoUsuario);
            return Double.compare(distancia1, distancia2); // menor primeiro
        };

        Collections.sort(copia, comparador);

        // Log detalhado dos resultados
        for (int i = 0; i < copia.size(); i++) {
            User u = copia.get(i);
            int afinidade = calcularPontosDeAfinidade(u, areasDaIdeia);
            double distancia = calcularDistancia(u, localizacaoUsuario);
            Log.d(TAG, String.format(
                    "Rank #%d → %s | Afinidade: %d | Distância: %.1fm",
                    i + 1,
                    u.getNome() != null ? u.getNome() : "(sem nome)",
                    afinidade,
                    (distancia == Double.MAX_VALUE ? -1 : distancia)
            ));
        }

        Log.i(TAG, "Ordenação concluída. Retornando lista ranqueada.");
        return copia;
    }

    // ------------------------------------------------------------------------
    // 🔸 MÉTODOS AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * Retorna número de áreas em comum entre o mentor e a ideia.
     */
    private static int calcularPontosDeAfinidade(@NonNull User user, @NonNull List<String> areasDaIdeia) {
        List<String> areasUser = user.getAreasDeInteresse();
        if (areasUser == null || areasUser.isEmpty() || areasDaIdeia == null || areasDaIdeia.isEmpty()) {
            return 0;
        }

        HashSet<String> setUser = new HashSet<>();
        for (String area : areasUser) {
            if (area != null && !area.trim().isEmpty()) {
                setUser.add(area.trim().toLowerCase());
            }
        }

        int count = 0;
        for (String area : areasDaIdeia) {
            if (area != null && setUser.contains(area.trim().toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retorna distância (em metros) entre o mentor e a localização do usuário.
     * Retorna Double.MAX_VALUE se não houver dados de localização válidos.
     */
    public static double calcularDistancia(@NonNull User mentor, @Nullable Location referencia) {
        try {
            if (mentor == null) {
                Log.w(TAG, "Mentor nulo ao calcular distância.");
                return -1;
            }

            Mentor mentorData = mentor.getMentorData();
            if (mentorData == null) {
                Log.w(TAG, "Mentor sem dados de localização.");
                return -1;
            }

            double latMentor = mentorData.getLatitude();
            double lonMentor = mentorData.getLongitude();

            // 🔹 Evita coordenadas zeradas (ou padrão sem GPS)
            if (latMentor == 0.0 && lonMentor == 0.0) {
                Log.w(TAG, "Mentor com coordenadas inválidas (0,0): " + mentor.getNome());
                return -1;
            }

            if (referencia == null) {
                Log.w(TAG, "Localização de referência nula.");
                return -1;
            }

            Location locMentor = new Location("mentor");
            locMentor.setLatitude(latMentor);
            locMentor.setLongitude(lonMentor);

            double distancia = referencia.distanceTo(locMentor);

            if (Double.isNaN(distancia) || Double.isInfinite(distancia)) {
                Log.w(TAG, "Distância inválida calculada para " + mentor.getNome());
                return -1;
            }

            return distancia;

        } catch (Exception e) {
            Log.e(TAG, "Erro ao calcular distância: " + e.getMessage(), e);
            return -1;
        }
    }


}
