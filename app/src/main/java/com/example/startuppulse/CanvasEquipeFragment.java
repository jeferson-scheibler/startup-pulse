package com.example.startuppulse;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

// Imports para a nova arquitetura
import com.example.startuppulse.databinding.FragmentCanvasEquipeBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasEquipeFragment extends Fragment {

    // A interface EquipeListener foi removida. O ViewModel lida com a comunicação.

    private FragmentCanvasEquipeBinding binding;
    private CanvasIdeiaViewModel sharedViewModel; // O ViewModel compartilhado
    private EquipeAdapter adapter;

    /**
     * O método newInstance foi removido, pois o CanvasPagerAdapter agora
     * pode simplesmente chamar 'new CanvasEquipeFragment()'.
     */
    public CanvasEquipeFragment() {
        // Construtor público vazio é obrigatório.
    }

    // O método onAttach foi removido, pois não usamos mais o listener.

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // A lógica de getArguments foi removida.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasEquipeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ponto-chave: Obtém a instância do ViewModel COMPARTILHADO do fragment pai.
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);

        setupRecyclerView();
        setupObservers(); // A UI agora é configurada pelos observers.

        binding.fabAddMembro.setOnClickListener(v -> showMembroDialog(null));
    }

    private void setupRecyclerView() {
        binding.recyclerViewEquipe.setLayoutManager(new LinearLayoutManager(getContext()));
        // CORREÇÃO DO ERRO LAMBDA: O listener é passado corretamente para o novo construtor do adapter.
        adapter = new EquipeAdapter(membro -> {
            if (!Boolean.TRUE.equals(sharedViewModel.isReadOnly.getValue())) {
                showMembroDialog(membro);
            }
        });
        binding.recyclerViewEquipe.setAdapter(adapter);
    }

    private void setupObservers() {
        // Observa o objeto Ideia. Quando ele muda, a lista da equipe é atualizada no adapter.
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null) {
                // Usa o método 'updateEquipe' que chama 'submitList' para máxima performance.
                adapter.updateEquipe(ideia.getEquipe());
                checkEmptyState();
            }
        });

        // Observa o estado de 'somente leitura' para controlar a UI.
        sharedViewModel.isReadOnly.observe(getViewLifecycleOwner(), isReadOnly -> {
            if (isReadOnly != null) {
                binding.fabAddMembro.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void showMembroDialog(@Nullable MembroEquipe membroExistente) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_membro, null);

        // CORREÇÃO DOS ERROS DE SÍMBOLO: As views são declaradas e usadas dentro deste escopo.
        final EditText nomeInput = dialogView.findViewById(R.id.edit_text_nome_membro);
        final EditText funcaoInput = dialogView.findViewById(R.id.edit_text_funcao_membro);
        final EditText linkedinInput = dialogView.findViewById(R.id.edit_text_linkedin_membro);

        String dialogTitle = (membroExistente == null) ? "Adicionar Membro" : "Editar Membro";

        if (membroExistente != null) {
            nomeInput.setText(membroExistente.getNome());
            funcaoInput.setText(membroExistente.getFuncao());
            linkedinInput.setText(membroExistente.getLinkedinUrl());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(dialogTitle)
                .setView(dialogView)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String nome = nomeInput.getText().toString().trim();
                String funcao = funcaoInput.getText().toString().trim();
                String linkedin = linkedinInput.getText().toString().trim();

                if (TextUtils.isEmpty(nome)) {
                    nomeInput.setError("O nome é obrigatório");
                    return;
                }
                if (TextUtils.isEmpty(funcao)) {
                    funcaoInput.setError("A função é obrigatória");
                    return;
                }
                if (!linkedin.isEmpty() && !isValidLinkedInUrl(linkedin)) {
                    linkedinInput.setError("Insira um link válido do LinkedIn");
                    return;
                }

                // Delega a lógica de negócio para o ViewModel.
                if (membroExistente == null) {
                    sharedViewModel.addMembroEquipe(new MembroEquipe(nome, funcao, linkedin));
                } else {
                    sharedViewModel.updateMembroEquipe(membroExistente, nome, funcao, linkedin);
                }

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            binding.recyclerViewEquipe.setVisibility(View.GONE);
            binding.viewEmptyStateEquipe.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerViewEquipe.setVisibility(View.VISIBLE);
            binding.viewEmptyStateEquipe.setVisibility(View.GONE);
        }
    }

    private boolean isValidLinkedInUrl(String url) {
        if (url == null) return false;
        String regex = "^((https|http):\\/\\/)?(www\\.)?linkedin\\.com\\/in\\/[a-zA-Z0-9_-]+(\\/)?$";
        return url.matches(regex);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}