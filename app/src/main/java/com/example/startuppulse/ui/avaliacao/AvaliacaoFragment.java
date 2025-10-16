package com.example.startuppulse.ui.avaliacao;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.startuppulse.databinding.FragmentAvaliacaoBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AvaliacaoFragment extends Fragment {

    private FragmentAvaliacaoBinding binding;
    private AvaliacaoViewModel viewModel;
    private AvaliacaoAdapter adapter;
    private final List<CriterioAvaliacao> criteriosList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAvaliacaoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AvaliacaoViewModel.class);

        setupToolbar();
        setupCriterios();
        setupRecyclerView();
        setupClickListeners();
        setupObservers();
        updateAverageScore();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupToolbar() {
        binding.toolbar.setTitle("Avaliar Ideia");
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupCriterios() {
        criteriosList.clear();
        criteriosList.add(new CriterioAvaliacao("Problema e Solução", "A ideia resolve um problema real de forma eficaz?"));
        criteriosList.add(new CriterioAvaliacao("Mercado Potencial", "O mercado para esta solução é grande e acessível?"));
        criteriosList.add(new CriterioAvaliacao("Originalidade", "Qual o nível de inovação e diferenciação da ideia?"));
        criteriosList.add(new CriterioAvaliacao("Modelo de Negócio", "Existe um caminho claro para a sustentabilidade financeira?"));
    }

    private void setupRecyclerView() {
        adapter = new AvaliacaoAdapter(criteriosList, this::updateAverageScore);
        binding.recyclerViewCriterios.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCriterios.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnEnviarAvaliacao.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Enviar avaliação?")
                    .setMessage("Após o envio, o autor da ideia receberá seu feedback.")
                    .setPositiveButton("Enviar", (d, w) -> viewModel.enviarAvaliacao(adapter.getAvaliacoesAsList()))
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void setupObservers() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.btnEnviarAvaliacao.setEnabled(!isLoading);
            binding.btnEnviarAvaliacao.setText(isLoading ? "Enviando..." : "Enviar Avaliação");
        });

        viewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.closeScreenEvent.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void updateAverageScore() {
        if (criteriosList.isEmpty()) return;
        float total = 0f;
        for (CriterioAvaliacao c : criteriosList) total += c.nota;
        float media = total / criteriosList.size();
        binding.textAverageScore.setText(String.format(Locale.getDefault(), "%.1f", media));
    }
}