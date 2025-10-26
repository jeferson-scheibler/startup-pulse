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

import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.databinding.FragmentCanvasInicioBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // <-- MUDANÇA: Import necessário

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
        debounceHandler.removeCallbacks(debounceRunnable);
        binding = null;
    }

    private void setupObservers() {
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null || binding == null) return;

            boolean isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;
            suppressWatcher = true;

            if (!binding.editTextTituloIdeia.getText().toString().equals(ideia.getNome())) {
                binding.editTextTituloIdeia.setText(ideia.getNome());
            }
            if (!binding.editTextDescricaoIdeia.getText().toString().equals(ideia.getDescricao())) {
                binding.editTextDescricaoIdeia.setText(ideia.getDescricao());
            }

            setReadOnlyMode(isReadOnly);
            // <-- MUDANÇA: Chama o novo método para exibir chips
            exibirChipsSelecionados(ideia.getAreasNecessarias(), isReadOnly);

            suppressWatcher = false;
        });
    }

    private void setupInputListeners() {
        debounceRunnable = () -> {
            if (sharedViewModel != null && binding != null && sharedViewModel.ideia.getValue() != null) {
                // <-- MUDANÇA: A seleção de áreas foi removida daqui.
                // O debounce agora salva apenas os textos, usando as áreas
                // que já estão salvas no ViewModel.
                sharedViewModel.updateIdeiaBasics(
                        binding.editTextTituloIdeia.getText().toString().trim(),
                        binding.editTextDescricaoIdeia.getText().toString().trim(),
                        sharedViewModel.ideia.getValue().getAreasNecessarias() // Passa a lista existente
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

        // <-- MUDANÇA: Adiciona o listener para o novo botão
        binding.btnSelecionarAreasInicio.setOnClickListener(v -> {
            abrirDialogSelecaoAreas();
        });
    }

    private void triggerDebounce() {
        debounceHandler.removeCallbacks(debounceRunnable);
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
    }

    // <-- MUDANÇA: Método renomeado e com lógica totalmente diferente
    /**
     * Atualiza o ChipGroup para *exibir* apenas as áreas que foram selecionadas.
     * Estes chips não são clicáveis para seleção.
     */
    private void exibirChipsSelecionados(List<String> areasSelecionadas, boolean isReadOnly) {
        if (getContext() == null || binding == null) return;
        binding.chipGroupAreasInicio.removeAllViews();

        if (areasSelecionadas == null || areasSelecionadas.isEmpty()) {
            return; // ChipGroup fica vazio
        }

        for (String area : areasSelecionadas) {
            Chip chip = new Chip(getContext());
            chip.setText(area);
            chip.setCheckable(false); // Apenas exibe, não é selecionável aqui
            chip.setEnabled(!isReadOnly);
            binding.chipGroupAreasInicio.addView(chip);
        }
    }

    // <-- MUDANÇA: Novo método para abrir o diálogo de seleção
    /**
     * Abre um diálogo de múltipla escolha para o usuário selecionar as áreas de atuação.
     */
    private void abrirDialogSelecaoAreas() {
        if (getContext() == null || sharedViewModel.ideia.getValue() == null) return;

        // 1. Pega todas as opções
        String[] allAreas = getResources().getStringArray(R.array.areas_atuacao_opcoes);

        // 2. Pega as áreas atualmente selecionadas
        List<String> currentAreasList = sharedViewModel.ideia.getValue().getAreasNecessarias();
        Set<String> currentAreasSet = (currentAreasList != null) ? new HashSet<>(currentAreasList) : new HashSet<>();

        // 3. Cria um array booleano para o estado 'checked' do diálogo
        boolean[] checkedItems = new boolean[allAreas.length];
        // 4. Cria uma lista temporária para rastrear as seleções dentro do diálogo
        ArrayList<String> dialogSelectedList = new ArrayList<>(currentAreasSet);

        for (int i = 0; i < allAreas.length; i++) {
            if (currentAreasSet.contains(allAreas[i])) {
                checkedItems[i] = true;
            }
        }

        // 5. Constrói e exibe o diálogo
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.areas_necessarias_titulo)
                .setMultiChoiceItems(allAreas, checkedItems, (dialog, which, isChecked) -> {
                    // Atualiza a lista temporária quando o usuário clica
                    String selected = allAreas[which];
                    if (isChecked) {
                        dialogSelectedList.add(selected);
                    } else {
                        dialogSelectedList.remove(selected);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // 6. No "OK", envia a nova seleção para o ViewModel
                    if (binding == null) return;
                    sharedViewModel.updateIdeiaBasics(
                            binding.editTextTituloIdeia.getText().toString().trim(),
                            binding.editTextDescricaoIdeia.getText().toString().trim(),
                            new ArrayList<>(dialogSelectedList) // Envia a nova lista de áreas
                    );
                })
                .show();
    }

    // <-- MUDANÇA: Método obsoleto removido
    // private List<String> coletarAreasSelecionadas() { ... }

    private void setReadOnlyMode(boolean isReadOnly) {
        binding.editTextTituloIdeia.setEnabled(!isReadOnly);
        binding.editTextDescricaoIdeia.setEnabled(!isReadOnly);
        // <-- MUDANÇA: Adiciona o controle do novo botão
        binding.btnSelecionarAreasInicio.setEnabled(!isReadOnly);
        // A lógica de habilitar/desabilitar os chips já está em 'exibirChipsSelecionados'
    }
}