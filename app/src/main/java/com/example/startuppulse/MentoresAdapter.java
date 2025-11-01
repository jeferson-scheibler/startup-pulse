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
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.util.AvatarUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Objects;

/**
 * Adapter para a lista de mentores.
 *
 * Utiliza ListAdapter (com DiffUtil) para eficiência e animações automáticas.
 * Exibe informações compartilhadas do User + campos do Mentor (quando disponíveis).
 */
public class MentoresAdapter extends ListAdapter<User, MentoresAdapter.MentorViewHolder> {

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
        User user = getItem(position);
        if (user != null) {
            holder.bind(user);
        }
    }

    // ------------------------------
    // ViewHolder interno
    // ------------------------------
    static class MentorViewHolder extends RecyclerView.ViewHolder {

        TextView nome, cidade;
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

        void bind(final User user) {
            String safeName = user.getNome() != null ? user.getNome() : "";
            nome.setText(safeName);
            itemView.setContentDescription("Mentor " + safeName);

            // Avatar com fallback gerado a partir da inicial do nome
            String fotoUrl = user.getFotoUrl();
            Resources res = itemView.getResources();
            int px = (int) (56 * res.getDisplayMetrics().density); // 56dp

            BitmapDrawable fallback = new BitmapDrawable(
                    res,
                    AvatarUtils.circleLetter(safeName, px, 0xFF455A64, 0xFFFFFFFF)
            );

            Glide.with(itemView.getContext())
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(fallback)
                    .fallback(fallback)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .circleCrop()
                    .into(image);

            // Se tiver mentorData (dados específicos)
            Mentor mentorData = user.getMentorData();
            if (mentorData != null) {
                // Exibir selo verificado se aplicável
                iconVerificado.setVisibility(mentorData.isVerified() ? View.VISIBLE : View.GONE);

                // Exibir cidade/estado, se houver TextView no layout
                if (cidade != null) {
                    String local = "";
                    if (!mentorData.getCity().isEmpty()) {
                        local += mentorData.getCity();
                        if (!mentorData.getState().isEmpty())
                            local += " - " + mentorData.getState();
                    }
                    cidade.setText(local);
                    cidade.setVisibility(local.isEmpty() ? View.GONE : View.VISIBLE);
                }
            } else {
                // Caso mentorData não esteja carregado
                iconVerificado.setVisibility(View.GONE);
                if (cidade != null) cidade.setVisibility(View.GONE);
            }

            // Chips com áreas de atuação (do User)
            setupAreaChips(user.getAreasDeInteresse());

            // Clique -> Detalhes do mentor
            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("mentorId", user.getId());

                try {
                    Navigation.findNavController(v)
                            .navigate(R.id.action_mentoresFragment_to_mentorDetailFragment, bundle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        private void setupAreaChips(List<String> areas) {
            chipGroupAreas.removeAllViews();

            if (areas != null && !areas.isEmpty()) {
                chipGroupAreas.setVisibility(View.VISIBLE);

                final int MAX_CHIPS = 1; // Exibe apenas uma + contador
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
            android.view.ContextThemeWrapper themedContext =
                    new android.view.ContextThemeWrapper(
                            itemView.getContext(),
                            R.style.ThemeOverlay_StartupPulse_Chip_Area_Compact
                    );

            Chip chip = new Chip(themedContext);
            chip.setText(text);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setFocusable(false);
            return chip;
        }
    }

    // ------------------------------
    // DiffUtil Callback
    // ------------------------------
    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<User>() {
                @Override
                public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                    return Objects.equals(oldItem.getNome(), newItem.getNome())
                            && Objects.equals(oldItem.getFotoUrl(), newItem.getFotoUrl())
                            && Objects.equals(oldItem.getAreasDeInteresse(), newItem.getAreasDeInteresse())
                            && Objects.equals(
                            oldItem.getMentorData() != null && oldItem.getMentorData().isVerified(),
                            newItem.getMentorData() != null && newItem.getMentorData().isVerified()
                    );
                }
            };
}
