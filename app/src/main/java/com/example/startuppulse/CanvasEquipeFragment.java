package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.databinding.FragmentCanvasEquipeBinding; // Importação CORRETA
import java.util.ArrayList;

public class CanvasEquipeFragment extends Fragment {

    private FragmentCanvasEquipeBinding binding; // Variável de binding CORRETA
    private EquipeAdapter adapter;
    private Ideia ideia;
    private boolean isReadOnly = false;

    public static CanvasEquipeFragment newInstance(Ideia ideia, boolean isReadOnly) {
        CanvasEquipeFragment fragment = new CanvasEquipeFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia);
        args.putBoolean("isReadOnly", isReadOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ideia = (Ideia) getArguments().getSerializable("ideia");
            isReadOnly = getArguments().getBoolean("isReadOnly", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout CORRETO usando o ViewBinding correto
        binding = FragmentCanvasEquipeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();

        // Esconde o botão de adicionar se estiver em modo de leitura
        binding.fabAddMembro.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);

        binding.fabAddMembro.setOnClickListener(v -> {
            // TODO: Abrir um Dialog para adicionar um novo membro da equipe.
            // Esta lógica será o nosso próximo passo.
            new AlertDialog.Builder(requireContext())
                    .setTitle("Funcionalidade em Construção")
                    .setMessage("A janela para adicionar membros da equipe será implementada na próxima etapa.")
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void setupRecyclerView() {
        if (ideia != null && ideia.getEquipe() == null) {
            ideia.setEquipe(new ArrayList<>());
        }
        binding.recyclerViewEquipe.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EquipeAdapter(ideia.getEquipe());
        binding.recyclerViewEquipe.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Limpa a referência ao binding para evitar memory leaks
    }
}