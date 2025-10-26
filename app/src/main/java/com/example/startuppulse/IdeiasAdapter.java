package com.example.startuppulse;

import android.annotation.SuppressLint;
// Imports removidos (não são mais necessários)
// import android.content.res.ColorStateList;
// import android.widget.ImageView;
// import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView; // <-- IMPORT ADICIONADO
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
        LottieAnimationView mentorHighlightView;
        LottieAnimationView statusAguardando;
        LottieAnimationView statusAprovada;
        LottieAnimationView statusReprovada;
        View iconContainer;
        IdeiaViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.textViewTituloIdeia);
            autor = itemView.findViewById(R.id.textViewAutorIdeia);

            iconContainer = itemView.findViewById(R.id.icon_container);

            // --- MUDANÇA: Encontrando os IDs corretos do Lottie ---
            mentorHighlightView = itemView.findViewById(R.id.mentor_highlight_view);
            statusAguardando = itemView.findViewById(R.id.icon_status_aguardando);
            statusAprovada = itemView.findViewById(R.id.icon_status_avaliacao_ok);
            statusReprovada = itemView.findViewById(R.id.icon_status_reprovada);

            // statusIcon = itemView.findViewById(R.id.icon_status_avaliacao); // Removido
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
            boolean souDono = uid != null && uid.equals(ideia.getOwnerId());

            // 1. Esconda todos os ícones individuais primeiro
            mentorHighlightView.setVisibility(View.GONE);
            statusAguardando.setVisibility(View.GONE);
            statusAprovada.setVisibility(View.GONE);
            statusReprovada.setVisibility(View.GONE);

            // 2. Assuma que o CONTAINER está escondido
            iconContainer.setVisibility(View.GONE);

            // 3. Decida qual ícone mostrar (e torne o CONTAINER visível)
            if (souMentor) {
                iconContainer.setVisibility(View.VISIBLE); // Mostra o container
                mentorHighlightView.setVisibility(View.VISIBLE);

            } else if (souDono && !"RASCUNHO".equals(ideia.getStatus())) {
                iconContainer.setVisibility(View.VISIBLE); // Mostra o container

                String status = String.valueOf(ideia.getStatus());
                if ("AVALIADA_APROVADA".equals(status)) {
                    statusAprovada.setVisibility(View.VISIBLE);
                    statusAprovada.setContentDescription("Ideia avaliada e aprovada");
                } else if ("AVALIADA_REPROVADA".equals(status)) {
                    statusReprovada.setVisibility(View.VISIBLE);
                    statusReprovada.setContentDescription("Ideia avaliada e reprovada");
                } else if ("EM_AVALIACAO".equals(status)){
                    statusAguardando.setVisibility(View.VISIBLE);
                    statusAguardando.setContentDescription("Ideia em avaliação");
                }else {
                    iconContainer.setVisibility(View.GONE);
                }
            }
            // Se não for nem mentor, nem dono vendo status, todos ícones ficam GONE (já feito acima).
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