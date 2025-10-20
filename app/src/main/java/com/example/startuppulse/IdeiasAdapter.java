package com.example.startuppulse;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.data.models.Ideia;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class IdeiasAdapter extends ListAdapter<Ideia, IdeiasAdapter.IdeiaViewHolder> {

    public interface OnIdeiaClickListener {
        void onIdeiaClick(Ideia ideia);
    }

    private final OnIdeiaClickListener clickListener;
    public IdeiasAdapter(@NonNull OnIdeiaClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public IdeiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ideia, parent, false);
        return new IdeiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IdeiaViewHolder holder, int position) {
        Ideia ideia = getItem(position);
        holder.bind(ideia, clickListener);
    }

    public Ideia getIdeiaAt(int position) {
        return getItem(position);
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

        @SuppressLint("SetTextI18n")
        void bind(final Ideia ideia, final OnIdeiaClickListener listener) {
            if (ideia == null) return;

            titulo.setText(ideia.getNome());
            autor.setText("Por: " + ideia.getAutorNome());

            itemView.setOnClickListener(v -> listener.onIdeiaClick(ideia));

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (user != null) ? user.getUid() : null;

            boolean souMentor = uid != null && uid.equals(ideia.getMentorId());
            highlightView.setVisibility(souMentor ? View.VISIBLE : View.GONE);

            boolean souDono = uid != null && uid.equals(ideia.getOwnerId());

            // Lógica para mostrar o status (ícone de avaliação)
            if (souDono && !"RASCUNHO".equals(ideia.getStatus())) {
                statusIcon.setVisibility(View.VISIBLE);
                if ("AVALIADA_APROVADA".equals(ideia.getStatus()) || "AVALIADA_REPROVADA".equals(ideia.getStatus())) {
                    statusIcon.setImageResource(R.drawable.ic_check);
                    statusIcon.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.green_success)
                    ));
                    statusIcon.setContentDescription("Ideia avaliada");
                } else { // EM_AVALIACAO
                    statusIcon.setImageResource(R.drawable.ic_hourglass);
                    statusIcon.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary)
                    ));
                    statusIcon.setContentDescription("Ideia em avaliação");
                }
            } else {
                statusIcon.setVisibility(View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<Ideia> DIFF_CALLBACK = new DiffUtil.ItemCallback<Ideia>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ideia oldItem, @NonNull Ideia newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ideia oldItem, @NonNull Ideia newItem) {
            // Compare todos os campos que afetam a UI
            return Objects.equals(oldItem.getNome(), newItem.getNome())
                    && Objects.equals(oldItem.getAutorNome(), newItem.getAutorNome())
                    && Objects.equals(oldItem.getMentorId(), newItem.getMentorId())
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && Objects.equals(oldItem.getOwnerId(), newItem.getOwnerId());
        }
    };
}