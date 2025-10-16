package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
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

        public MetricaViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeMetrica = itemView.findViewById(R.id.edit_text_nome_metrica);
            valorMetrica = itemView.findViewById(R.id.edit_text_valor_metrica);
            btnRemover = itemView.findViewById(R.id.btn_remover_metrica);
        }

        public void bind(Metrica metrica) {
            nomeMetrica.setText(metrica.getNome());
            valorMetrica.setText(metrica.getValor());
            btnRemover.setOnClickListener(v -> onRemoveCallback.accept(getItem(getAdapterPosition())));
        }
    }

    private static final DiffUtil.ItemCallback<Metrica> DIFF_CALLBACK = new DiffUtil.ItemCallback<Metrica>() {
        @Override
        public boolean areItemsTheSame(@NonNull Metrica oldItem, @NonNull Metrica newItem) {
            return Objects.equals(oldItem.getNome(), newItem.getNome());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Metrica oldItem, @NonNull Metrica newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
}