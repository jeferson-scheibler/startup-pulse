package com.example.startuppulse;

import android.content.Context;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.FragmentCanvasEquipeBinding;
import java.util.ArrayList;

public class CanvasEquipeFragment extends Fragment {

    // --- Interfaces e Constantes ---
    public interface EquipeListener {
        void onEquipeChanged(); // Notifica a Activity que a lista de membros mudou
    }

    // --- Propriedades ---
    private FragmentCanvasEquipeBinding binding;
    private EquipeAdapter adapter;
    private Ideia ideia;
    private boolean isReadOnly = false;
    private EquipeListener listener;

    public static CanvasEquipeFragment newInstance(Ideia ideia, boolean isReadOnly) {
        CanvasEquipeFragment fragment = new CanvasEquipeFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia);
        args.putBoolean("isReadOnly", isReadOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Garante que a Activity implementa o listener
        if (context instanceof EquipeListener) {
            listener = (EquipeListener) context;
        } else {
            // Se a Activity que hospeda este fragmento não implementar o listener, algo está errado.
            // Para evitar crashes, podemos apenas logar um aviso.
            // throw new RuntimeException(context + " deve implementar EquipeListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isReadOnly = getArguments().getBoolean("isReadOnly", false);
        }
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
        if (getActivity() instanceof CanvasIdeiaActivity) {
            this.ideia = ((CanvasIdeiaActivity) getActivity()).getIdeiaAtual();
        }
        if (this.ideia == null) {
            // Lógica de segurança caso algo corra mal
            return;
        }
        setupRecyclerView();
        binding.fabAddMembro.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        binding.fabAddMembro.setOnClickListener(v -> showMembroDialog(null));
    }

    private void setupRecyclerView() {
        if (ideia != null && ideia.getEquipe() == null) {
            ideia.setEquipe(new ArrayList<>());
        }
        binding.recyclerViewEquipe.setLayoutManager(new LinearLayoutManager(getContext()));
        // Passa um listener para o adapter para capturar cliques nos itens
        adapter = new EquipeAdapter(ideia.getEquipe(), membro -> {
            if (!isReadOnly) {
                showMembroDialog(membro);
            }
        });
        binding.recyclerViewEquipe.setAdapter(adapter);
        checkEmptyState();
    }

    /**
     * Mostra o diálogo para adicionar ou editar um membro da equipe.
     * @param membroExistente O membro a ser editado, ou null para adicionar um novo.
     */
    private void showMembroDialog(@Nullable MembroEquipe membroExistente) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_membro, null);

        final EditText nomeInput = dialogView.findViewById(R.id.edit_text_nome_membro);
        final EditText funcaoInput = dialogView.findViewById(R.id.edit_text_funcao_membro);
        final EditText linkedinInput = dialogView.findViewById(R.id.edit_text_linkedin_membro);

        String dialogTitle = (membroExistente == null) ? "Adicionar Membro" : "Editar Membro";

        // Se estiver a editar, preenche os campos com os dados existentes
        if (membroExistente != null) {
            nomeInput.setText(membroExistente.getNome());
            funcaoInput.setText(membroExistente.getFuncao());
            linkedinInput.setText(membroExistente.getLinkedinUrl());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(dialogTitle)
                .setView(dialogView)
                .setPositiveButton("Salvar", null) // Definimos o listener depois para controlar o fecho
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

                // --- VALIDAÇÃO DO LINKEDIN ---
                if (!linkedin.isEmpty() && !isValidLinkedInUrl(linkedin)) {
                    linkedinInput.setError("Insira um link válido do LinkedIn (ex: https://linkedin.com/in/perfil)");
                    return;
                }

                // Se a validação passar, continua com a lógica de salvar
                if (membroExistente == null) {
                    MembroEquipe novoMembro = new MembroEquipe();
                    novoMembro.setNome(nome);
                    novoMembro.setFuncao(funcao);
                    novoMembro.setLinkedinUrl(linkedin);
                    ideia.getEquipe().add(novoMembro);
                } else {
                    membroExistente.setNome(nome);
                    membroExistente.setFuncao(funcao);
                    membroExistente.setLinkedinUrl(linkedin);
                }

                adapter.notifyDataSetChanged();
                checkEmptyState();
                if (listener != null) {
                    listener.onEquipeChanged();
                }

                dialog.dismiss(); // Fecha o diálogo apenas se tudo estiver correto
            });
        });

        dialog.show();
    }

    /**
     * Valida se uma string é um URL válido do LinkedIn.
     * Aceita http, https, www e a ausência destes.
     * @param url A string a ser validada.
     * @return true se for um link válido, false caso contrário.
     */
    private boolean isValidLinkedInUrl(String url) {
        if (url == null) {
            return false;
        }
        // Padrão Regex para validar os formatos mais comuns de URL do LinkedIn
        String regex = "^((https|http):\\/\\/)?(www\\.)?linkedin\\.com\\/in\\/[a-zA-Z0-9_-]+(\\/)?$";
        return url.matches(regex);
    }
    private void checkEmptyState() {
        if (ideia.getEquipe() == null || ideia.getEquipe().isEmpty()) {
            binding.recyclerViewEquipe.setVisibility(View.GONE);
            binding.viewEmptyStateEquipe.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerViewEquipe.setVisibility(View.VISIBLE);
            binding.viewEmptyStateEquipe.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}