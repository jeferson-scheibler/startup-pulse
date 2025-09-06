package com.example.startuppulse;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class IdeiasAdapter extends ListAdapter<Ideia, IdeiasAdapter.IdeiaViewHolder> {

    public interface OnIdeiaClickListener {
        void onIdeiaClick(Ideia ideia);
    }

    private final OnIdeiaClickListener listener;
    @Nullable private final Fragment fragment;

    // DiffUtil: identifica pelo ID e compara campos que impactam a UI
    private static final DiffUtil.ItemCallback<Ideia> DIFF = new DiffUtil.ItemCallback<Ideia>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ideia oldItem, @NonNull Ideia newItem) {
            String a = oldItem.getId(), b = newItem.getId();
            return a != null && a.equals(b);
        }
        @Override
        public boolean areContentsTheSame(@NonNull Ideia o, @NonNull Ideia n) {
            return safeEq(o.getNome(), n.getNome())
                    && safeEq(o.getAutorNome(), n.getAutorNome())
                    && safeEq(o.getOwnerId(), n.getOwnerId())
                    && safeEq(o.getMentorId(), n.getMentorId())
                    && safeEq(o.getStatus(), n.getStatus())
                    && safeEq(o.getAvaliacaoStatus(), n.getAvaliacaoStatus());
        }
        private boolean safeEq(String a, String b) { return a == null ? b == null : a.equals(b); }
    };

    // Construtores
    public IdeiasAdapter(@NonNull OnIdeiaClickListener listener) {
        super(DIFF);
        this.listener = listener;
        this.fragment = null;
    }

    public IdeiasAdapter(@NonNull OnIdeiaClickListener listener, @NonNull Fragment fragment) {
        super(DIFF);
        this.listener = listener;
        this.fragment = fragment;
    }

    // Compat com assinatura antiga (prefira submitList no Fragment)
    public IdeiasAdapter(@NonNull List<Ideia> ideias, @NonNull OnIdeiaClickListener listener) {
        this(listener);
        submitList(new ArrayList<>(ideias));
    }
    public IdeiasAdapter(@NonNull List<Ideia> ideias, @NonNull OnIdeiaClickListener listener, @NonNull Fragment fragment) {
        this(listener, fragment);
        submitList(new ArrayList<>(ideias));
    }

    @NonNull
    @Override
    public IdeiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ideia, parent, false);

        return new IdeiaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IdeiaViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    // ===== Helpers para o swipe/undo no Fragment =====
    public Ideia getItemAt(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return getItem(position);
    }

    public void removeAt(int position) {
        if (position < 0 || position >= getItemCount()) return;
        List<Ideia> nova = new ArrayList<>(getCurrentList());
        nova.remove(position);
        submitList(nova);
    }

    public void restore(Ideia ideia, int position) {
        if (ideia == null) return;
        List<Ideia> nova = new ArrayList<>(getCurrentList());
        if (position < 0 || position > nova.size()) position = nova.size();
        nova.add(position, ideia);
        submitList(nova);
    }

    // ===== ViewHolder =====
    static class IdeiaViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, autor;
        View highlightView;
        ImageView statusIcon;

        IdeiaViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.textViewTituloIdeia);
            autor = itemView.findViewById(R.id.textViewAutorIdeia);
            highlightView = itemView.findViewById(R.id.mentor_highlight_view);
            statusIcon = itemView.findViewById(R.id.icon_status_avaliacao);
        }

        void bind(final Ideia ideia, final OnIdeiaClickListener listener) {
            if (ideia == null) return;

            titulo.setText(ideia.getNome());
            autor.setText("Por: " + ideia.getAutorNome());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onIdeiaClick(ideia);
            });

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = user != null ? user.getUid() : null;

            // Destaque para mentor
            boolean souMentor = uid != null && uid.equals(ideia.getMentorId());
            highlightView.setVisibility(souMentor ? View.VISIBLE : View.GONE);

            // Ícone de status (apenas dono)
            boolean souDono = uid != null && uid.equals(ideia.getOwnerId());
            if (souDono && "PUBLICADA".equals(ideia.getStatus())) {
                statusIcon.setVisibility(View.VISIBLE);
                if ("Avaliada".equals(ideia.getAvaliacaoStatus())) {
                    statusIcon.setImageResource(R.drawable.ic_check);
                    statusIcon.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.green_success)
                    ));
                } else {
                    statusIcon.setImageResource(R.drawable.ic_hourglass);
                    statusIcon.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.primary_color)
                    ));
                }
            } else {
                statusIcon.setVisibility(View.GONE);
            }

            // A11y
            itemView.setContentDescription(
                    "Ideia " + (ideia.getNome() != null ? ideia.getNome() : "") +
                            " por " + (ideia.getAutorNome() != null ? ideia.getAutorNome() : "")
            );
            statusIcon.setContentDescription("Status da avaliação");
        }
    }

    // ===== Exclusão opcional via Adapter (se ainda quiser por aqui) =====
    // Obs.: com ListAdapter é melhor disparar a exclusão no Fragment (com undo).
    public void iniciarExclusao(final int position) {
        if (fragment == null || position < 0 || position >= getItemCount()) return;

        Ideia ideia = getItem(position);
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que quer excluir a ideia '" + ideia.getNome() + "'?")
                .setPositiveButton("Excluir", (dialog, which) -> excluirItem(position))
                .setNegativeButton("Cancelar", (dialog, which) -> notifyItemChanged(position))
                .setOnCancelListener(dialog -> notifyItemChanged(position))
                .show();
    }

    private void excluirItem(int position) {
        if (fragment == null || position < 0 || position >= getItemCount()) return;

        Ideia ideia = getItem(position);
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        firestoreHelper.excluirIdeia(ideia.getId(), r -> {
            if (fragment.getView() == null) return;

            if (r.isOk()) {
                List<Ideia> nova = new ArrayList<>(getCurrentList());
                if (position < nova.size()) {
                    nova.remove(position);
                    submitList(nova);
                }
                Snackbar.make(fragment.requireView(), "Ideia excluída!", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(fragment.requireView(),
                        "Erro ao excluir ideia: " + r.error.getMessage(),
                        Snackbar.LENGTH_LONG).show();
                notifyItemChanged(position); // reverte visual
            }
        });
    }
}