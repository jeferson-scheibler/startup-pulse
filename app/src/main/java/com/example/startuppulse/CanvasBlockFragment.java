package com.example.startuppulse;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.startuppulse.databinding.FragmentCanvasBlockBinding;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class CanvasBlockFragment extends Fragment
        implements AddPostItDialogFragment.AddPostItListener, PostItAdapter.OnPostItClickListener {

    public interface CanvasBlockListener {
        void onDataChanged(); // Um nome mais genérico, já que notifica sobre qualquer mudança
    }

    private FragmentCanvasBlockBinding binding;
    private Ideia ideia;
    private CanvasEtapa etapaInfo;
    private String etapaChave;
    private boolean isReadOnly = false;

    private PostItAdapter postItAdapter;
    private CanvasBlockListener listener;
    private FirestoreHelper firestoreHelper;

    public static CanvasBlockFragment newInstance(CanvasEtapa etapa, String etapaChave, Ideia ideia, boolean isReadOnly) {
        CanvasBlockFragment fragment = new CanvasBlockFragment();
        Bundle args = new Bundle();
        args.putSerializable("etapa", etapa);
        args.putString("etapa_chave", etapaChave);
        args.putSerializable("ideia", ideia);
        args.putBoolean("isReadOnly", isReadOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof CanvasBlockListener) {
            listener = (CanvasBlockListener) context;
        } else {
            throw new RuntimeException(context + " deve implementar CanvasBlockListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreHelper = new FirestoreHelper();
        if (getArguments() != null) {
            ideia = (Ideia) getArguments().getSerializable("ideia");
            etapaInfo = (CanvasEtapa) getArguments().getSerializable("etapa");
            etapaChave = getArguments().getString("etapa_chave");
            isReadOnly = getArguments().getBoolean("isReadOnly");
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
        setupUI();
        setupRecycler();
        loadPostIts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupUI() {
        if (etapaInfo != null) {
            binding.textViewBlockTitle.setText(etapaInfo.getTitulo());
            binding.textViewBlockDescription.setText(etapaInfo.getDescricao());
            binding.imageViewBlockIcon.setImageResource(etapaInfo.getIconeResId());
        }
        binding.fabAddPostIt.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        binding.fabAddPostIt.setOnClickListener(v -> abrirDialogoParaAdicionar());
    }

    private void setupRecycler() {
        binding.recyclerViewPostIts.setLayoutManager(new LinearLayoutManager(requireContext()));
        postItAdapter = new PostItAdapter(new ArrayList<>(), isReadOnly ? null : this);
        binding.recyclerViewPostIts.setAdapter(postItAdapter);
    }

    private void loadPostIts() {
        if (ideia == null || etapaChave == null) return;
        List<PostIt> postIts = ideia.getPostItsPorChave(etapaChave);
        postItAdapter.updatePostIts(postIts);
        refreshEmptyState(postIts);
    }

    private void refreshEmptyState(List<PostIt> list) {
        boolean isEmpty = list == null || list.isEmpty();
        binding.viewEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewPostIts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    public void atualizarDadosIdeia(@NonNull Ideia ideiaAtualizada) {
        this.ideia = ideiaAtualizada;
        if (binding != null) {
            loadPostIts();
        }
    }

    private void abrirDialogoParaAdicionar() {
        if (ideia == null || ideia.getId() == null) {
            Snackbar.make(binding.getRoot(), "Primeiro, dê um nome e salve a sua ideia.", Snackbar.LENGTH_LONG).show();
            return;
        }
        // VOLTAMOS A USAR O SEU DIALOG PERSONALIZADO
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForAdd(ideia.getId(), etapaChave);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "AddPostItDialog");
    }

    @Override
    public void onPostItClick(PostIt postit) {
        if (isReadOnly) return;
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForEdit(ideia.getId(), etapaChave, postit);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "EditPostItDialog");
    }

    @Override
    public void onPostItLongClick(PostIt postit) {
        if (isReadOnly) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Apagar Ponto-Chave")
                .setMessage("Tem certeza que deseja apagar este post-it?")
                .setPositiveButton("Apagar", (d, w) -> apagarPostit(postit))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarPostit(PostIt postit) {
        // Chamada direta ao Firestore, como no seu código original
        firestoreHelper.deletePostitFromIdeia(ideia.getId(), etapaChave, postit, r -> {
            if (getView() == null) return;
            if (r.isOk()) {
                Snackbar.make(getView(), "Post-it excluído!", Snackbar.LENGTH_SHORT).show();
                // A UI será atualizada pelo listener da Activity
            } else {
                Snackbar.make(getView(), "Erro ao excluir: " + r.error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // Callback do AddPostItDialogFragment quando um post-it é ADICIONADO
    @Override
    public void onPostItAdded() {
        // A UI será atualizada pelo listener do Firestore na Activity
        // Não precisamos fazer nada aqui, o que está correto na sua arquitetura
    }

    // Callback do AddPostItDialogFragment quando um post-it é EDITADO
    @Override
    public void onPostItEdited(PostIt postitAntigo, String novoTexto, String novaCor) {
        firestoreHelper.updatePostitInIdeia(ideia.getId(), etapaChave, postitAntigo, novoTexto, novaCor, r -> {
            if (getView() == null) return;
            if (r.isOk()) {
                Snackbar.make(getView(), "Post-it atualizado!", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(getView(), "Erro ao atualizar: " + r.error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}