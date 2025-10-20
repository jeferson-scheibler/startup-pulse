package com.example.startuppulse;

import android.annotation.SuppressLint;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.databinding.FragmentMentoresBinding;
import com.example.startuppulse.ui.mentor.MentoresViewModel;
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
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MentoresFragment extends Fragment {

    private FragmentMentoresBinding binding;
    private MentoresViewModel viewModel;
    private MentoresAdapter mentoresAdapter;
    private final List<Mentor> allMentores = new ArrayList<>();
    private String selectedArea = "Todas as áreas";
    private GeoCache geoCache;
    private static String lastKnownCity = null;

    // --- Ciclo de Vida do Fragment ---

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configuração do OSMDroid - essencial que seja feito antes de inflar o layout
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMentoresBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(MentoresViewModel.class);
        geoCache = new GeoCache(requireContext());

        setupUI();
        observeViewModel();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.carregarMentores();
        if (binding != null) binding.mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Libera a referência do binding para evitar memory leaks
        binding = null;
    }

    // --- Configuração da UI ---

    private void setupUI() {
        setupMap();
        setupRecyclerView();
        setupFilterUi();

        addPressAnimation(binding.cardStatMentores);
        addPressAnimation(binding.cardStatAreas);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.carregarMentores());
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.getController().setZoom(8.0);
        binding.mapView.getController().setCenter(new GeoPoint(-30.05, -53.50)); // Centro do RS
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.setContentDescription("Mapa com agrupamento de mentores por cidade");
    }

    private void setupRecyclerView() {
        mentoresAdapter = new MentoresAdapter(); // O listener não é mais necessário aqui
        binding.recyclerViewMentores.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewMentores.setAdapter(mentoresAdapter);
        binding.recyclerViewMentores.setHasFixedSize(true);
    }

    private void setupFilterUi() {
        binding.autoCompleteAreaFilter.setInputType(InputType.TYPE_NULL);
        binding.autoCompleteAreaFilter.setKeyListener(null);

        View.OnClickListener showDropdown = v -> binding.autoCompleteAreaFilter.showDropDown();
        binding.autoCompleteAreaFilter.setOnClickListener(showDropdown);
        binding.iconChevron.setOnClickListener(showDropdown);
        binding.filterPill.setOnClickListener(showDropdown);

        binding.autoCompleteAreaFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedArea = (String) parent.getItemAtPosition(position);
            aplicarFiltroEAtualizarUI();
        });
    }

    // --- Observadores e Lógica de Dados ---

    private void observeViewModel() {
        viewModel.mentores.observe(getViewLifecycleOwner(), result -> {
            if (binding == null) return;
            binding.swipeRefreshLayout.setRefreshing(result instanceof Result.Loading);

            if (result instanceof Result.Success) {
                List<Mentor> mentores = ((Result.Success<List<Mentor>>) result).data;
                handleMentoresSuccess(mentores);
            } else if (result instanceof Result.Error) {
                String error = ((Result.Error<List<Mentor>>) result).error.getMessage();
                showErrorSnackbar("Erro ao carregar mentores: " + error);
            }
        });
    }

    private void handleMentoresSuccess(@Nullable List<Mentor> mentores) {
        allMentores.clear();
        if (mentores != null) {
            allMentores.addAll(mentores);
        }

        List<String> opcoes = construirOpcoesDeArea(allMentores);
        ArrayAdapter<String> dropAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, opcoes);
        binding.autoCompleteAreaFilter.setAdapter(dropAdapter);

        if (!opcoes.contains(selectedArea)) {
            selectedArea = "Todas as áreas";
        }
        binding.autoCompleteAreaFilter.setText(selectedArea, false);

        aplicarFiltroEAtualizarUI();
    }

    private void aplicarFiltroEAtualizarUI() {
        if (binding == null) return;

        List<Mentor> visiveis;
        if ("Todas as áreas".equals(selectedArea)) {
            visiveis = new ArrayList<>(allMentores);
        } else {
            visiveis = new ArrayList<>();
            for (Mentor m : allMentores) {
                if (m.getAreas() != null && m.getAreas().contains(selectedArea)) {
                    visiveis.add(m);
                }
            }
        }

        boolean isEmpty = visiveis.isEmpty();
        binding.viewEmptyStateMentores.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewMentores.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        mentoresAdapter.submitList(visiveis);
        updateStats(visiveis);
        adicionarClustersNoMapa(visiveis);
    }

    private List<String> construirOpcoesDeArea(List<Mentor> mentores) {
        Set<String> set = new HashSet<>();
        for (Mentor m : mentores) {
            if (m.getAreas() != null) {
                for (String area : m.getAreas()) {
                    if (area != null && !area.trim().isEmpty()) {
                        set.add(area.trim());
                    }
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

    // --- Métodos do Mapa e Clusters ---

    private void adicionarClustersNoMapa(List<Mentor> mentores) {
        if (binding == null || !isAdded()) return;

        Map<String, List<Mentor>> mentoresPorCidade = new HashMap<>();
        for (Mentor mentor : mentores) {
            String cidade = mentor.getCity();
            String estado = mentor.getState();
            if (cidade != null && estado != null && !cidade.trim().isEmpty() && !estado.trim().isEmpty()) {
                String key = (cidade.trim() + "," + estado.trim()).toLowerCase(Locale.ROOT);
                mentoresPorCidade.computeIfAbsent(key, k -> new ArrayList<>()).add(mentor);
            }
        }

        binding.mapView.getOverlays().clear();

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            final Geocoder geocoder = Geocoder.isPresent() ? new Geocoder(requireContext(), Locale.getDefault()) : null;

            for (Map.Entry<String, List<Mentor>> entry : mentoresPorCidade.entrySet()) {
                if (!isAdded()) break;

                List<Mentor> grupo = entry.getValue();
                String cidadeNome = grupo.get(0).getCity();
                String estadoNome = grupo.get(0).getState();

                GeoCache.Entry cached = geoCache.getFresh(cidadeNome, estadoNome);
                if (cached != null) {
                    GeoPoint point = new GeoPoint(cached.lat, cached.lon);
                    handler.post(() -> addMarker(cidadeNome, grupo.size(), point));
                } else if (geocoder != null) {
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(cidadeNome + ", " + estadoNome, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            double lat = addresses.get(0).getLatitude();
                            double lon = addresses.get(0).getLongitude();
                            geoCache.put(cidadeNome, estadoNome, lat, lon);
                            GeoPoint point = new GeoPoint(lat, lon);
                            handler.post(() -> addMarker(cidadeNome, grupo.size(), point));
                        }
                    } catch (IOException ignored) {}
                }
            }
            handler.post(() -> { if(binding != null) binding.mapView.invalidate(); });
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
        binding.mapView.getOverlays().add(marker);
    }

    private Drawable createClusterIcon(int count) {
        if (!isAdded()) return null;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View markerView = inflater.inflate(R.layout.marker_cluster_view, null);

        TextView markerText = markerView.findViewById(R.id.marker_text);
        markerText.setText(String.valueOf(count));

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    // --- Métodos de UI Auxiliares ---

    private void updateStats(@NonNull List<Mentor> mentoresVisiveis) {
        if (binding == null) return;

        binding.statCountMentores.setText(String.valueOf(mentoresVisiveis.size()));

        int totalAreas;
        if ("Todas as áreas".equals(selectedArea)) {
            HashSet<String> areasUnicas = new HashSet<>();
            for (Mentor m : mentoresVisiveis) {
                if (m.getAreas() != null) {
                    for (String area : m.getAreas()) {
                        if (area != null && !area.trim().isEmpty()) {
                            areasUnicas.add(area.trim());
                        }
                    }
                }
            }
            totalAreas = areasUnicas.size();
        } else {
            totalAreas = mentoresVisiveis.isEmpty() ? 0 : 1;
        }
        binding.statCountAreas.setText(String.valueOf(totalAreas));
    }

    private void showErrorSnackbar(@NonNull String msg) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addPressAnimation(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().translationY(3f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().translationY(0f).setDuration(150).start();
                    break;
            }
            return false;
        });
    }
}