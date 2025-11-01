package com.example.startuppulse.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Utilitário para converter cidade e estado em coordenadas usando a API Nominatim (OpenStreetMap).
 */
public class GeocodeUtils {

    private static final String TAG = "GeocodeUtils";

    /**
     * Retorna as coordenadas (latitude e longitude) da cidade informada.
     *
     * @param cidade Nome da cidade (ex: "Lajeado")
     * @param estado Sigla do estado (ex: "RS")
     * @return double[] {latitude, longitude} ou null se não encontrado
     */
    public static double[] obterCoordenadasPorCidade(String cidade, String estado) {
        try {
            if (cidade == null || estado == null) return null;

            String endereco = URLEncoder.encode(cidade + ", " + estado + ", Brasil", "UTF-8");
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" + endereco;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "StartupPulseApp/1.0 (contato@startuppulse.com)");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, "Erro HTTP: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONArray results = new JSONArray(response.toString());
            if (results.length() > 0) {
                JSONObject obj = results.getJSONObject(0);
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                return new double[]{lat, lon};
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar coordenadas: " + e.getMessage(), e);
        }
        return null;
    }
}
