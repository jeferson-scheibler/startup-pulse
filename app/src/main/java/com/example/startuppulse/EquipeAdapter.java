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

import java.util.Objects;

/**
 * Adapter otimizado para a lista de membros da equipe (somente leitura).
 * Utiliza ListAdapter e DiffUtil para atualizações de UI performáticas e automáticas.
 */
public class EquipeAdapter extends ListAdapter<MembroEquipe, EquipeAdapter.MembroViewHolder> {

    public interface OnMembroClickListener {
        void onMembroClick(MembroEquipe membro);
    }

    private final OnMembroClickListener listener;

    /**
     * O construtor recebe o listener de clique. A lista de dados é gerenciada
     * internamente pelo ListAdapter.
     */
    public EquipeAdapter(OnMembroClickListener listener) {
        super(DIFF_CALLBACK);
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
        MembroEquipe membro = getItem(position);
        holder.bind(membro);
    }

    class MembroViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView photo;
        private final TextView name;
        private final TextView role;

        public MembroViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.image_view_membro_photo);
            name = itemView.findViewById(R.id.text_view_membro_nome);
            role = itemView.findViewById(R.id.text_view_membro_funcao);
        }

        // --- MELHORIA: O listener é acessado da classe externa, simplificando a assinatura do método ---
        public void bind(MembroEquipe membro) {
            name.setText(membro.getNome());
            role.setText(membro.getFuncao());

            Glide.with(itemView.getContext())
                    .load(membro.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person) // Boa prática: definir uma imagem de erro também
                    .circleCrop() // Usar circleCrop() se a ShapeableImageView não for um círculo por padrão
                    .into(photo);

            itemView.setOnClickListener(v -> {
                // Adiciona uma verificação de segurança para a posição do adapter
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onMembroClick(getItem(getAdapterPosition()));
                }
            });
        }
    }

    /**
     * Implementação do DiffUtil para permitir que o ListAdapter anime apenas os itens que mudaram,
     * melhorando a performance.
     */
    private static final DiffUtil.ItemCallback<MembroEquipe> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MembroEquipe>() {
                @Override
                public boolean areItemsTheSame(@NonNull MembroEquipe oldItem, @NonNull MembroEquipe newItem) {
                    // --- CORREÇÃO: Usa um campo que existe na classe MembroEquipe, como 'nome'. ---
                    // O ideal é usar um ID único se disponível.
                    return Objects.equals(oldItem.getNome(), newItem.getNome());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MembroEquipe oldItem, @NonNull MembroEquipe newItem) {
                    // Requer que a classe MembroEquipe tenha um método .equals() bem implementado.
                    return oldItem.equals(newItem);
                }
            };
}