package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.startuppulse.data.MembroEquipe;
import com.example.startuppulse.databinding.ItemMembroEquipeEditBinding;
import java.util.List;
import java.util.function.Consumer;

public class EquipeEditAdapter extends RecyclerView.Adapter<EquipeEditAdapter.MembroViewHolder> {

    private final List<MembroEquipe> membros;
    private final Consumer<MembroEquipe> onRemoveListener;

    public EquipeEditAdapter(List<MembroEquipe> membros, Consumer<MembroEquipe> onRemoveListener) {
        this.membros = membros;
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
        MembroEquipe membro = membros.get(position);
        holder.bind(membro, onRemoveListener);
    }

    @Override
    public int getItemCount() {
        return membros != null ? membros.size() : 0;
    }

    static class MembroViewHolder extends RecyclerView.ViewHolder {
        private final ItemMembroEquipeEditBinding binding;

        public MembroViewHolder(ItemMembroEquipeEditBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final MembroEquipe membro, final Consumer<MembroEquipe> onRemoveListener) {
            binding.textMembroNome.setText(membro.getNome());
            binding.textMembroFuncao.setText(membro.getFuncao());

            Glide.with(itemView.getContext())
                    .load(membro.getFotoUrl())
                    .placeholder(R.drawable.ic_person) // Imagem padrão
                    .circleCrop()
                    .into(binding.imageMembroAvatar);

            binding.btnRemoverMembro.setOnClickListener(v -> onRemoveListener.accept(membro));
        }
    }
}