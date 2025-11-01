package com.example.startuppulse.ui.match;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class MatchCandidatesDialog extends Dialog {

    public interface OnMentorSelectedListener {
        void onMentorSelected(User mentor);
    }

    public MatchCandidatesDialog(@NonNull Context context, List<User> mentores, OnMentorSelectedListener listener) {
        super(context);
        setContentView(R.layout.dialog_match_candidates);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        RecyclerView recyclerView = findViewById(R.id.rvMentores);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new MentorAdapter(mentores, listener));
    }

    static class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.MentorViewHolder> {

        private final List<User> mentores;
        private final OnMentorSelectedListener listener;

        MentorAdapter(List<User> mentores, OnMentorSelectedListener listener) {
            this.mentores = mentores;
            this.listener = listener;
        }

        @NonNull
        @Override
        public MentorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mentor, parent, false);
            return new MentorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MentorViewHolder holder, int position) {
            User user = mentores.get(position);
            Mentor mentorData = user.getMentorData();

            holder.textViewNome.setText(user.getNome() != null ? user.getNome() : "Mentor sem nome");

            // Foto de perfil
            if (user.getFotoUrl() != null && !user.getFotoUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(user.getFotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .into(holder.imageViewMentor);
            } else {
                holder.imageViewMentor.setImageResource(R.drawable.ic_person);
            }

            // Selo verificado
            if (mentorData != null && mentorData.isVerified()) {
                holder.iconVerificado.setVisibility(View.VISIBLE);
            } else {
                holder.iconVerificado.setVisibility(View.GONE);
            }

            // Áreas (origem: User)
            holder.chipGroupAreas.removeAllViews();
            List<String> areas = user.getAreasDeInteresse();
            if (areas != null && !areas.isEmpty()) {
                for (String area : areas) {
                    Chip chip = new Chip(holder.itemView.getContext());
                    chip.setText(area);
                    chip.setTextColor(holder.itemView.getResources().getColor(android.R.color.white));
                    chip.setChipBackgroundColorResource(R.color.colorPrimary);
                    chip.setClickable(false);
                    holder.chipGroupAreas.addView(chip);
                }
            }

            // Clique
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Mentor selecionado: " + user.getNome(), Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onMentorSelected(user);
            });
        }

        @Override
        public int getItemCount() {
            return mentores != null ? mentores.size() : 0;
        }

        static class MentorViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewMentor, iconVerificado;
            TextView textViewNome;
            ChipGroup chipGroupAreas;

            MentorViewHolder(@NonNull View itemView) {
                super(itemView);
                imageViewMentor = itemView.findViewById(R.id.imageViewMentor);
                textViewNome = itemView.findViewById(R.id.textViewNome);
                chipGroupAreas = itemView.findViewById(R.id.chipGroupAreas);
                iconVerificado = itemView.findViewById(R.id.iconVerificado);
            }
        }
    }
}
