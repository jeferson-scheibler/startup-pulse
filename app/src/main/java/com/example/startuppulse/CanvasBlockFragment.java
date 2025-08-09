package com.example.startuppulse;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragmento FINAL e CORRIGIDO que exibe um bloco do canvas.
 * Utiliza uma chave de texto estável para todas as operações de dados.
 */
public class CanvasBlockFragment extends Fragment
        implements AddPostItDialogFragment.AddPostItListener, PostItAdapter.OnPostItClickListener {

    private Ideia ideia;
    private CanvasEtapa etapaInfo; // Contém a chave da etapa (ex: "PROPOSTA_VALOR")
    private boolean isReadOnly = false;

    // Componentes da UI
    private PostItAdapter postItAdapter;
    private RecyclerView recyclerViewPostIts;
    private LinearLayout viewEmptyState;
    private CanvasBlockListener listener;

    public interface CanvasBlockListener {
        void onPostItAdded();
    }

    public static CanvasBlockFragment newInstance(Ideia ideia, CanvasEtapa etapaInfo, boolean isReadOnly) {
        CanvasBlockFragment fragment = new CanvasBlockFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia);
        args.putSerializable("etapaInfo", etapaInfo);
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
            throw new RuntimeException(context.toString() + " must implement CanvasBlockListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ideia = (Ideia) getArguments().getSerializable("ideia");
            etapaInfo = (CanvasEtapa) getArguments().getSerializable("etapaInfo");
            isReadOnly = getArguments().getBoolean("isReadOnly");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_canvas_block, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tituloTextView = view.findViewById(R.id.text_view_block_title);
        TextView descricaoTextView = view.findViewById(R.id.text_view_block_description);
        ImageView iconImageView = view.findViewById(R.id.image_view_block_icon);
        recyclerViewPostIts = view.findViewById(R.id.recycler_view_post_its);
        viewEmptyState = view.findViewById(R.id.view_empty_state);
        FloatingActionButton fabAddPostIt = view.findViewById(R.id.fab_add_post_it);

        tituloTextView.setText(etapaInfo.getTitulo());
        descricaoTextView.setText(etapaInfo.getDescricao());
        iconImageView.setImageResource(etapaInfo.getIconeResId());

        setupRecyclerView();

        fabAddPostIt.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        fabAddPostIt.setOnClickListener(v -> abrirDialogoParaAdicionar());

        loadPostIts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPostIts();
    }

    private void setupRecyclerView() {
        recyclerViewPostIts.setLayoutManager(new LinearLayoutManager(getContext()));
        postItAdapter = new PostItAdapter(new ArrayList<>(), isReadOnly ? null : this);
        recyclerViewPostIts.setAdapter(postItAdapter);
    }

    private void abrirDialogoParaAdicionar() {
        if (ideia != null && ideia.getId() != null) {
            AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForAdd(ideia.getId(), etapaInfo.getKey());
            dialog.setTargetFragment(this, 0);
            dialog.show(getParentFragmentManager(), "AddPostItDialog");
        }
    }

    private void loadPostIts() {
        if (ideia != null && postItAdapter != null && getContext() != null) {
            List<PostIt> postIts = ideia.getPostItsPorChave(etapaInfo.getKey());

            Log.d("CanvasBlockFragment", "Etapa " + etapaInfo.getKey() + " - Carregando " + postIts.size() + " post-its.");

            postItAdapter.updatePostIts(postIts);

            boolean hasPostIts = !postIts.isEmpty();
            viewEmptyState.setVisibility(hasPostIts ? View.GONE : View.VISIBLE);
            recyclerViewPostIts.setVisibility(hasPostIts ? View.VISIBLE : View.GONE);
        }
    }

    public void atualizarDadosIdeia(Ideia ideiaAtualizada) {
        this.ideia = ideiaAtualizada;
        if (isAdded()) {
            loadPostIts();
        }
    }

    @Override
    public void onPostItClick(PostIt postit) {
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForEdit(
                ideia.getId(),
                etapaInfo.getKey(),
                postit
        );
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "EditPostItDialog");
    }

    @Override
    public void onPostItLongClick(PostIt postit) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Apagar Ponto-Chave")
                .setMessage("Tem a certeza que quer apagar este post-it?")
                .setPositiveButton("Apagar", (dialog, which) -> apagarPostit(postit))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarPostit(PostIt postit) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.deletePostitFromIdeia(ideia.getId(), etapaInfo.getKey(), postit, new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess() {
                // Não precisa de Toast, a UI atualiza-se sozinha.
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Erro ao apagar o post-it.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPostItAdded() {
        if (listener != null) {
            listener.onPostItAdded();
        }
    }

    @Override
    public void onPostItEdited(PostIt postitAntigo, String novoTexto, String novaCor) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.updatePostitInIdeia(ideia.getId(), etapaInfo.getKey(), postitAntigo, novoTexto, novaCor, new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess() {
                // UI atualiza-se via listener da Activity.
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Erro ao atualizar o post-it.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}