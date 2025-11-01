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
import com.example.startuppulse.data.models.CanvasEtapa;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.models.PostIt;
import com.example.startuppulse.databinding.FragmentCanvasBlockBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.example.startuppulse.util.Event;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
// --- CORREÇÃO: Removida a implementação da interface antiga ---
public class CanvasBlockFragment extends Fragment {

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

        // --- CORREÇÃO: Adapter criado uma única vez aqui ---
        postItAdapter = new PostItAdapter(sharedViewModel, isReadOnly);

        setupRecyclerView(); // Apenas configura o LayoutManager
        setupObservers();
        setupClickListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupRecyclerView() {
        binding.recyclerViewPostIts.setLayoutManager(new LinearLayoutManager(requireContext()));
        // --- CORREÇÃO: Adapter já foi criado, apenas o definimos aqui ---
        binding.recyclerViewPostIts.setAdapter(postItAdapter);
    }

    private void setupObservers() {
        // Observador principal da ideia
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null && etapaChave != null) {
                this.isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;
                postItAdapter.setReadOnly(this.isReadOnly); // Atualiza o adapter
                List<PostIt> postItsDaEtapa = ideia.getPostItsPorChave(etapaChave); // Pega a lista correta
                postItAdapter.submitList(postItsDaEtapa);
                binding.fabAddPostIt.setVisibility(this.isReadOnly ? View.GONE : View.VISIBLE);
                refreshEmptyState(postItsDaEtapa);
            }
        });

        // Observador das etapas (para título/descrição)
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

        // Observador para EDITAR post-it (correto)
        sharedViewModel.editPostItEvent.observe(getViewLifecycleOwner(), new Event.EventObserver<>(postIt -> {
            showEditPostItDialog(postIt);
        }));

        // --- NOVO OBSERVADOR: Para DELETAR post-it ---
        sharedViewModel.deletePostItEvent.observe(getViewLifecycleOwner(), new Event.EventObserver<>(postIt -> {
            // Só mostra o diálogo se não estiver em modo somente leitura (segurança extra)
            if (!isReadOnly) {
                showDeleteConfirmationDialog(postIt);
            }
        }));
    }

    private void setupClickListeners() {
        binding.fabAddPostIt.setOnClickListener(v -> abrirDialogoParaAdicionar());
    }

    private void abrirDialogoParaAdicionar() {
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForAdd(etapaChave);
        dialog.show(getParentFragmentManager(), "AddPostItDialog");
    }

    // --- Lógica de Edição (Movida para cá) ---
    private void showEditPostItDialog(PostIt postIt) {
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForEdit(etapaChave, postIt);
        dialog.show(getParentFragmentManager(), "AddPostItDialog");
    }

    // --- Lógica de Exclusão (Movida para cá) ---
    private void showDeleteConfirmationDialog(PostIt postit) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Apagar Ponto-Chave")
                .setMessage("Tem certeza que deseja apagar este post-it?")
                .setPositiveButton("Apagar", (d, w) -> {
                    sharedViewModel.deletePostIt(etapaChave, postit); // Chama o ViewModel para deletar
                    Snackbar.make(binding.getRoot(), "Post-it excluído!", Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    // --- Método auxiliar ---
    private void refreshEmptyState(List<PostIt> list) {
        boolean isEmpty = list == null || list.isEmpty();
        binding.viewEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewPostIts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}