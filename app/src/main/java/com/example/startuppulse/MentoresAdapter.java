package com.example.startuppulse;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.util.AvatarUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MentoresAdapter extends ListAdapter<Mentor, MentoresAdapter.MentorViewHolder> {

    public interface OnMentorClickListener {
        void onMentorClick(Mentor mentor);
    }

    private final OnMentorClickListener listener;

    // DiffUtil: identifica item pelo ID e verifica mudanças relevantes (inclui verificado e áreas)
    private static final DiffUtil.ItemCallback<Mentor> DIFF = new DiffUtil.ItemCallback<Mentor>() {
        @Override
        public boolean areItemsTheSame(@NonNull Mentor oldItem, @NonNull Mentor newItem) {
            return notNullEq(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Mentor oldItem, @NonNull Mentor newItem) {
            return notNullEq(oldItem.getNome(), newItem.getNome())
                    && oldItem.isVerificado() == newItem.isVerificado()
                    && notNullEq(oldItem.getImagem(), newItem.getImagem())
                    && listEquals(oldItem.getAreas(), newItem.getAreas());
        }

        private boolean notNullEq(String a, String b) {
            return Objects.equals(a, b);
        }

        private boolean listEquals(List<String> a, List<String> b) {
            if (a == null) a = new ArrayList<>();
            if (b == null) b = new ArrayList<>();
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++) {
                if (!Objects.equals(a.get(i), b.get(i))) return false;
            }
            return true;
        }
    };

    public MentoresAdapter(OnMentorClickListener listener) {
        super(DIFF);
        this.listener = listener;
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
        holder.bind(getItem(position), listener);
    }

    // ---------------- ViewHolder ----------------
    static class MentorViewHolder extends RecyclerView.ViewHolder {
        TextView nome;
        ImageView image, imageArrow, iconVerificado;
        com.google.android.material.chip.ChipGroup chipGroupAreas;

        MentorViewHolder(@NonNull View itemView) {
            super(itemView);
            nome           = itemView.findViewById(R.id.textViewNome);
            image          = itemView.findViewById(R.id.imageViewMentor);
            imageArrow     = itemView.findViewById(R.id.imageViewArrow);
            iconVerificado = itemView.findViewById(R.id.iconVerificado);
            chipGroupAreas = itemView.findViewById(R.id.chipGroupAreas);
        }

        void bind(final Mentor mentor, final OnMentorClickListener listener) {
            if (mentor == null) return;

            // Nome (safe) + a11y do item
            String safeName = mentor.getNome() == null ? "" : mentor.getNome();
            nome.setText(safeName);
            itemView.setContentDescription("Mentor " + safeName);

            // Avatar (com fallback gerado por letra)
            String fotoUrl = mentor.getImagem();
            Resources r = itemView.getResources();
            int px = (int) (56 * r.getDisplayMetrics().density); // tamanho base para fallback
            BitmapDrawable fallback = new BitmapDrawable(
                    r, AvatarUtils.circleLetter(safeName, px, 0xFF455A64, 0xFFFFFFFF)
            );

            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(fotoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(fallback)
                        .fallback(fallback)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .circleCrop()
                        .into(image);
            } else {
                image.setImageDrawable(fallback);
            }
            image.setContentDescription("Avatar do mentor " + safeName);

            // Chevron (tint já pode vir do XML; manter simples aqui)
            imageArrow.setImageResource(R.drawable.ic_chevron_right);
            imageArrow.setContentDescription(itemView.getContext()
                    .getString(R.string.ver_detalhes_mentor_content_desc));

            // Selo verificado
            iconVerificado.setVisibility(mentor.isVerificado() ? View.VISIBLE : View.GONE);

            chipGroupAreas.removeAllViews();
            List<String> areas = mentor.getAreas();
            if (areas != null && !areas.isEmpty()) {
                final int MAX = 1; // mostra UM chip + “+N”
                int shown = Math.min(MAX, areas.size());

                for (int i = 0; i < shown; i++) {
                    android.view.ContextThemeWrapper themed =
                            new android.view.ContextThemeWrapper(itemView.getContext(),
                                    R.style.Widget_StartupPulse_Chip_Area_Compact);
                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(themed);
                    chip.setText(areas.get(i));
                    chip.setCheckable(false);
                    chip.setClickable(false);
                    chip.setChipIconResource(R.drawable.ic_tag);
                    chip.setChipBackgroundColorResource(R.color.colorSurface);
                    chip.setChipStrokeColorResource(R.color.colorDivider);
                    chip.setChipStrokeWidth(
                            itemView.getResources().getDisplayMetrics().density * 1f // 1dp
                    );
                    chip.setTextColor(itemView.getResources().getColor(R.color.colorOnSurface));
                    chip.setChipIconTintResource(R.color.colorPrimary);
                    chip.setIconStartPadding(4f);
                    chip.setTextStartPadding(2f);
                    chipGroupAreas.addView(chip);
                }

                int remaining = areas.size() - shown;
                if (remaining > 0) {
                    android.view.ContextThemeWrapper themed =
                            new android.view.ContextThemeWrapper(itemView.getContext(),
                                    R.style.Widget_StartupPulse_Chip_Area_Compact);
                    com.google.android.material.chip.Chip more = new com.google.android.material.chip.Chip(themed);
                    more.setText("+" + remaining);
                    more.setCheckable(false);
                    more.setClickable(false);
                    more.setChipBackgroundColorResource(R.color.colorSurface);
                    more.setChipStrokeColorResource(R.color.colorDivider);
                    more.setChipStrokeWidth(
                            itemView.getResources().getDisplayMetrics().density * 1f
                    );
                    more.setTextColor(itemView.getResources().getColor(R.color.colorOnSurface));
                    chipGroupAreas.addView(more);
                }
            } else {
                chipGroupAreas.setVisibility(View.GONE);
            }

            // Clique abre detalhes
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMentorClick(mentor);
            });
        }
    }
}