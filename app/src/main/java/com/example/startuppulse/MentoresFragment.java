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
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager; // <- AndroidX
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.databinding.FragmentMentoresBinding;
import com.example.startuppulse.util.GeoCache;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MentoresFragment extends Fragment implements MentoresAdapter.OnMentorClickListener {

    private FragmentMentoresBinding binding;
    private MentoresAdapter mentoresAdapter;
    private FirestoreHelper firestoreHelper;
    private final List<Mentor> allMentores = new ArrayList<>();
    private String selectedArea = "Todas as áreas";
    private GeoCache geoCache;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMentoresBinding.inflate(inflater, container, false);
        firestoreHelper = new FirestoreHelper();
        geoCache = new GeoCache(requireContext());

        setupMap();
        setupRecyclerView();
        setupFilterUi();

        // A11y
        binding.mapView.setContentDescription("Mapa com agrupamento de mentores por cidade");
        binding.cardFilter.setContentDescription("Filtro por área de atuação");
        binding.btnClearAreaFilter.setContentDescription("Limpar filtro por área");
        binding.viewEmptyStateMentores.setContentDescription("Nenhum mentor encontrado para o filtro selecionado");

        binding.swipeRefreshLayout.setOnRefreshListener(this::carregarMentores);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
        carregarMentores();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.getController().setZoom(8.0);
        // Centro aproximado do RS
        binding.mapView.getController().setCenter(new GeoPoint(-30.05, -53.50));
        binding.mapView.setMultiTouchControls(true);
    }

    private void setupRecyclerView() {
        binding.recyclerViewMentores.setLayoutManager(new LinearLayoutManager(requireContext()));
        mentoresAdapter = new MentoresAdapter(this);
        binding.recyclerViewMentores.setAdapter(mentoresAdapter);
        binding.recyclerViewMentores.setContentDescription("Lista de mentores");
    }

    private void setupFilterUi() {
        // Deixa o campo realmente como dropdown (sem teclado)
        binding.autoCompleteAreaFilter.setInputType(InputType.TYPE_NULL);
        binding.autoCompleteAreaFilter.setKeyListener(null);
        binding.autoCompleteAreaFilter.setFocusable(false);
        binding.autoCompleteAreaFilter.setOnClickListener(v -> binding.autoCompleteAreaFilter.showDropDown());

        // Adapter vazio – será preenchido ao carregar mentores
        ArrayAdapter<String> dropAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );
        binding.autoCompleteAreaFilter.setAdapter(dropAdapter);

        binding.autoCompleteAreaFilter.setOnItemClickListener((parent, view, position, id) -> {
            String chosen = (String) parent.getItemAtPosition(position);
            selectedArea = chosen;
            aplicarFiltroEAtualizarUI();
        });

        binding.btnClearAreaFilter.setOnClickListener(v -> {
            selectedArea = "Todas as áreas";
            binding.autoCompleteAreaFilter.setText(selectedArea, false);
            aplicarFiltroEAtualizarUI();
        });
    }

    private void carregarMentores() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(true);

        firestoreHelper.getMentoresPublicados((Result<List<Mentor>> r) -> {
            if (binding == null) return;
            binding.swipeRefreshLayout.setRefreshing(false);

            if (!r.isOk()) {
                showErrorSnackbar("Erro ao carregar mentores: " + r.error.getMessage());
                return;
            }

            allMentores.clear();
            if (r.data != null) allMentores.addAll(r.data);

            // (Re)cria as opções do dropdown com base nas áreas existentes
            List<String> opcoes = construirOpcoesDeArea(allMentores);
            ArrayAdapter<String> dropAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    opcoes
            );
            binding.autoCompleteAreaFilter.setAdapter(dropAdapter);

            // Mantém/repõe seleção
            if (!opcoes.contains(selectedArea)) {
                selectedArea = "Todas as áreas";
            }
            binding.autoCompleteAreaFilter.setText(selectedArea, false);

            aplicarFiltroEAtualizarUI();
        });
    }

    private List<String> construirOpcoesDeArea(List<Mentor> mentores) {
        Set<String> set = new HashSet<>();
        for (Mentor m : mentores) {
            List<String> areas = m.getAreas();
            if (areas == null) continue;
            for (String a : areas) {
                if (a != null) {
                    String t = a.trim();
                    if (!t.isEmpty()) set.add(t);
                }
            }
        }
        List<String> out = new ArrayList<>();
        out.add("Todas as áreas");
        List<String> sorted = new ArrayList<>(set);
        sorted.sort(String::compareToIgnoreCase);
        out.addAll(sorted);
        return out;
    }

    private void aplicarFiltroEAtualizarUI() {
        if (binding == null) return;

        List<Mentor> visiveis;
        if ("Todas as áreas".equals(selectedArea)) {
            visiveis = new ArrayList<>(allMentores);
        } else {
            visiveis = new ArrayList<>();
            for (Mentor m : allMentores) {
                List<String> areas = m.getAreas();
                if (areas != null) {
                    for (String a : areas) {
                        if (selectedArea.equalsIgnoreCase(a)) {
                            visiveis.add(m);
                            break;
                        }
                    }
                }
            }
        }

        boolean vazio = visiveis.isEmpty();
        binding.viewEmptyStateMentores.setVisibility(vazio ? View.VISIBLE : View.GONE);
        binding.recyclerViewMentores.setVisibility(vazio ? View.GONE : View.VISIBLE);

        mentoresAdapter.submitList(visiveis);
        adicionarClustersNoMapa(visiveis);
    }

    private void adicionarClustersNoMapa(List<Mentor> mentores) {
        if (binding == null || !isAdded()) return;

        // Agrupa por cidade+estado (case-insensitive)
        Map<String, List<Mentor>> mentoresPorCidade = new HashMap<>();
        for (Mentor mentor : mentores) {
            String cidade = mentor.getCidade();
            String estado = mentor.getEstado();
            if (cidade == null || estado == null) continue;
            cidade = cidade.trim();
            estado = estado.trim();
            if (cidade.isEmpty() || estado.isEmpty()) continue;

            String key = (cidade + "," + estado).toLowerCase(Locale.ROOT);
            mentoresPorCidade.computeIfAbsent(key, k -> new ArrayList<>()).add(mentor);
        }

        binding.mapView.getOverlays().clear();

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean geocoderAvailable = Geocoder.isPresent();
        final Geocoder geocoder = geocoderAvailable ? new Geocoder(requireContext(), Locale.getDefault()) : null;

        executor.execute(() -> {
            for (Map.Entry<String, List<Mentor>> entry : mentoresPorCidade.entrySet()) {
                if (!isAdded() || binding == null) break;

                List<Mentor> grupo = entry.getValue();
                String cidadeNome = grupo.get(0).getCidade();
                String estadoNome = grupo.get(0).getEstado();
                int totalMentores = grupo.size();

                // 1) cache
                GeoCache.Entry cached = geoCache.getFresh(cidadeNome, estadoNome);
                if (cached != null) {
                    GeoPoint point = new GeoPoint(cached.lat, cached.lon);
                    handler.post(() -> addMarker(cidadeNome, totalMentores, point));
                    continue;
                }

                // 2) geocoder (se disponível)
                if (geocoderAvailable) {
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(cidadeNome + ", " + estadoNome, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            double lat = addresses.get(0).getLatitude();
                            double lon = addresses.get(0).getLongitude();
                            geoCache.put(cidadeNome, estadoNome, lat, lon);
                            GeoPoint point = new GeoPoint(lat, lon);
                            handler.post(() -> addMarker(cidadeNome, totalMentores, point));
                        }
                    } catch (IOException ignored) {
                        // ignora falhas de geocoding
                    }
                }
            }
            executor.shutdown();
        });
    }

    private void addMarker(String cidadeNome, int totalMentores, GeoPoint point) {
        if (binding == null) return;
        Marker marker = new Marker(binding.mapView);
        marker.setPosition(point);
        marker.setTitle(cidadeNome + " (" + totalMentores + " " + (totalMentores > 1 ? "mentores" : "mentor") + ")");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(createClusterIcon(totalMentores));
        marker.setOnMarkerClickListener((m, mv) -> {
            String ann = m.getTitle() != null ? m.getTitle() : "Local";
            binding.getRoot().announceForAccessibility(ann);
            return false;
        });
        binding.mapView.getOverlays().add(marker);
        binding.mapView.invalidate();
    }

    private Drawable createClusterIcon(int count) {
        if (!isAdded()) return null;
        LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View markerView = inflater.inflate(R.layout.marker_cluster_view, null);

        TextView markerText = markerView.findViewById(R.id.marker_text);
        markerText.setText(String.valueOf(count));
        markerText.setContentDescription("Quantidade de mentores: " + count);

        markerView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(
                markerView.getMeasuredWidth(),
                markerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    @Override
    public void onMentorClick(Mentor mentor) {
        if (binding == null) return;
        Snackbar sb = Snackbar.make(binding.getRoot(), "Clicou em: " + mentor.getNome(), Snackbar.LENGTH_SHORT);
        sb.addCallback(new Snackbar.Callback() {
            @Override public void onShown(Snackbar transientBottomBar) {
                transientBottomBar.getView().announceForAccessibility("Clicou no mentor " + mentor.getNome());
            }
        });
        sb.show();
    }

    private void showErrorSnackbar(@NonNull String msg) {
        if (binding == null) return;
        Snackbar sb = Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG);
        sb.addCallback(new Snackbar.Callback() {
            @Override public void onShown(Snackbar transientBottomBar) {
                transientBottomBar.getView().announceForAccessibility(msg);
            }
        });
        sb.show();
    }
}