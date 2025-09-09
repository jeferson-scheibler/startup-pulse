package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class EquipeAdapter extends RecyclerView.Adapter<EquipeAdapter.MembroViewHolder> {

    public interface OnMembroClickListener {
        void onMembroClick(MembroEquipe membro);
    }
    private final List<MembroEquipe> membros;
    private final OnMembroClickListener listener;

    public EquipeAdapter(List<MembroEquipe> membros, OnMembroClickListener listener) {
        this.membros = membros;
        this.listener = listener;
    }



    @NonNull
    @Override
    public MembroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_membro_equipe, parent, false);
        return new MembroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MembroViewHolder holder, int position) {
        holder.bind(membros.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return membros != null ? membros.size() : 0;
    }

    static class MembroViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView photo;
        private final TextView name;
        private final TextView role;

        public MembroViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.image_view_membro_photo);
            name = itemView.findViewById(R.id.text_view_membro_nome);
            role = itemView.findViewById(R.id.text_view_membro_funcao);
        }

        public void bind(MembroEquipe membro, final OnMembroClickListener clickListener) {
            name.setText(membro.getNome());
            role.setText(membro.getFuncao());

            Glide.with(itemView.getContext())
                    .load(membro.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(photo);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMembroClick(membro);
                }
            });
        }
    }
}