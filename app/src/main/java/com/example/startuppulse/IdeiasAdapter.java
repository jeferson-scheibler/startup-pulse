package com.example.startuppulse;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class IdeiasAdapter extends RecyclerView.Adapter<IdeiasAdapter.IdeiaViewHolder> {

    public interface OnIdeiaClickListener {
        void onIdeiaClick(Ideia ideia);
    }

    private List<Ideia> ideias;
    private final OnIdeiaClickListener listener;
    @Nullable
    private final Fragment fragment;
    public IdeiasAdapter(List<Ideia> ideias, OnIdeiaClickListener listener) {
        this.ideias = ideias;
        this.listener = listener;
        this.fragment = null;
    }

    public IdeiasAdapter(List<Ideia> ideias, OnIdeiaClickListener listener, @NonNull Fragment fragment) {
        this.ideias = ideias;
        this.listener = listener;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public IdeiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ideia, parent, false);
        return new IdeiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IdeiaViewHolder holder, int position) {
        Ideia ideia = ideias.get(position);
        holder.bind(ideia, listener);
    }

    @Override
    public int getItemCount() {
        return ideias != null ? ideias.size() : 0;
    }

    public void setIdeias(List<Ideia> novasIdeias) {
        this.ideias = novasIdeias;
        notifyDataSetChanged();
    }

    // ViewHolder
    static class IdeiaViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, autor;
        View highlightView;
        ImageView statusIcon;

        public IdeiaViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.textViewTituloIdeia);
            autor = itemView.findViewById(R.id.textViewAutorIdeia);
            highlightView = itemView.findViewById(R.id.mentor_highlight_view);
            statusIcon = itemView.findViewById(R.id.icon_status_avaliacao);
            //iconePremium = itemView.findViewById(R.id.imageViewPremium);
        }

        public void bind(final Ideia ideia, final OnIdeiaClickListener listener) {
            titulo.setText(ideia.getNome());
            autor.setText("Por: " + ideia.getAutorNome()); // Mostra o nome do autor

            itemView.setOnClickListener(v -> {
                if(listener != null) {
                    listener.onIdeiaClick(ideia);
                }
            });

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            // Lógica do Destaque de Mentor
            boolean souMentor = user.getUid().equals(ideia.getMentorId());
            highlightView.setVisibility(souMentor ? View.VISIBLE : View.GONE);

            // Lógica do Ícone de Status (só para ideias do próprio utilizador)
            boolean souDono = user.getUid().equals(ideia.getOwnerId());
            if (souDono && "PUBLICADA".equals(ideia.getStatus())) {
                statusIcon.setVisibility(View.VISIBLE);
                if ("Avaliada".equals(ideia.getAvaliacaoStatus())) {
                    statusIcon.setImageResource(R.drawable.ic_check);
                    statusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.green_success)));
                } else {
                    statusIcon.setImageResource(R.drawable.ic_hourglass);
                    statusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary_color)));
                }
            } else {
                statusIcon.setVisibility(View.GONE);
            }
        }
    }

    // Lógica para exclusão
    public void iniciarExclusao(final int position) {
        if (position >= ideias.size() || position < 0) return;

        Ideia ideiaParaExcluir = ideias.get(position);
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que quer excluir a ideia '" + ideiaParaExcluir.getNome() + "'?") // CORRIGIDO
                .setPositiveButton("Excluir", (dialog, which) -> excluirItem(position))
                .setNegativeButton("Cancelar", (dialog, which) -> notifyItemChanged(position))
                .setOnCancelListener(dialog -> notifyItemChanged(position))
                .show();
    }

    private void excluirItem(int position) {
        if (position >= ideias.size() || position < 0) return;

        Ideia ideia = ideias.get(position);
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        firestoreHelper.excluirIdeia(ideia.getId(), new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess() {
                ideias.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(fragment.getContext(), "Ideia excluída!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(fragment.getContext(), "Erro ao excluir ideia.", Toast.LENGTH_SHORT).show();
                notifyItemChanged(position);
            }
        });
    }
}