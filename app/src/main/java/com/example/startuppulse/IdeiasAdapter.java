package com.example.startuppulse;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Objects;

public class IdeiasAdapter extends ListAdapter<Ideia, IdeiasAdapter.IdeiaViewHolder> {

    public interface OnIdeiaClickListener {
        void onIdeiaClick(Ideia ideia);
    }

    private final OnIdeiaClickListener listener;
    @Nullable private final Fragment fragment;

    private static final DiffUtil.ItemCallback<Ideia> DIFF = new DiffUtil.ItemCallback<Ideia>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ideia oldItem, @NonNull Ideia newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ideia o, @NonNull Ideia n) {
            return Objects.equals(o.getNome(), n.getNome())
                    && Objects.equals(o.getAutorNome(), n.getAutorNome())
                    && Objects.equals(o.getMentorId(), n.getMentorId())
                    && o.getStatus().equals(n.getStatus())
                    && Objects.equals(o.getAvaliacaoStatus(), n.getAvaliacaoStatus());
        }
    };

    public IdeiasAdapter(@NonNull OnIdeiaClickListener listener, @Nullable Fragment fragment) {
        super(DIFF);
        this.listener = listener;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public IdeiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ideia, parent, false);
        return new IdeiaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IdeiaViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

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
            String uid = (user != null) ? user.getUid() : null;

            boolean souMentor = uid != null && uid.equals(ideia.getMentorId());
            highlightView.setVisibility(souMentor ? View.VISIBLE : View.GONE);

            // --- CORREÇÃO: A lógica agora usa o Enum de Status ---
            boolean souDono = uid != null && uid.equals(ideia.getOwnerId());
            // Mostra o ícone se a ideia não for mais um rascunho
            if (souDono && ideia.getStatus() != Ideia.Status.RASCUNHO) {
                statusIcon.setVisibility(View.VISIBLE);
                if ("Avaliada".equals(ideia.getAvaliacaoStatus())) {
                    statusIcon.setImageResource(R.drawable.ic_check);
                    statusIcon.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.green_success)
                    ));
                    statusIcon.setContentDescription("Ideia avaliada");
                } else {
                    statusIcon.setImageResource(R.drawable.ic_hourglass);
                    statusIcon.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.primary_color)
                    ));
                    statusIcon.setContentDescription("Ideia em avaliação");
                }
            } else {
                statusIcon.setVisibility(View.GONE);
            }
        }
    }

    // --- MÉTODOS DE AJUDA PARA FRAGMENTOS ---
    public void iniciarExclusao(final int position) {
        if (fragment == null || position < 0 || position >= getItemCount()) return;
        Ideia ideia = getItem(position);
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que quer excluir a ideia '" + ideia.getNome() + "'?")
                .setPositiveButton("Excluir", (dialog, which) -> excluirItem(ideia, position))
                .setNegativeButton("Cancelar", (dialog, which) -> notifyItemChanged(position))
                .setOnCancelListener(dialog -> notifyItemChanged(position))
                .show();
    }

    private void excluirItem(Ideia ideia, int position) {
        if (fragment == null) return;
        new FirestoreHelper().excluirIdeia(ideia.getId(), r -> {
            if (fragment.getView() == null) return;
            if (r.isOk()) {
                Snackbar.make(fragment.requireView(), "Ideia excluída!", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(fragment.requireView(), "Erro ao excluir: " + r.error.getMessage(), Snackbar.LENGTH_LONG).show();
                // A lista será atualizada pelo listener do Firestore, revertendo a mudança visual se falhar.
            }
        });
    }
}