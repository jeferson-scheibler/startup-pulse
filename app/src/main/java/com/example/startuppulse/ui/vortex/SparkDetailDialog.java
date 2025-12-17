package com.example.startuppulse.ui.vortex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.startuppulse.data.models.Spark;
import com.example.startuppulse.databinding.DialogSparkDetailBinding;
import com.example.startuppulse.util.Event;
import com.example.startuppulse.views.PulseVoteView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SparkDetailDialog extends BottomSheetDialogFragment {

    private DialogSparkDetailBinding binding;
    private SparkDetailViewModel viewModel;
    private Spark spark;

    // Chave para os argumentos
    private static final String ARG_SPARK = "spark";

    /**
     * Cria uma nova instância do Dialog, passando a Faísca como argumento.
     */
    public static SparkDetailDialog newInstance(Spark spark) {
        SparkDetailDialog fragment = new SparkDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SPARK, spark); // Spark precisa de implementar Serializable
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            spark = (Spark) getArguments().getSerializable(ARG_SPARK);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogSparkDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SparkDetailViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (spark == null) {
            Toast.makeText(getContext(), "Erro: Faísca não encontrada.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // 1. Popula a UI
        binding.sparkText.setText(spark.getText());

        // 2. Copia a lógica do PulseVoteView
        binding.pulseVoteView.setOnVoteListener(new PulseVoteView.OnVoteListener() { // Correção 2: A interface é 'OnVoteListener'

            @Override
            public void onVoteConfirmed(float score) {
                // 'score' é um float de 1.0 a 5.0

                // Correção 3: Calcular o 'weight' (1, 2, ou 3) com base no 'score'
                // para que o nosso backend (Etapa 6.1) funcione.
                int weight;
                if (score <= 2.5) {
                    weight = 1; // Pulso Fraco
                } else if (score <= 4.0) {
                    weight = 2; // Pulso Médio
                } else {
                    weight = 3; // Pulso Forte
                }

                // O utilizador confirmou o voto (arrastou)
                viewModel.voteOnSpark(spark.getId(), weight);
            }
        });

        // 3. Configura os observers
        setupObservers();
    }

    private void setupObservers() {
        // Observa o evento de fechar o dialog
        viewModel.getCloseDialogEvent().observe(getViewLifecycleOwner(), shouldClose -> {
            if (shouldClose) {
                dismiss();
            }
        });

        // Observa os Toasts
        viewModel.getToastEvent().observe(getViewLifecycleOwner(), new Event.EventObserver<>(message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}