package com.example.startuppulse.ui.vortex;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.startuppulse.R;
import com.example.startuppulse.data.models.Spark;

import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.expressions.Expression;
import org.maplibre.android.style.layers.HeatmapLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VortexFragment extends Fragment implements OnMapReadyCallback {

    private VortexViewModel viewModel;
    private MapView mapView;
    private MapLibreMap maplibreMap;

    private static final String TAG = "VortexFragment";

    private static final String HEATMAP_SOURCE_ID = "heatmap-source";
    private static final String SPARKS_SOURCE_ID  = "sparks-source";
    private static final String HEATMAP_LAYER_ID  = "heatmap-layer";
    private static final String SPARKS_LAYER_ID   = "sparks-layer";
    private static final String SPARK_ICON_ID     = "spark-icon-id";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MapLibre.getInstance(requireContext());
        return inflater.inflate(R.layout.fragment_vortex, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(VortexViewModel.class);

        mapView = view.findViewById(R.id.map_view_maplibre);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap map) {
        this.maplibreMap = map;

        String styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=" +
                getString(R.string.maptiler_api_key);

        map.setStyle(styleUrl, this::configureStyleFully);

        map.addOnMapClickListener(point -> {
            return map.queryRenderedFeatures(
                    map.getProjection().toScreenLocation(point), SPARKS_LAYER_ID
            ).stream().findFirst().map(feature -> {

                // --- INÍCIO DA CORREÇÃO ---
                // Obtenha os dados do LiveData, não do 'feature'
                List<Spark> sparks = viewModel.getSparkPins().getValue();
                if (sparks == null) return false;

                String sparkId = feature.getStringProperty("spark_id");

                // Encontra o objeto Spark completo na nossa lista
                Spark sparkClicada = null;
                for (Spark s : sparks) {
                    if (s.getId().equals(sparkId)) {
                        sparkClicada = s;
                        break;
                    }
                }

                if (sparkClicada != null) {
                    // ABRE O NOVO BOTTOM SHEET
                    SparkDetailDialog.newInstance(sparkClicada)
                            .show(getChildFragmentManager(), "SparkDetailDialog");
                } else {
                    Toast.makeText(getContext(), "Erro: Faísca não encontrada.", Toast.LENGTH_SHORT).show();
                }
                // --- FIM DA CORREÇÃO ---

                return true; // Evento consumido
            }).orElse(false); // Evento não consumido
        });
    }

    private void configureStyleFully(@NonNull Style style) {
        Log.d(TAG, "Estilo carregado completamente.");

        // 1) Registrar a imagem depois que o estilo já está ativo
        Bitmap icon = getBitmapFromVectorDrawable(R.drawable.ic_location_pin);
        style.addImage(SPARK_ICON_ID, icon, true);

        // 2) Criar as fontes
        style.addSource(new GeoJsonSource(HEATMAP_SOURCE_ID));
        style.addSource(new GeoJsonSource(SPARKS_SOURCE_ID));

        // 3) Adicionar heatmap
        HeatmapLayer heatmap = new HeatmapLayer(HEATMAP_LAYER_ID, HEATMAP_SOURCE_ID);
        heatmap.setProperties(
                PropertyFactory.heatmapOpacity(0.9f),
                PropertyFactory.heatmapColor(
                        Expression.interpolate(
                                Expression.linear(), Expression.heatmapDensity(),
                                Expression.literal(0),   Expression.rgba(0, 0, 0, 0),
                                Expression.literal(0.5), Expression.rgba(0, 255, 0, 0.7),
                                Expression.literal(1.0), Expression.rgba(255, 0, 0, 0.9)
                        )
                ),
                PropertyFactory.heatmapRadius(22f)
        );
        style.addLayer(heatmap);

        // 4) Adicionar camada de pins (agora funciona!)
        SymbolLayer layer = new SymbolLayer(SPARKS_LAYER_ID, SPARKS_SOURCE_ID);
        layer.setProperties(
                PropertyFactory.iconImage(SPARK_ICON_ID),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconSize(1.0f)
        );
        style.addLayerAbove(layer, HEATMAP_LAYER_ID);

        // 5) Observers agora funcionam
        setupObservers();

        // 6) Carregar dados
        viewModel.loadMapData();

        // 7) Click nos pins
        maplibreMap.addOnMapClickListener(point -> {
            List<Feature> list = maplibreMap.queryRenderedFeatures(
                    maplibreMap.getProjection().toScreenLocation(point),
                    SPARKS_LAYER_ID
            );
            if (!list.isEmpty()) {
                String txt = list.get(0).getStringProperty("spark_text");
                Toast.makeText(getContext(), "Faísca: " + txt, Toast.LENGTH_LONG).show();
                return true;
            }
            return false;
        });
    }

    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), drawableId);

        if (drawable == null) {
            Log.e(TAG, "ERRO: drawable não encontrado.");
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void setupObservers() {
        viewModel.getHeatmapLocations().observe(getViewLifecycleOwner(), list -> {
            if (list == null) return;
            updateHeatmap(list);
        });

        viewModel.getSparkPins().observe(getViewLifecycleOwner(), sparks -> {
            if (sparks == null) return;
            updatePins(sparks);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateHeatmap(List<com.google.android.gms.maps.model.LatLng> list) {
        Style style = maplibreMap.getStyle();
        if (style == null) return;

        List<Feature> features = new ArrayList<>();
        for (var l : list) {
            features.add(Feature.fromGeometry(Point.fromLngLat(l.longitude, l.latitude)));
        }

        GeoJsonSource src = style.getSourceAs(HEATMAP_SOURCE_ID);
        if (src != null) src.setGeoJson(FeatureCollection.fromFeatures(features));
    }

    private void updatePins(List<Spark> sparks) {
        Style style = maplibreMap.getStyle();
        if (style == null) return;

        List<Feature> features = new ArrayList<>();
        for (Spark s : sparks) {
            Feature f = Feature.fromGeometry(Point.fromLngLat(s.getLng(), s.getLat()));
            f.addStringProperty("spark_id", s.getId());
            f.addStringProperty("spark_text", s.getText());
            features.add(f);
        }

        GeoJsonSource src = style.getSourceAs(SPARKS_SOURCE_ID);
        if (src != null) src.setGeoJson(FeatureCollection.fromFeatures(features));
    }

    // MapView lifecycle
    @Override public void onStart(){super.onStart();mapView.onStart();}
    @Override public void onResume(){super.onResume();mapView.onResume();}
    @Override public void onPause(){super.onPause();mapView.onPause();}
    @Override public void onStop(){super.onStop();mapView.onStop();}
    @Override public void onLowMemory(){super.onLowMemory();mapView.onLowMemory();}
    @Override public void onDestroyView(){super.onDestroyView();mapView.onDestroy();}
}
