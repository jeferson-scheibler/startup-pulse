package com.example.startuppulse.data.repositories;

import android.util.Log;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.Spark;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SparkRepository implements ISparkRepository {

    private final FirebaseFunctions functions;
    private final Gson gson; // Usar Gson para facilitar o mapeamento

    private static final String TAG = "SparkRepository";

    @Inject
    public SparkRepository(FirebaseFunctions functions) {
        this.functions = functions;
        this.gson = new Gson();
    }

    @Override
    public void getIdeaLocations(ResultCallback<List<LatLng>> callback) {
        functions.getHttpsCallable("get_idea_locations")
                .call()
                .addOnSuccessListener(result -> {
                    try {
                        Map<String, Object> data = (Map<String, Object>) result.getData();
                        List<Map<String, Double>> locationsMap = (List<Map<String, Double>>) data.get("locations");

                        List<LatLng> latLngList = new ArrayList<>();
                        if (locationsMap != null) {
                            for (Map<String, Double> loc : locationsMap) {
                                latLngList.add(new LatLng(loc.get("lat"), loc.get("lng")));
                            }
                        }
                        callback.onResult(new Result.Success<>(latLngList));
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao parsear getIdeaLocations", e);
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao chamar getIdeaLocations", e);
                    callback.onResult(new Result.Error<>(e));
                });
    }

    @Override
    public void getPublicSparks(ResultCallback<List<Spark>> callback) {
        functions.getHttpsCallable("get_public_sparks")
                .call()
                .addOnSuccessListener(result -> {
                    try {
                        Map<String, Object> data = (Map<String, Object>) result.getData();
                        // Mapeamento robusto usando Gson
                        String jsonSparks = gson.toJson(data.get("sparks"));
                        Type sparkListType = new TypeToken<ArrayList<Spark>>(){}.getType();
                        List<Spark> sparks = gson.fromJson(jsonSparks, sparkListType);

                        callback.onResult(new Result.Success<>(sparks != null ? sparks : new ArrayList<>()));
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao parsear getPublicSparks", e);
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao chamar getPublicSparks", e);
                    callback.onResult(new Result.Error<>(e));
                });
    }

    @Override
    public void createSpark(String text, double lat, double lng, ResultCallback<String> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("lat", lat);
        data.put("lng", lng);

        functions.getHttpsCallable("criar_spark")
                .call(data)
                .addOnSuccessListener(result -> {
                    try {
                        Map<String, Object> resultMap = (Map<String, Object>) result.getData();
                        if (resultMap != null && resultMap.containsKey("id")) {
                            String sparkId = (String) resultMap.get("id");
                            callback.onResult(new Result.Success<>(sparkId));
                        } else {
                            // O backend deu "sucesso" (HTTP 200) mas não
                            // retornou um ID. Isto é um erro.
                            Log.w(TAG, "createSpark 'sucesso' mas sem ID. Payload: " + resultMap);
                            callback.onResult(new Result.Error<>(new Exception("A resposta do servidor estava incompleta.")));
                        }
                    } catch (Exception e) {
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> {
                    // O App Check (agora instalado) irá provavelmente
                    // falhar aqui se houver um problema.
                    Log.e(TAG, "createSpark falhou (addOnFailureListener)", e);
                    callback.onResult(new Result.Error<>(e));
                });
    }

    @Override
    public void voteSpark(String sparkId, int weight, ResultCallback<String> callback) { // <--- MUDANÇA (int weight)
        Map<String, Object> data = new HashMap<>();
        data.put("sparkId", sparkId);
        data.put("weight", weight);

        functions.getHttpsCallable("votar_spark")
                .call(data)
                .addOnSuccessListener(result -> {
                    try {
                        Map<String, Object> resultMap = (Map<String, Object>) result.getData();
                        String status = (String) resultMap.get("status");
                        callback.onResult(new Result.Success<>(status)); // Retorna "success" ou "already_voted"
                    } catch (Exception e) {
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}