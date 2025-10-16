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
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.MembroEquipe;
import com.example.startuppulse.databinding.FragmentCanvasEquipeBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasEquipeFragment extends Fragment {

    private FragmentCanvasEquipeBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;
    private EquipeAdapter adapter;
    private boolean isReadOnly = false; // Estado de UI local

    public CanvasEquipeFragment() {
        // Construtor público vazio é obrigatório.
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
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);

        setupRecyclerView();
        setupObservers();

        binding.fabAddMembro.setOnClickListener(v -> showMembroDialog(null));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupRecyclerView() {
        binding.recyclerViewEquipe.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EquipeAdapter(membro -> {
            // A verificação de 'readOnly' acontece aqui, antes de abrir o diálogo
            if (!this.isReadOnly) {
                showMembroDialog(membro);
            }
        });
        binding.recyclerViewEquipe.setAdapter(adapter);
    }

    private void setupObservers() {
        // Observador principal que controla a UI
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null && binding != null) {
                // 1. Determina o estado a partir do status da ideia
                this.isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;

                // 2. Atualiza o adapter e a visibilidade do FAB
                adapter.submitList(ideia.getEquipe()); // Assumindo que EquipeAdapter usa ListAdapter
                binding.fabAddMembro.setVisibility(this.isReadOnly ? View.GONE : View.VISIBLE);

                checkEmptyState();
            }
        });
    }

    private void showMembroDialog(@Nullable MembroEquipe membroExistente) {
        // Esta lógica permanece a mesma, pois já está bem estruturada.
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_membro, null);

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

                // Delega a ação para o ViewModel
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
        // Regex simples para validação de URL do LinkedIn
        String regex = "^((https|http):\\/\\/)?(www\\.)?linkedin\\.com\\/in\\/[a-zA-Z0-9_-]+(\\/)?$";
        return url.matches(regex);
    }
}