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
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.databinding.FragmentCanvasBlockBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasBlockFragment extends Fragment implements PostItAdapter.OnPostItClickListener {

    private FragmentCanvasBlockBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;
    private PostItAdapter postItAdapter;
    private String etapaChave;
    private boolean isReadOnly = false; // Mantém o estado localmente

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupObservers() {
        // O observer principal que controla tudo neste fragmento
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null && etapaChave != null) {
                // 1. Determina o estado de apenas leitura
                this.isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;

                // 2. Atualiza o adapter com a lista de Post-its e o estado read-only
                postItAdapter.setReadOnly(this.isReadOnly);
                postItAdapter.submitList(ideia.getPostItsPorChave(etapaChave));

                // 3. Atualiza a visibilidade dos componentes da UI
                binding.fabAddPostIt.setVisibility(this.isReadOnly ? View.GONE : View.VISIBLE);
                refreshEmptyState(ideia.getPostItsPorChave(etapaChave));
            }
        });

        // Este observer permanece o mesmo, para preencher o título e descrição do bloco
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

    // --- Callbacks do Adapter ---

    @Override
    public void onPostItClick(PostIt postit) {
        // A verificação de isReadOnly é feita aqui para evitar abrir o diálogo desnecessariamente
        if (this.isReadOnly) return;
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForEdit(etapaChave, postit);
        dialog.show(getParentFragmentManager(), "EditPostItDialog");
    }

    @Override
    public void onPostItLongClick(PostIt postit) {
        if (this.isReadOnly) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Apagar Ponto-Chave")
                .setMessage("Tem certeza que deseja apagar este post-it?")
                .setPositiveButton("Apagar", (d, w) -> {
                    sharedViewModel.deletePostIt(etapaChave, postit);
                    // O feedback visual agora pode vir do ViewModel se desejado,
                    // mas um Snackbar local ainda é uma boa opção.
                    Snackbar.make(binding.getRoot(), "Post-it excluído!", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void refreshEmptyState(List<PostIt> list) {
        boolean isEmpty = list == null || list.isEmpty();
        binding.viewEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewPostIts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}