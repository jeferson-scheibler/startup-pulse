package com.example.startuppulse.ui.vortex;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.startuppulse.R;
import com.example.startuppulse.data.models.Spark;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.maplibre.android.MapLibre;
import org.maplibre.android.geometry.LatLng; // Importa o LatLng do MapLibre
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.HeatmapLayer;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VortexFragment extends Fragment implements OnMapReadyCallback {

    private VortexViewModel viewModel;
    private MapView mapView; // MapView do MapLibre
    private MapLibreMap maplibreMap;

    private static final String TAG = "VortexFragment";
    private static final String HEATMAP_SOURCE_ID = "heatmap-source";
    private static final String HEATMAP_LAYER_ID = "heatmap-layer";
    private static final String SPARKS_SOURCE_ID = "sparks-source";
    private static final String SPARKS_LAYER_ID = "sparks-layer";

    public VortexFragment() {
        // Construtor público vazio obrigatório
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inicializa o MapLibre (necessário antes de inflar o layout)
        MapLibre.getInstance(requireContext());
        return inflater.inflate(R.layout.fragment_vortex, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(VortexViewModel.class);

        // Encontra o MapView do MapLibre e gere o seu ciclo de vida
        mapView = view.findViewById(R.id.map_view_maplibre);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setupObservers();
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        this.maplibreMap = map;

        // Define o estilo (MapTiler)
        String styleUrl = "https://api.maptiler.com/maps/dataviz/style.json?key=" + getString(R.string.maptiler_api_key);

        map.setStyle(styleUrl, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // O mapa está pronto, carregue os dados
                Log.d(TAG, "Estilo MapLibre carregado. A carregar dados...");
                viewModel.loadMapData();
            }
        });

        // Listener de clique para os Pinos (Faíscas)
        map.addOnMapClickListener(point -> {
            // Converte o ponto de clique para uma caixa de píxeis
            // e consulta a camada "sparks-layer"
            return map.queryRenderedFeatures(
                    map.getProjection().toScreenLocation(point), SPARKS_LAYER_ID
            ).stream().findFirst().map(feature -> {
                // Encontrou uma faísca!
                // O ID da faísca está nas propriedades do GeoJSON que criámos
                String sparkId = feature.getStringProperty("spark_id");
                String sparkText = feature.getStringProperty("spark_text");

                // TODO: Abrir um BottomSheet com os detalhes
                Toast.makeText(getContext(), "Faísca: " + sparkText, Toast.LENGTH_LONG).show();

                return true; // Evento consumido
            }).orElse(false); // Evento não consumido
        });
    }

    private void setupObservers() {
        // Observador para o Heatmap
        viewModel.getHeatmapLocations().observe(getViewLifecycleOwner(), locations -> {
            if (maplibreMap != null && locations != null && !locations.isEmpty()) {
                addHeatmapLayer(locations);
            }
        });

        // Observador para as Faíscas
        viewModel.getSparkPins().observe(getViewLifecycleOwner(), sparks -> {
            if (maplibreMap != null && sparks != null) {
                addSparkMarkersLayer(sparks);
            }
        });

        // Observador de Erros
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Converte a lista de LatLng (Google) para um GeoJSON Source e adiciona a camada de Heatmap
     */
    private void addHeatmapLayer(List<com.google.android.gms.maps.model.LatLng> googleLocations) {
        Style style = maplibreMap.getStyle();
        if (style == null) return;

        // 1. Converter a lista de DTOs (Google LatLng) para um GeoJSON (formato MapLibre)
        JsonArray features = new JsonArray();
        for (com.google.android.gms.maps.model.LatLng loc : googleLocations) {
            JsonObject feature = new JsonObject();
            feature.addProperty("type", "Feature");
            JsonObject geometry = new JsonObject();
            geometry.addProperty("type", "Point");
            JsonArray coordinates = new JsonArray();
            coordinates.add(loc.longitude);
            coordinates.add(loc.latitude);
            geometry.add("coordinates", coordinates);
            feature.add("geometry", geometry);
            features.add(feature);
        }
        JsonObject featureCollection = new JsonObject();
        featureCollection.addProperty("type", "FeatureCollection");
        featureCollection.add("features", features);

        // 2. Criar ou atualizar a fonte de dados (Source)
        GeoJsonSource source = (GeoJsonSource) style.getSource(HEATMAP_SOURCE_ID);
        if (source == null) {
            source = new GeoJsonSource(HEATMAP_SOURCE_ID, String.valueOf(featureCollection));
            style.addSource(source);
        } else {
            source.setGeoJson(String.valueOf(featureCollection));
        }

        // 3. Adicionar a camada (Layer) de Heatmap, se não existir
        if (style.getLayer(HEATMAP_LAYER_ID) == null) {
            HeatmapLayer heatmapLayer = new HeatmapLayer(HEATMAP_LAYER_ID, HEATMAP_SOURCE_ID);
            heatmapLayer.setProperties(
                    // Configuração de cores (ex: de transparente para vermelho)
                    PropertyFactory.heatmapColor(
                            Expression.interpolate(
                                    Expression.linear(), Expression.heatmapDensity(),
                                    Expression.literal(0.01), Expression.rgba(0, 0, 0, 0.0),
                                    Expression.literal(0.25), Expression.rgba(0, 0, 255, 0.5),
                                    Expression.literal(0.5), Expression.rgba(0, 255, 0, 0.6),
                                    Expression.literal(0.75), Expression.rgba(255, 255, 0, 0.7),
                                    Expression.literal(1.0), Expression.rgba(255, 0, 0, 0.8)
                            )
                    ),
                    // Raio dos pontos
                    PropertyFactory.heatmapRadius(20f),
                    // Intensidade
                    PropertyFactory.heatmapIntensity(0.8f)
            );
            style.addLayer(heatmapLayer); // Adiciona o heatmap
            Log.d(TAG, "Camada de Heatmap adicionada.");
        }
    }

    /**
     * Converte a lista de Faíscas (Sparks) para um GeoJSON Source e adiciona a camada de Pinos (Markers)
     */
    private void addSparkMarkersLayer(List<Spark> sparks) {
        Style style = maplibreMap.getStyle();
        if (style == null) return;

        // 1. Converter a lista de Sparks para um GeoJSON
        JsonArray features = new JsonArray();
        for (Spark spark : sparks) {
            JsonObject feature = new JsonObject();
            feature.addProperty("type", "Feature");

            // Adiciona as propriedades (para o clique)
            JsonObject properties = new JsonObject();
            properties.addProperty("spark_id", spark.getId());
            properties.addProperty("spark_text", spark.getText());
            feature.add("properties", properties);

            // Adiciona a geometria (localização)
            JsonObject geometry = new JsonObject();
            geometry.addProperty("type", "Point");
            JsonArray coordinates = new JsonArray();
            coordinates.add(spark.getLng());
            coordinates.add(spark.getLat());
            geometry.add("coordinates", coordinates);
            feature.add("geometry", geometry);

            features.add(feature);
        }
        JsonObject featureCollection = new JsonObject();
        featureCollection.addProperty("type", "FeatureCollection");
        featureCollection.add("features", features);

        // 2. Criar ou atualizar a fonte de dados (Source)
        GeoJsonSource source = (GeoJsonSource) style.getSource(SPARKS_SOURCE_ID);
        if (source == null) {
            source = new GeoJsonSource(SPARKS_SOURCE_ID, String.valueOf(featureCollection));
            style.addSource(source);
        } else {
            source.setGeoJson(String.valueOf(featureCollection));
        }

        // 3. Adicionar a camada (Layer) de Símbolos, se não existir
        if (style.getLayer(SPARKS_LAYER_ID) == null) {
            // TODO: Adicionar um ícone customizado (ex: ic_pin) ao projeto
            // style.addImage("pin-icon", BitmapFactory.decodeResource(getResources(), R.drawable.ic_pin));

            SymbolLayer symbolLayer = new SymbolLayer(SPARKS_LAYER_ID, SPARKS_SOURCE_ID);
            symbolLayer.setProperties(
                    // PropertyFactory.iconImage("pin-icon"), // Usar ícone customizado
                    PropertyFactory.iconColor(Color.parseColor("#FF0000")), // Fallback: cor vermelha
                    PropertyFactory.iconSize(1.5f),
                    PropertyFactory.iconAllowOverlap(true)
            );

            // Adiciona a camada de Pinos ABAIXO da camada de Heatmap (se existir)
            style.addLayerBelow(symbolLayer, HEATMAP_LAYER_ID);
            Log.d(TAG, "Camada de Faíscas (Pins) adicionada.");
        }
    }

    // --- Gestão do Ciclo de Vida do MapView (Obrigatório) ---
    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) mapView.onDestroy();
        mapView = null;
        maplibreMap = null;
    }
}