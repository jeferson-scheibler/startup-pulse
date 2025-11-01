package com.example.startuppulse;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.databinding.FragmentPreparacaoInvestidorBinding;
import com.example.startuppulse.ui.preparacao.MetricasEditAdapter;
import com.example.startuppulse.ui.preparacao.PreparacaoInvestidorViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreparacaoInvestidorFragment extends Fragment {

    private FragmentPreparacaoInvestidorBinding binding;
    private PreparacaoInvestidorViewModel viewModel;
    private EquipeEditAdapter equipeAdapter;
    private MetricasEditAdapter metricasAdapter;

    // Launcher para selecionar o arquivo Pitch Deck
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    viewModel.uploadPitchDeck(uri);
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPreparacaoInvestidorBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(PreparacaoInvestidorViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupRecyclerViews() {
        equipeAdapter = new EquipeEditAdapter(membro -> viewModel.removerMembro(membro));
        binding.recyclerEquipe.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerEquipe.setAdapter(equipeAdapter);

        metricasAdapter = new MetricasEditAdapter(
                metrica -> viewModel.removerMetrica(metrica),
                unused -> viewModel.onMetricaEditada()
        );
        binding.recyclerMetricas.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerMetricas.setAdapter(metricasAdapter);
    }

    private void setupClickListeners() {
        binding.btnAdicionarMembro.setOnClickListener(v -> showAddMembroDialog());
        binding.btnAnexarPitch.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        binding.btnAdicionarMetrica.setOnClickListener(v -> viewModel.adicionarMetrica());
        binding.btnSalvarLiberarAcesso.setOnClickListener(v -> viewModel.salvarEFinalizar());
    }

    private void setupObservers() {
        viewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null) return;

            equipeAdapter.submitList(ideia.getEquipe() != null ? new ArrayList<>(ideia.getEquipe()) : new ArrayList<>());
            metricasAdapter.submitList(ideia.getMetricas() != null ? new ArrayList<>(ideia.getMetricas()) : new ArrayList<>());

            boolean hasPitchDeck = !TextUtils.isEmpty(ideia.getPitchDeckUrl());
            binding.textPitchDeckStatus.setText(hasPitchDeck ? "Arquivo anexado com sucesso!" : "Nenhum arquivo anexado.");
            binding.textPitchDeckStatus.setTextColor(ContextCompat.getColor(requireContext(),
                    hasPitchDeck ? R.color.green_success : R.color.delete_red));
        });

        viewModel.readinessData.observe(getViewLifecycleOwner(), readiness -> {
            if (readiness == null) return;

            animateReadinessScore(readiness.getScore());

            updateChecklistItem(binding.checkCanvas, readiness.isCanvasCompleto());
            updateChecklistItem(binding.checkEquipe, readiness.isEquipeDefinida());
            updateChecklistItem(binding.checkMetricas, readiness.isMetricasIniciais());
            updateChecklistItem(binding.checkMentor, readiness.isValidadoPorMentor());
            updateChecklistItem(binding.checkPitchDeck, readiness.hasPitchDeck());
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSalvarLiberarAcesso.setEnabled(!isLoading);
            binding.btnAnexarPitch.setEnabled(!isLoading);
        });

        viewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        viewModel.navigationEvent.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                NavHostFragment.findNavController(this).navigateUp();
            }
        });
    }

    private void animateReadinessScore(int score) {
        String scoreText = score + "%";
        binding.textReadinessScore.setText(scoreText);
        binding.textReadinessScore.setAlpha(0f);
        binding.textReadinessScore.animate().alpha(1f).setDuration(300).start();

        binding.progressReadiness.setProgress(score);
    }

    private void updateChecklistItem(ImageView imageView, boolean isCompleted) {
        if (getContext() == null) return;

        int iconRes = isCompleted ? R.drawable.ic_check_circle : R.drawable.ic_radio_button_unchecked;
        int tintColor = ContextCompat.getColor(requireContext(),
                isCompleted ? R.color.green_success : R.color.delete_red);

        imageView.setImageResource(iconRes);
        imageView.setImageTintList(ColorStateList.valueOf(tintColor));
        imageView.setAlpha(0f);
        imageView.animate().alpha(1f).setDuration(300).start();
    }

    private void showAddMembroDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_membro, null);
        final TextInputEditText inputNome = dialogView.findViewById(R.id.edit_text_nome_membro);
        final TextInputEditText inputFuncao = dialogView.findViewById(R.id.edit_text_funcao_membro);
        final TextInputEditText inputLinkedin = dialogView.findViewById(R.id.edit_text_linkedin_membro);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Adicionar Membro da Equipe")
                .setView(dialogView)
                .setPositiveButton("Adicionar", (dialog, which) -> {
                    String nome = inputNome.getText().toString().trim();
                    String funcao = inputFuncao.getText().toString().trim();
                    String linkedin = inputLinkedin.getText().toString().trim();
                    if (!nome.isEmpty() && !funcao.isEmpty()) {
                        viewModel.adicionarMembro(nome, funcao, linkedin);
                    } else {
                        Toast.makeText(getContext(), "Nome e função são obrigatórios.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
