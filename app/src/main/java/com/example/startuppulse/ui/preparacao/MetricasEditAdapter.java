package com.example.startuppulse.ui.preparacao;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.R;
import com.example.startuppulse.data.Metrica;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;
import java.util.function.Consumer;

public class MetricasEditAdapter extends ListAdapter<Metrica, MetricasEditAdapter.MetricaViewHolder> {

    private final Consumer<Metrica> onRemoveCallback;

    public MetricasEditAdapter(Consumer<Metrica> onRemoveCallback) {
        super(DIFF_CALLBACK);
        this.onRemoveCallback = onRemoveCallback;
    }

    @NonNull
    @Override
    public MetricaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_metrica_edit, parent, false);
        return new MetricaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricaViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MetricaViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText nomeMetrica, valorMetrica;
        ImageButton btnRemover;
        // NOVO: TextWatchers para evitar que sejam recriados a cada bind
        private TextWatcher nomeWatcher;
        private TextWatcher valorWatcher;

        public MetricaViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeMetrica = itemView.findViewById(R.id.edit_text_nome_metrica);
            valorMetrica = itemView.findViewById(R.id.edit_text_valor_metrica);
            btnRemover = itemView.findViewById(R.id.btn_remover_metrica);
        }

        public void bind(Metrica metrica) {
            // Remove os listeners antigos para evitar chamadas recursivas ou com dados errados
            if (nomeWatcher != null) {
                nomeMetrica.removeTextChangedListener(nomeWatcher);
            }
            if (valorWatcher != null) {
                valorMetrica.removeTextChangedListener(valorWatcher);
            }

            nomeMetrica.setText(metrica.getNome());
            // --- CORREÇÃO: Converte o valor para String de forma segura ---
            valorMetrica.setText(String.valueOf(metrica.getValor()));

            // --- NOVO: Lógica para salvar os dados enquanto o usuário digita ---
            nomeWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Atualiza o objeto Metrica na lista
                    getItem(getAdapterPosition()).setNome(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            };

            valorWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Atualiza o objeto Metrica na lista
                    getItem(getAdapterPosition()).setValor(Double.parseDouble(s.toString()));
                }
                @Override
                public void afterTextChanged(Editable s) {}
            };

            // Adiciona os novos listeners
            nomeMetrica.addTextChangedListener(nomeWatcher);
            valorMetrica.addTextChangedListener(valorWatcher);

            btnRemover.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onRemoveCallback.accept(getItem(getAdapterPosition()));
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Metrica> DIFF_CALLBACK = new DiffUtil.ItemCallback<Metrica>() {
        @Override
        public boolean areItemsTheSame(@NonNull Metrica oldItem, @NonNull Metrica newItem) {
            // Idealmente, seria melhor usar um ID único aqui, mas o nome funciona se for único.
            return Objects.equals(oldItem.getNome(), newItem.getNome());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Metrica oldItem, @NonNull Metrica newItem) {
            // `Metrica` precisa ter um método `equals()` bem implementado para isso funcionar corretamente.
            return oldItem.equals(newItem);
        }
    };
}