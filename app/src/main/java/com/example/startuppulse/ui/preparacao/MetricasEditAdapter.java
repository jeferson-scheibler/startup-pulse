package com.example.startuppulse.ui.preparacao;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
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
import com.example.startuppulse.data.models.Metrica;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;
import java.util.function.Consumer;

public class MetricasEditAdapter extends ListAdapter<Metrica, MetricasEditAdapter.MetricaViewHolder> {

    private final Consumer<Void> onMetricaEditadaCallback;
    private final Consumer<Metrica> onRemoveCallback;

    public MetricasEditAdapter(Consumer<Metrica> onRemoveCallback, Consumer<Void> onMetricaEditadaCallback) {
        super(DIFF_CALLBACK);
        this.onRemoveCallback = onRemoveCallback;
        this.onMetricaEditadaCallback = onMetricaEditadaCallback;
    }

    @NonNull
    @Override
    public MetricaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_metrica_edit, parent, false);
        return new MetricaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricaViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MetricaViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText nomeMetrica, valorMetrica;
        ImageButton btnRemover;
        private TextWatcher nomeWatcher;
        private TextWatcher valorWatcher;

        public MetricaViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeMetrica = itemView.findViewById(R.id.edit_text_nome_metrica);
            valorMetrica = itemView.findViewById(R.id.edit_text_valor_metrica);
            btnRemover = itemView.findViewById(R.id.btn_remover_metrica);
        }

        public void bind(Metrica metrica) {
            if (nomeWatcher != null) nomeMetrica.removeTextChangedListener(nomeWatcher);
            if (valorWatcher != null) valorMetrica.removeTextChangedListener(valorWatcher);

            nomeMetrica.setText(metrica.getNome());
            valorMetrica.setText(String.valueOf(metrica.getValor()));

            nomeWatcher = new SimpleTextWatcher(s -> {
                metrica.setNome(s.toString());
                onMetricaEditadaCallback.accept(null);
            });

            valorWatcher = new SimpleTextWatcher(s -> {
                try { metrica.setValor(Double.parseDouble(s.toString())); }
                catch (Exception ignored) {}
                onMetricaEditadaCallback.accept(null);
            });

            nomeMetrica.addTextChangedListener(nomeWatcher);
            valorMetrica.addTextChangedListener(valorWatcher);

            btnRemover.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onRemoveCallback.accept(getItem(getAdapterPosition()));
                }
            });
        }
    }

    static class SimpleTextWatcher implements TextWatcher {
        private final Consumer<CharSequence> consumer;

        public SimpleTextWatcher(Consumer<CharSequence> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            consumer.accept(s);
        }

        @Override
        public void afterTextChanged(Editable s) {}
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