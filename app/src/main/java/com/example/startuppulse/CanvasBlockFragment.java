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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.common.Result;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento que exibe um bloco do canvas (ex.: PROPOSTA_VALOR, SEGMENTO_CLIENTES...).
 * Compatível com o FirestoreHelper (Callback<Result<T>>) e sem alterar IDs dos XMLs.
 */
public class CanvasBlockFragment extends Fragment
        implements AddPostItDialogFragment.AddPostItListener, PostItAdapter.OnPostItClickListener {

    public interface CanvasBlockListener {
        void onPostItAdded();
    }

    private static final String ARG_IDEIA = "ideia";
    private static final String ARG_ETAPA = "etapaInfo";
    private static final String ARG_READ_ONLY = "isReadOnly";

    private Ideia ideia;
    private CanvasEtapa etapaInfo; // contém a key estável (ex: "PROPOSTA_VALOR")
    private boolean isReadOnly = false;

    // UI
    private RecyclerView recyclerViewPostIts;
    private LinearLayout viewEmptyState;
    private PostItAdapter postItAdapter;
    private CanvasBlockListener listener;

    // ===== Factory =====
    public static CanvasBlockFragment newInstance(Ideia ideia, CanvasEtapa etapaInfo, boolean isReadOnly) {
        CanvasBlockFragment fragment = new CanvasBlockFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDEIA, ideia);
        args.putSerializable(ARG_ETAPA, etapaInfo);
        args.putBoolean(ARG_READ_ONLY, isReadOnly);
        fragment.setArguments(args);
        return fragment;
    }

    // ===== Lifecycle =====
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof CanvasBlockListener) {
            listener = (CanvasBlockListener) context;
        } else {
            throw new RuntimeException(context + " must implement CanvasBlockListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if (b != null) {
            ideia = (Ideia) b.getSerializable(ARG_IDEIA);
            etapaInfo = (CanvasEtapa) b.getSerializable(ARG_ETAPA);
            isReadOnly = b.getBoolean(ARG_READ_ONLY, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_canvas_block, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextView titulo = v.findViewById(R.id.text_view_block_title);
        TextView descricao = v.findViewById(R.id.text_view_block_description);
        ImageView icon = v.findViewById(R.id.image_view_block_icon);
        recyclerViewPostIts = v.findViewById(R.id.recycler_view_post_its);
        viewEmptyState = v.findViewById(R.id.view_empty_state);
        FloatingActionButton fabAdd = v.findViewById(R.id.fab_add_post_it);

        if (etapaInfo != null) {
            titulo.setText(etapaInfo.getTitulo());
            descricao.setText(etapaInfo.getDescricao());
            icon.setImageResource(etapaInfo.getIconeResId());
        }

        setupRecycler();

        // FAB visível apenas fora do read-only
        fabAdd.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        fabAdd.setOnClickListener(v1 -> abrirDialogoParaAdicionar());

        loadPostIts(); // carrega do objeto ideia recebido
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPostIts();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // ===== UI/Adapter =====
    private void setupRecycler() {
        recyclerViewPostIts.setLayoutManager(new LinearLayoutManager(requireContext()));
        // seu PostItAdapter já existente, que usa item_postit (ids: text_view_postit_content, text_view_last_modified)
        postItAdapter = new PostItAdapter(new ArrayList<>(), isReadOnly ? null : this);
        recyclerViewPostIts.setAdapter(postItAdapter);
    }

    private void refreshEmptyState(List<PostIt> list) {
        boolean vazio = list == null || list.isEmpty();
        if (!isAdded()) return;
        viewEmptyState.setVisibility(vazio ? View.VISIBLE : View.GONE);
        recyclerViewPostIts.setVisibility(vazio ? View.GONE : View.VISIBLE);
    }

    private void loadPostIts() {
        if (!isAdded() || ideia == null || etapaInfo == null || postItAdapter == null) return;
        List<PostIt> postIts = ideia.getPostItsPorChave(etapaInfo.getKey());
        if (postIts == null) postIts = new ArrayList<>();

        Log.d("CanvasBlockFragment",
                "Etapa " + etapaInfo.getKey() + " - carregando " + postIts.size() + " post-its.");

        postItAdapter.updatePostIts(postIts);
        refreshEmptyState(postIts);
    }

    /** Chamado pela Activity quando a ideia foi atualizada via listener do Firestore. */
    public void atualizarDadosIdeia(@NonNull Ideia ideiaAtualizada) {
        this.ideia = ideiaAtualizada;
        loadPostIts();
    }

    private void abrirDialogoParaAdicionar() {
        if (!isAdded()) return;
        if (isReadOnly) {
            Snackbar.make(requireView(), "Visualização somente leitura.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (ideia == null || ideia.getId() == null || etapaInfo == null) {
            Snackbar.make(requireView(), "Salve a etapa anterior para criar post-its.", Snackbar.LENGTH_LONG).show();
            return;
        }
        AddPostItDialogFragment dialog =
                AddPostItDialogFragment.newInstanceForAdd(ideia.getId(), etapaInfo.getKey());
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "AddPostItDialog");
    }

    // ===== Callbacks do adapter (clique/long-click) =====
    @Override
    public void onPostItClick(PostIt postit) {
        if (!isAdded()) return;
        if (isReadOnly) {
            Snackbar.make(requireView(), "Visualização somente leitura.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        AddPostItDialogFragment dialog = AddPostItDialogFragment.newInstanceForEdit(
                ideia.getId(), etapaInfo.getKey(), postit
        );
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "EditPostItDialog");
    }

    @Override
    public void onPostItLongClick(PostIt postit) {
        if (!isAdded() || isReadOnly) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Apagar Ponto-Chave")
                .setMessage("Tem certeza que deseja apagar este post-it?")
                .setPositiveButton("Apagar", (d, w) -> apagarPostit(postit))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ===== Firestore (com Result<T>) =====
    private void apagarPostit(PostIt postit) {
        if (!isAdded() || ideia == null || etapaInfo == null) return;

        new FirestoreHelper().deletePostitFromIdeia(
                ideia.getId(),
                etapaInfo.getKey(),
                postit,
                (Result<Void> r) -> {
                    if (!isAdded() || getView() == null) return;
                    if (r.isOk()) {
                        // UI será atualizada pelo listener da Activity (CanvasIdeiaActivity)
                        Snackbar.make(requireView(), "Post-it excluído!", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(requireView(),
                                "Erro ao excluir: " + r.error.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }

    // ===== Callbacks do AddPostItDialogFragment =====
    @Override
    public void onPostItAdded() {
        if (listener != null) listener.onPostItAdded();
        // A lista se atualiza via listener de ideia na Activity, então nada a fazer aqui.
    }

    @Override
    public void onPostItEdited(PostIt postitAntigo, String novoTexto, String novaCor) {
        if (!isAdded() || ideia == null || etapaInfo == null) return;

        new FirestoreHelper().updatePostitInIdeia(
                ideia.getId(),
                etapaInfo.getKey(),
                postitAntigo,
                novoTexto,
                novaCor,
                (Result<Void> r) -> {
                    if (!isAdded() || getView() == null) return;
                    if (r.isOk()) {
                        Snackbar.make(requireView(), "Post-it atualizado!", Snackbar.LENGTH_SHORT).show();
                        if (listener != null) listener.onPostItAdded();
                    } else {
                        Snackbar.make(requireView(),
                                "Erro ao atualizar: " + r.error.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }
}