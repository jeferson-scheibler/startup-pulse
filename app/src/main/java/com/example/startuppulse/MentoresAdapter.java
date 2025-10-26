// app/src/main/java/com/example/startuppulse/MentoresAdapter.java

package com.example.startuppulse;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.util.AvatarUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Objects;

/**
 * Adapter para a lista de mentores, utilizando ListAdapter para performance e DiffUtil para
 * animações eficientes de atualização de lista.
 * Atualizado para ser compatível com o modelo de dados Mentor refatorado.
 */
public class MentoresAdapter extends ListAdapter<Mentor, MentoresAdapter.MentorViewHolder> {

    public MentoresAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public MentorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mentor, parent, false);
        return new MentorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MentorViewHolder holder, int position) {
        Mentor mentor = getItem(position);
        if (mentor != null) {
            holder.bind(mentor);
        }
    }

    // --- ViewHolder ---
    static class MentorViewHolder extends RecyclerView.ViewHolder {
        TextView nome;
        ImageView image, imageArrow, iconVerificado;
        ChipGroup chipGroupAreas;

        MentorViewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textViewNome);
            image = itemView.findViewById(R.id.imageViewMentor);
            imageArrow = itemView.findViewById(R.id.imageViewArrow);
            iconVerificado = itemView.findViewById(R.id.iconVerificado);
            chipGroupAreas = itemView.findViewById(R.id.chipGroupAreas);
        }

        void bind(final Mentor mentor) {
            // Nome (com tratamento para nulo)
            String safeName = mentor.getName() != null ? mentor.getName() : "";
            nome.setText(safeName);
            itemView.setContentDescription("Mentor " + safeName);

            // Avatar (com fallback gerado a partir da primeira letra do nome)
            String fotoUrl = mentor.getFotoUrl();
            Resources res = itemView.getResources();
            int px = (int) (56 * res.getDisplayMetrics().density); // 56dp
            BitmapDrawable fallback = new BitmapDrawable(
                    res, AvatarUtils.circleLetter(safeName, px, 0xFF455A64, 0xFFFFFFFF)
            );

            Glide.with(itemView.getContext())
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(fallback) // Mostra o fallback em caso de erro no load
                    .fallback(fallback) // Mostra o fallback se a URL for nula
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .circleCrop()
                    .into(image);

            // Selo de verificado
            iconVerificado.setVisibility(mentor.isVerified() ? View.VISIBLE : View.GONE);

            // Chips com áreas de atuação
            setupAreaChips(mentor.getAreas());

            // Listener de clique para navegar para os detalhes
            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("mentorId", mentor.getId()); // Correção: Usar o método correto

                try {
                    Navigation.findNavController(v).navigate(R.id.action_mentoresFragment_to_mentorDetailFragment, bundle);
                } catch (Exception e) {
                    // Logar o erro pode ser útil para depuração em cenários complexos
                    e.printStackTrace();
                }
            });
        }

        private void setupAreaChips(List<String> areas) {
            chipGroupAreas.removeAllViews();
            if (areas != null && !areas.isEmpty()) {
                chipGroupAreas.setVisibility(View.VISIBLE);
                final int MAX_CHIPS = 1;
                int chipsToShow = Math.min(MAX_CHIPS, areas.size());

                for (int i = 0; i < chipsToShow; i++) {
                    chipGroupAreas.addView(createChip(areas.get(i)));
                }

                int remaining = areas.size() - chipsToShow;
                if (remaining > 0) {
                    chipGroupAreas.addView(createChip("+" + remaining));
                }
            } else {
                chipGroupAreas.setVisibility(View.GONE);
            }
        }

        private Chip createChip(String text) {
            android.view.ContextThemeWrapper themedContext = new android.view.ContextThemeWrapper(itemView.getContext(),
                    R.style.ThemeOverlay_StartupPulse_Chip_Area_Compact);
            Chip chip = new Chip(themedContext);
            chip.setText(text);
            chip.setClickable(false); // Apenas visual
            return chip;
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<Mentor> DIFF_CALLBACK = new DiffUtil.ItemCallback<Mentor>() {
        @Override
        public boolean areItemsTheSame(@NonNull Mentor oldItem, @NonNull Mentor newItem) {
            // Itens são os mesmos se os IDs forem iguais
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Mentor oldItem, @NonNull Mentor newItem) {
            // Conteúdo é o mesmo se esses campos não mudaram
            return Objects.equals(oldItem.getName(), newItem.getName())
                    && oldItem.isVerified() == newItem.isVerified()
                    && Objects.equals(oldItem.getFotoUrl(), newItem.getFotoUrl())
                    && Objects.equals(oldItem.getAreas(), newItem.getAreas());
        }
    };
}