package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.startuppulse.data.MembroEquipe;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter otimizado para a lista de membros da equipe.
 * Utiliza ListAdapter e DiffUtil para atualizações de UI performáticas e automáticas.
 */
public class EquipeAdapter extends ListAdapter<MembroEquipe, EquipeAdapter.MembroViewHolder> {

    public interface OnMembroClickListener {
        void onMembroClick(MembroEquipe membro);
    }

    private final OnMembroClickListener listener;

    /**
     * O construtor agora só precisa do listener. A lista é gerenciada pelo ListAdapter.
     */
    public EquipeAdapter(OnMembroClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * Novo método para submeter a lista.
     * O ListAdapter cuidará de calcular as diferenças e animar as mudanças.
     */
    public void updateEquipe(List<MembroEquipe> membros) {
        // Garante que uma lista nula seja tratada como uma lista vazia.
        submitList(membros != null ? new ArrayList<>(membros) : new ArrayList<>());
    }

    @NonNull
    @Override
    public MembroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_membro_equipe, parent, false);
        return new MembroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MembroViewHolder holder, int position) {
        // Obtém o item da posição atual de forma segura.
        MembroEquipe membro = getItem(position);
        holder.bind(membro, listener);
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

            // O listener é configurado para o item inteiro.
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMembroClick(membro);
                }
            });
        }
    }

    /**
     * Implementação do DiffUtil para calcular as diferenças entre a lista antiga e a nova.
     * Isso permite que o ListAdapter anime apenas os itens que mudaram.
     */
    private static final DiffUtil.ItemCallback<MembroEquipe> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MembroEquipe>() {
                @Override
                public boolean areItemsTheSame(@NonNull MembroEquipe oldItem, @NonNull MembroEquipe newItem) {
                    // Itens são os mesmos se seus IDs forem iguais.
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MembroEquipe oldItem, @NonNull MembroEquipe newItem) {
                    // O conteúdo é o mesmo se os objetos forem iguais.
                    // Isso requer que a classe MembroEquipe tenha um método .equals() bem implementado.
                    return oldItem.equals(newItem);
                }
            };
}