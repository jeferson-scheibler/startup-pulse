package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.Spark;
import com.google.android.gms.maps.model.LatLng; // Usando LatLng do Google como DTO

import java.util.List;
import java.util.Map;

/**
 * Repositório para todas as interações do Vórtex (Faíscas e Mapa).
 */
public interface ISparkRepository {

    /**
     * Busca as coordenadas (LatLng) de todas as ideias públicas para o heatmap.
     * Chama a função 'get_idea_locations'.
     */
    void getIdeaLocations(ResultCallback<List<LatLng>> callback);

    /**
     * Busca a lista de faíscas (Sparks) públicas para os pinos do mapa.
     * Chama a função 'get_public_sparks'.
     */
    void getPublicSparks(ResultCallback<List<Spark>> callback);

    /**
     * Envia uma nova faísca para ser publicada anonimamente no Vórtex.
     * Chama a função 'criar_spark'.
     */
    void createSpark(String text, double lat, double lng, ResultCallback<String> callback);

    /**
     * Registra um "pulso" (voto) em uma faísca.
     * Chama a função 'votar_spark'.
     */
    void voteSpark(String sparkId, ResultCallback<String> callback);
}