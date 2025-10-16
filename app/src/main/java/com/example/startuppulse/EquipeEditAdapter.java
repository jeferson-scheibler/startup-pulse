package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.startuppulse.data.MembroEquipe;
import com.example.startuppulse.databinding.ItemMembroEquipeEditBinding;
import java.util.Objects;
import java.util.function.Consumer;

public class EquipeEditAdapter extends ListAdapter<MembroEquipe, EquipeEditAdapter.MembroViewHolder> {

    private final Consumer<MembroEquipe> onRemoveListener;

    public EquipeEditAdapter(Consumer<MembroEquipe> onRemoveListener) {
        // Passa o DIFF_CALLBACK para o construtor da superclasse
        super(DIFF_CALLBACK);
        this.onRemoveListener = onRemoveListener;
    }

    @NonNull
    @Override
    public MembroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMembroEquipeEditBinding binding = ItemMembroEquipeEditBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MembroViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MembroViewHolder holder, int position) {
        // Usa getItem(position) que é fornecido pelo ListAdapter
        MembroEquipe membro = getItem(position);
        holder.bind(membro);
    }

    // A classe ViewHolder permanece quase a mesma
    class MembroViewHolder extends RecyclerView.ViewHolder {
        private final ItemMembroEquipeEditBinding binding;

        public MembroViewHolder(ItemMembroEquipeEditBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        // O listener agora é acessado a partir da classe externa
        public void bind(final MembroEquipe membro) {
            binding.textMembroNome.setText(membro.getNome());
            binding.textMembroFuncao.setText(membro.getFuncao());

            Glide.with(itemView.getContext())
                    .load(membro.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.imageMembroAvatar);

            binding.btnRemoverMembro.setOnClickListener(v -> {
                // Adiciona uma verificação de segurança para a posição do adapter
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onRemoveListener.accept(getItem(getAdapterPosition()));
                }
            });
        }
    }

    // --- NOVO: DiffUtil.ItemCallback para calcular as diferenças da lista ---
    private static final DiffUtil.ItemCallback<MembroEquipe> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MembroEquipe>() {
                @Override
                public boolean areItemsTheSame(@NonNull MembroEquipe oldItem, @NonNull MembroEquipe newItem) {
                    // Idealmente, cada membro teria um ID único
                    return oldItem.getNome().equals(newItem.getNome());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MembroEquipe oldItem, @NonNull MembroEquipe newItem) {
                    // Verifica se todos os campos do objeto são iguais.
                    // Isso requer que a classe MembroEquipe tenha um método equals() implementado.
                    return oldItem.equals(newItem);
                }
            };
}