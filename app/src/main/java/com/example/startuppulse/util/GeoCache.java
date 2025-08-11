package com.example.startuppulse.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

public class GeoCache {

    private static final String PREFS_NAME = "geo_cache_prefs";
    private static final String KEY_PREFIX = "geo:";

    // 7 dias em ms
    private static final long DEFAULT_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    private final SharedPreferences prefs;
    private final long ttlMs;

    public static class Entry {
        public final double lat;
        public final double lon;
        public final long   ts; // epoch millis
        public Entry(double lat, double lon, long ts) {
            this.lat = lat; this.lon = lon; this.ts = ts;
        }
    }

    public GeoCache(Context ctx) {
        this(ctx, DEFAULT_TTL_MS);
    }

    public GeoCache(Context ctx, long ttlMs) {
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.ttlMs = ttlMs;
    }

    private String key(String cidade, String estado) {
        String c = (cidade == null ? "" : cidade.trim().toLowerCase());
        String e = (estado == null ? "" : estado.trim().toLowerCase());
        return KEY_PREFIX + c + "," + e;
    }

    /** Retorna entrada fresca (não expirada) ou null. */
    @Nullable
    public Entry getFresh(String cidade, String estado) {
        String k = key(cidade, estado);
        String v = prefs.getString(k, null);
        if (v == null) return null;
        String[] parts = v.split("\\|");
        if (parts.length != 3) return null;
        try {
            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);
            long   ts  = Long.parseLong(parts[2]);
            if (System.currentTimeMillis() - ts <= ttlMs) {
                return new Entry(lat, lon, ts);
            } else {
                // expirado — apaga silenciosamente
                prefs.edit().remove(k).apply();
                return null;
            }
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Salva/atualiza coordenadas da cidade com timestamp atual. */
    public void put(String cidade, String estado, double lat, double lon) {
        if (TextUtils.isEmpty(cidade) || TextUtils.isEmpty(estado)) return;
        String k = key(cidade, estado);
        String v = lat + "|" + lon + "|" + System.currentTimeMillis();
        prefs.edit().putString(k, v).apply();
    }

    /** Limpa tudo (opcional). */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}