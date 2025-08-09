package com.example.startuppulse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MentoresFragment extends Fragment implements MentoresAdapter.OnMentorClickListener {

    private MapView mapView;
    private RecyclerView recyclerViewMentores;
    private MentoresAdapter mentoresAdapter;
    private FirestoreHelper firestoreHelper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mentores, container, false);
        firestoreHelper = new FirestoreHelper();
        mapView = view.findViewById(R.id.map_view);
        recyclerViewMentores = view.findViewById(R.id.recycler_view_mentores);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyStateView = view.findViewById(R.id.view_empty_state_mentores);
        setupMap();
        setupRecyclerView();
        swipeRefreshLayout.setOnRefreshListener(this::carregarMentores);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        carregarMentores();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(8.0);
        // Coordenadas geográficas aproximadas do centro do Rio Grande do Sul
        mapView.getController().setCenter(new GeoPoint(-30.05, -53.50));
        mapView.setMultiTouchControls(true);
    }

    private void setupRecyclerView() {
        recyclerViewMentores.setLayoutManager(new LinearLayoutManager(getContext()));
        mentoresAdapter = new MentoresAdapter(new ArrayList<>(), this);
        recyclerViewMentores.setAdapter(mentoresAdapter);
    }

    private void carregarMentores() {
        swipeRefreshLayout.setRefreshing(true);
        firestoreHelper.getMentoresPublicados(new FirestoreHelper.MentoresListener() {
            @Override
            public void onMentoresCarregados(List<Mentor> mentores) {
                swipeRefreshLayout.setRefreshing(false);
                if (mentores.isEmpty()) {
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerViewMentores.setVisibility(View.GONE);
                } else {
                    emptyStateView.setVisibility(View.GONE);
                    recyclerViewMentores.setVisibility(View.VISIBLE);
                    mentoresAdapter.setMentores(mentores);
                    adicionarClustersNoMapa(mentores); // Chama o novo método
                }
            }

            @Override
            public void onError(Exception e) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Erro ao carregar mentores.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void adicionarClustersNoMapa(List<Mentor> mentores) {
        // 1. Agrupa os mentores por cidade
        Map<String, List<Mentor>> mentoresPorCidade = new HashMap<>();
        for (Mentor mentor : mentores) {
            if (mentor.getCidade() != null && !mentor.getCidade().isEmpty() && mentor.getEstado() != null) {
                String cidadeKey = mentor.getCidade().toLowerCase() + "," + mentor.getEstado().toLowerCase();
                mentoresPorCidade.computeIfAbsent(cidadeKey, k -> new ArrayList<>()).add(mentor);
            }
        }

        mapView.getOverlays().clear(); // Limpa marcadores antigos antes de adicionar novos
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());

        executor.execute(() -> {
            // 2. Itera sobre cada cidade agrupada
            for (Map.Entry<String, List<Mentor>> entry : mentoresPorCidade.entrySet()) {
                String cidadeNome = entry.getValue().get(0).getCidade();
                String estadoNome = entry.getValue().get(0).getEstado();
                int totalMentores = entry.getValue().size();

                try {
                    // 3. Obtém a coordenada da cidade
                    List<Address> addresses = geocoder.getFromLocationName(cidadeNome + ", " + estadoNome, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());

                        // 4. Cria o marcador personalizado na thread principal
                        handler.post(() -> {
                            Marker marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setTitle(cidadeNome + " (" + totalMentores + " " + (totalMentores > 1 ? "mentores" : "mentor") + ")");
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); // Centraliza o ícone

                            // 5. Converte o layout XML em um ícone de Drawable
                            marker.setIcon(createClusterIcon(totalMentores));

                            mapView.getOverlays().add(marker);
                            mapView.invalidate(); // Força o mapa a redesenhar
                        });
                    }
                } catch (IOException e) {
                    // Ignora erros de geocoding para não parar o processo
                    e.printStackTrace();
                }
            }
        });
    }

    // Método mágico para converter um layout em um ícone
    private Drawable createClusterIcon(int count) {
        if (getContext() == null) return null;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View markerView = inflater.inflate(R.layout.marker_cluster_view, null);

        TextView markerText = markerView.findViewById(R.id.marker_text);
        markerText.setText(String.valueOf(count));

        markerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    @Override
    public void onMentorClick(Mentor mentor) {
        Toast.makeText(getContext(), "Clicou em: " + mentor.getNome(), Toast.LENGTH_SHORT).show();
    }
}