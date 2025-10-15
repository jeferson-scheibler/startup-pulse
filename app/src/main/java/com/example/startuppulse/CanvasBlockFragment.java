package com.example.startuppulse;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.data.CanvasEtapa;
// Removido: import com.example.startuppulse.data.Ideia; // Não é mais necessário diretamente
import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.databinding.FragmentCanvasBlockBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.google.android.material.snackbar.Snackbar;

// Removido: import java.util.ArrayList; // Não é mais necessário para o construtor do adapter

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasBlockFragment extends Fragment implements PostItAdapter.OnPostItClickListener {

    private FragmentCanvasBlockBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;
    private PostItAdapter postItAdapter;
    private String etapaChave;
    // Removido: private boolean isReadOnly = false; // O estado é gerenciado pelo adapter

    public static CanvasBlockFragment newInstance(String etapaChave) {
        CanvasBlockFragment fragment = new CanvasBlockFragment();
        Bundle args = new Bundle();
        args.putString("etapa_chave", etapaChave);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            etapaChave = getArguments().getString("etapa_chave");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasBlockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);
        setupRecycler();
        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null && etapaChave != null) {
                // MODIFICAÇÃO 2: Usar submitList() para performance máxima com DiffUtil
                postItAdapter.submitList(ideia.getPostItsPorChave(etapaChave));
                refreshEmptyState(ideia.getPostItsPorChave(etapaChave));
            }
        });

        sharedViewModel.isReadOnly.observe(getViewLifecycleOwner(), readOnly -> {
            if (readOnly != null) {
                // Informa o adapter sobre o modo read-only
                postItAdapter.setReadOnly(readOnly);
                binding.fabAddPostIt.setVisibility(readOnly ? View.GONE : View.VISIBLE);
            }
        });

        sharedViewModel.etapas.observe(getViewLifecycleOwner(), etapas -> {
            if (etapas == null) return;
            for (CanvasEtapa etapa : etapas) {
                if (etapa.getChave().equals(etapaChave)) {
                    binding.textViewBlockTitle.setText(etapa.getTitulo());
                    binding.textViewBlockDescription.setText(etapa.getDescricao());
                    binding.imageViewBlockIcon.setImageResource(etapa.getIconeResId());
                    break;
                }
            }
        });
    }

    private void setupRecycler() {
        binding.recyclerViewPostIts.setLayoutManager(new LinearLayoutManager(requireContext()));
        // MODIFICAÇÃO 1: Usar o novo construtor do ListAdapter
        postItAdapter = new PostItAdapter(this);
        binding.recyclerViewPostIts.setAdapter(postItAdapter);
    }

    private void setupClickListeners() {
        binding.fabAddPostIt.setOnClickListener(v -> abrirDialogoParaAdicionar());
    }

    private void abrirDialogoParaAdicionar() {
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForAdd(etapaChave);
        dialog.show(getParentFragmentManager(), "AddPostItDialog");
    }

    // Callbacks do Adapter (OnPostItClickListener)

    @Override
    public void onPostItClick(PostIt postit) {
        // A verificação de isReadOnly agora é feita dentro do adapter,
        // mas é uma boa prática manter aqui também por segurança.
        if (Boolean.TRUE.equals(sharedViewModel.isReadOnly.getValue())) return;
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForEdit(etapaChave, postit);
        dialog.show(getParentFragmentManager(), "EditPostItDialog");
    }

    @Override
    public void onPostItLongClick(PostIt postit) {
        if (Boolean.TRUE.equals(sharedViewModel.isReadOnly.getValue())) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Apagar Ponto-Chave")
                .setMessage("Tem certeza que deseja apagar este post-it?")
                .setPositiveButton("Apagar", (d, w) -> {
                    sharedViewModel.deletePostIt(etapaChave, postit);
                    Snackbar.make(binding.getRoot(), "Post-it excluído!", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void refreshEmptyState(java.util.List<PostIt> list) {
        boolean isEmpty = list == null || list.isEmpty();
        binding.viewEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewPostIts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}