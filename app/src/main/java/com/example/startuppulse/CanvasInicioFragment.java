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
import com.example.startuppulse.databinding.FragmentCanvasInicioBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasInicioFragment extends Fragment {

    private FragmentCanvasInicioBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_MS = 350L;
    private Runnable debounceRunnable;
    private boolean suppressWatcher = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasInicioBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);

        setupInputListeners();
        setupObservers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        debounceHandler.removeCallbacks(debounceRunnable); // Garante a limpeza do handler
        binding = null;
    }

    private void setupObservers() {
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null || binding == null) return;

            // Determina o estado de apenas leitura a partir do status da ideia
            boolean isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;

            // Previne que os TextWatchers sejam acionados ao popular a UI
            suppressWatcher = true;

            // Atualiza os campos de texto se houver diferença
            if (!binding.editTextTituloIdeia.getText().toString().equals(ideia.getNome())) {
                binding.editTextTituloIdeia.setText(ideia.getNome());
            }
            if (!binding.editTextDescricaoIdeia.getText().toString().equals(ideia.getDescricao())) {
                binding.editTextDescricaoIdeia.setText(ideia.getDescricao());
            }

            // Atualiza o modo de edição e os chips
            setReadOnlyMode(isReadOnly);
            montarChipsAreas(ideia.getAreasNecessarias(), isReadOnly);

            suppressWatcher = false;
        });
    }

    private void setupInputListeners() {
        debounceRunnable = () -> {
            if (sharedViewModel != null && binding != null) {
                sharedViewModel.updateIdeiaBasics(
                        binding.editTextTituloIdeia.getText().toString().trim(),
                        binding.editTextDescricaoIdeia.getText().toString().trim(),
                        coletarAreasSelecionadas()
                );
            }
        };

        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppressWatcher) triggerDebounce();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.editTextTituloIdeia.addTextChangedListener(textWatcher);
        binding.editTextDescricaoIdeia.addTextChangedListener(textWatcher);
    }

    private void triggerDebounce() {
        debounceHandler.removeCallbacks(debounceRunnable);
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
    }

    private void montarChipsAreas(List<String> preSelecionadasList, boolean isReadOnly) {
        if (getContext() == null) return;
        binding.chipGroupAreasInicio.removeAllViews();
        String[] allAreas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        Set<String> preSelecionadas = (preSelecionadasList != null) ? new HashSet<>(preSelecionadasList) : new HashSet<>();

        for (String area : allAreas) {
            Chip chip = new Chip(getContext()); // Usar o construtor padrão é suficiente
            chip.setText(area);
            chip.setCheckable(true);
            chip.setChecked(preSelecionadas.contains(area));
            chip.setEnabled(!isReadOnly); // Desabilita o chip se estiver em modo de leitura
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
        // A lógica de habilitar/desabilitar os chips já está em 'montarChipsAreas'
    }
}