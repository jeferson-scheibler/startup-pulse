package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MentoresAdapter extends RecyclerView.Adapter<MentoresAdapter.MentorViewHolder> {

    public interface OnMentorClickListener {
        void onMentorClick(Mentor mentor);
    }
    private List<Mentor> mentores;
    private final OnMentorClickListener listener;
    private final Fragment fragment; // Para o contexto do diálogo de exclusão

    public MentoresAdapter(List<Mentor> mentores, OnMentorClickListener listener) {
        this.mentores = mentores;
        this.listener = listener;
        this.fragment = null;
    }

    public MentoresAdapter(List<Mentor> mentores, OnMentorClickListener listener, @NonNull Fragment fragment) {
        this.mentores = mentores;
        this.listener = listener;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public MentorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mentor, parent, false);
        return new MentorViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MentorViewHolder holder, int position) {
        Mentor mentor = mentores.get(position);
        holder.bind(mentor, listener);
    }

    @Override
    public int getItemCount() {
        return mentores != null ? mentores.size() : 0;
    }

    public void setMentores(List<Mentor> novosMentores) {
        this.mentores = novosMentores;
        notifyDataSetChanged();
    }

    // ViewHolder
    static class MentorViewHolder extends RecyclerView.ViewHolder {
        TextView nome, cidade, profissao;
        ImageView image, imageArrow;

        public MentorViewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textViewNome);
            profissao = itemView.findViewById(R.id.chipProfissao);
            cidade = itemView.findViewById(R.id.textViewCidade);
            image = itemView.findViewById(R.id.imageViewMentor);
            imageArrow = itemView.findViewById(R.id.imageViewArrow);
        }

        public void bind(final Mentor mentor, final OnMentorClickListener listener) {
            nome.setText(mentor.getNome());
            cidade.setText(String.format("%s, %s", mentor.getCidade(), mentor.getEstado()));
            profissao.setText(mentor.getProfissao());

            String fotoUrl = mentor.getImagem();
            if (fotoUrl != null && !fotoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(fotoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(image);
            } else {
                image.setImageResource(R.drawable.ic_person);
            }

            imageArrow.setImageResource(R.drawable.ic_chevron_right);
            imageArrow.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary_color));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMentorClick(mentor);
                }
            });
        }
    }

}
