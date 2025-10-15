package com.example.startuppulse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.FragmentCanvasInicioBinding; // Import para View Binding
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel; // Import do ViewModel
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasInicioFragment extends Fragment {

    // A interface InicioStateListener foi removida.

    private FragmentCanvasInicioBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;

    // Debounce para evitar sobrecarregar o ViewModel com atualizações
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_MS = 300L;
    private Runnable debounceRunnable;
    private boolean suppressWatcher = false;

    /**
     * O método newInstance foi removido. O CanvasPagerAdapter agora pode
     * simplesmente chamar 'new CanvasInicioFragment()'.
     */
    public CanvasInicioFragment() {
        // Construtor público vazio é obrigatório.
    }

    // Os métodos onAttach e onCreate foram simplificados ou removidos.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasInicioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ponto-chave: Obtém a instância do ViewModel COMPARTILHADO do fragment pai.
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);

        setupObservers();
        addInputListeners();
    }

    private void setupObservers() {
        // Observa a Ideia para popular a UI quando os dados chegam ou mudam.
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null) return;
            // Usa 'suppressWatcher' para preencher a UI sem acionar os listeners de volta.
            suppressWatcher = true;
            if (!binding.editTextTituloIdeia.getText().toString().equals(ideia.getNome())) {
                binding.editTextTituloIdeia.setText(ideia.getNome());
            }
            if (!binding.editTextDescricaoIdeia.getText().toString().equals(ideia.getDescricao())) {
                binding.editTextDescricaoIdeia.setText(ideia.getDescricao());
            }
            montarChipsAreas(ideia.getAreasNecessarias());
            suppressWatcher = false;
        });

        // Observa o estado de 'somente leitura'.
        sharedViewModel.isReadOnly.observe(getViewLifecycleOwner(), isReadOnly -> {
            if (isReadOnly != null) {
                setReadOnlyMode(isReadOnly);
            }
        });
    }

    private void addInputListeners() {
        // Runnable que será chamado após o debounce.
        debounceRunnable = () -> {
            if (sharedViewModel != null) {
                sharedViewModel.updateIdeiaBasics(
                        binding.editTextTituloIdeia.getText().toString(),
                        binding.editTextDescricaoIdeia.getText().toString(),
                        coletarAreasSelecionadas()
                );
            }
        };

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppressWatcher) triggerDebounce();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.editTextTituloIdeia.addTextChangedListener(textWatcher);
        binding.editTextDescricaoIdeia.addTextChangedListener(textWatcher);
    }

    private void triggerDebounce() {
        handler.removeCallbacks(debounceRunnable);
        handler.postDelayed(debounceRunnable, DEBOUNCE_MS);
    }

    private void montarChipsAreas(List<String> preSelecionadasList) {
        if (getContext() == null) return;
        binding.chipGroupAreasInicio.removeAllViews();
        String[] areas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        Set<String> preSelecionadas = (preSelecionadasList != null) ? new HashSet<>(preSelecionadasList) : new HashSet<>();
        boolean isReadOnly = Boolean.TRUE.equals(sharedViewModel.isReadOnly.getValue());

        for (String area : areas) {
            Chip chip = new Chip(getContext(), null, com.google.android.material.R.attr.chipStyle);
            chip.setText(area);
            chip.setCheckable(true);
            chip.setChecked(preSelecionadas.contains(area));
            chip.setEnabled(!isReadOnly);
            chip.setOnClickListener(v -> {
                if (!suppressWatcher) triggerDebounce();
            });
            binding.chipGroupAreasInicio.addView(chip);
        }
    }

    private List<String> coletarAreasSelecionadas() {
        List<String> selecionadas = new ArrayList<>();
        for (int i = 0; i < binding.chipGroupAreasInicio.getChildCount(); i++) {
            View child = binding.chipGroupAreasInicio.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                selecionadas.add(((Chip) child).getText().toString());
            }
        }
        return selecionadas;
    }

    private void setReadOnlyMode(boolean isReadOnly) {
        binding.editTextTituloIdeia.setEnabled(!isReadOnly);
        binding.editTextDescricaoIdeia.setEnabled(!isReadOnly);
        for (int i = 0; i < binding.chipGroupAreasInicio.getChildCount(); i++) {
            binding.chipGroupAreasInicio.getChildAt(i).setEnabled(!isReadOnly);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(debounceRunnable); // Limpa o handler
        binding = null;
    }
}