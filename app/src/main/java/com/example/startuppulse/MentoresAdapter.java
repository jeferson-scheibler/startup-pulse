package com.example.startuppulse;

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

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.util.AvatarUtils;
import android.graphics.drawable.BitmapDrawable;
import android.content.res.Resources;

import com.bumptech.glide.Glide;

public class MentoresAdapter extends ListAdapter<Mentor, MentoresAdapter.MentorViewHolder> {

    public interface OnMentorClickListener {
        void onMentorClick(Mentor mentor);
    }

    private final OnMentorClickListener listener;

    // DiffUtil: identifica item pelo ID e verifica mudanças relevantes
    private static final DiffUtil.ItemCallback<Mentor> DIFF = new DiffUtil.ItemCallback<Mentor>() {
        @Override
        public boolean areItemsTheSame(@NonNull Mentor oldItem, @NonNull Mentor newItem) {
            // ajuste se o campo de ID tiver outro nome
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Mentor oldItem, @NonNull Mentor newItem) {
            // compare campos que mudam na UI
            return safeEq(oldItem.getNome(), newItem.getNome())
                    && safeEq(oldItem.getCidade(), newItem.getCidade())
                    && safeEq(oldItem.getEstado(), newItem.getEstado())
                    && safeEq(oldItem.getProfissao(), newItem.getProfissao())
                    && safeEq(oldItem.getImagem(), newItem.getImagem());
        }

        private boolean safeEq(String a, String b) {
            if (a == null) return b == null;
            return a.equals(b);
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

    // ViewHolder
    static class MentorViewHolder extends RecyclerView.ViewHolder {
        TextView nome, cidade, profissao;
        com.google.android.material.chip.Chip chipProfissao;
        ImageView image, imageArrow, iconVerificado;
        com.google.android.material.chip.ChipGroup chipGroupAreas;

        MentorViewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textViewNome);
            chipProfissao = itemView.findViewById(R.id.chipProfissao);
            cidade = itemView.findViewById(R.id.textViewCidade);
            image = itemView.findViewById(R.id.imageViewMentor);
            imageArrow = itemView.findViewById(R.id.imageViewArrow);
            iconVerificado = itemView.findViewById(R.id.iconVerificado);
            chipGroupAreas = itemView.findViewById(R.id.chipGroupAreas);
        }

        void bind(final Mentor mentor, final OnMentorClickListener listener) {
            if (mentor == null) return;

            nome.setText(mentor.getNome());
            cidade.setText(String.format("%s, %s", mentor.getCidade(), mentor.getEstado()));
            chipProfissao.setText(mentor.getProfissao());

            // A11y
            image.setContentDescription("Avatar do mentor " + (mentor.getNome() != null ? mentor.getNome() : ""));
            imageArrow.setContentDescription("Ver detalhes do mentor");
            itemView.setContentDescription(
                    String.format("Mentor %s, %s, %s", mentor.getNome(), mentor.getProfissao(), cidade.getText())
            );

            String fotoUrl = mentor.getImagem();
            Resources r = itemView.getResources();
            BitmapDrawable fallback = new BitmapDrawable(r,
                    AvatarUtils.circleLetter(mentor.getNome(), (int)(56 * r.getDisplayMetrics().density),
                            0xFF455A64, 0xFFFFFFFF)); // cinza azulado com letra branca

            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(fotoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(fallback)
                        .fallback(fallback)
                        .transition(DrawableTransitionOptions.withCrossFade()) // crossfade
                        .circleCrop()
                        .into(image);
            } else {
                image.setImageDrawable(fallback);
            }

            imageArrow.setImageResource(R.drawable.ic_chevron_right);
            imageArrow.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary_color));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMentorClick(mentor);
            });

            // Badge verificado
            boolean v = mentor.isVerificado();
            iconVerificado.setVisibility(v ? View.VISIBLE : View.GONE);

            // Chips de áreas
            chipGroupAreas.removeAllViews();
            java.util.List<String> areas = mentor.getAreas();
            if (areas != null && !areas.isEmpty()) {
                for (String area : areas) {
                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(itemView.getContext(), null, com.google.android.material.R.attr.chipStyle);
                    chip.setText(area);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    chip.setChipIconResource(R.drawable.ic_tag);
                    chip.setChipIconTintResource(androidx.appcompat.R.color.material_grey_600);
                    chipGroupAreas.addView(chip);
                }
                chipGroupAreas.setVisibility(View.VISIBLE);
            } else {
                chipGroupAreas.setVisibility(View.GONE);
            }
        }
    }
}