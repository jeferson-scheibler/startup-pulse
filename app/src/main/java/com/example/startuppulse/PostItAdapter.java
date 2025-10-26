package com.example.startuppulse;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel; // Certifique-se que o import está correto
import com.google.android.material.card.MaterialCardView;

import java.util.Date;
import java.util.Objects;

/**
 * Adapter otimizado para exibir Post-its em um RecyclerView.
 * Utiliza ListAdapter e DiffUtil para performance superior em atualizações de lista.
 * Usa um ViewModel compartilhado para lidar com interações (cliques).
 */
public class PostItAdapter extends ListAdapter<PostIt, PostItAdapter.PostItViewHolder> {

    private final CanvasIdeiaViewModel sharedViewModel;
    private boolean isReadOnly = false; // Estado de somente leitura

    // --- REMOVIDO: A interface OnPostItClickListener foi removida ---

    /**
     * O construtor agora recebe o ViewModel compartilhado e o estado inicial de readOnly.
     */
    public PostItAdapter(CanvasIdeiaViewModel viewModel, boolean isReadOnly) {
        super(DIFF_CALLBACK);
        this.sharedViewModel = viewModel;
        this.isReadOnly = isReadOnly; // Define o estado inicial
    }

    /**
     * Método para atualizar o modo de "somente leitura" dinamicamente.
     * Requer que você chame notifyDataSetChanged() ou use DiffUtil para atualizar a UI.
     * Geralmente, é melhor recriar o adapter ou invalidar o layout se isso mudar frequentemente.
     */
    public void setReadOnly(boolean readOnly) {
        // Verifica se o estado realmente mudou para evitar atualizações desnecessárias
        if (this.isReadOnly != readOnly) {
            this.isReadOnly = readOnly;
            notifyDataSetChanged(); // Notifica que os itens precisam ser redesenhados (afeta listeners)
        }
    }

    @NonNull
    @Override
    public PostItViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_postit, parent, false);
        // --- CORREÇÃO: Não passa mais o listener antigo ---
        return new PostItViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostItViewHolder holder, int position) {
        PostIt postIt = getItem(position);
        // Passa o PostIt, ViewModel e o estado readOnly atual para o bind
        holder.bind(postIt, sharedViewModel, isReadOnly);
    }

    // ===================================================================
    // ==         ViewHolder Interno e Lógica de Performance            ==
    // ===================================================================
    static class PostItViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPostIt, textViewLastModified;
        MaterialCardView cardView;

        // --- CORREÇÃO: Construtor não precisa mais do listener ---
        public PostItViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPostIt = itemView.findViewById(R.id.text_view_postit_content);
            cardView = itemView.findViewById(R.id.postit_card);
            textViewLastModified = itemView.findViewById(R.id.text_view_last_modified);
            // Listeners são configurados no bind agora, onde temos o objeto PostIt específico
        }

        @SuppressLint("SetTextI18n") // Para o DateFormat
        void bind(final PostIt postIt, final CanvasIdeiaViewModel viewModel, boolean isReadOnly) {
            textViewPostIt.setText(postIt.getTexto());
            setCardColor(postIt.getCor());
            setLastModified(postIt.getLastModified());

            // Configura o OnClickListener para chamar o ViewModel
            itemView.setOnClickListener(v -> {
                if (!isReadOnly) { // Só permite editar se não for read-only
                    viewModel.requestEditPostIt(postIt); // Chama o ViewModel
                }
            });

            // --- CORREÇÃO: Configura o OnLongClickListener para chamar o ViewModel ---
            // (Assumindo que você adicione um método requestDeletePostIt no ViewModel)
            itemView.setOnLongClickListener(v -> {
                if (!isReadOnly) {
                    // Exemplo: Chamar um método no ViewModel para exclusão
                    // viewModel.requestDeletePostIt(postIt);
                    // Retorne true para indicar que o evento foi consumido
                    // Se você não tiver a lógica de exclusão ainda, pode deixar comentado
                    // ou retornar false.
                    Toast.makeText(v.getContext(), "Segure para excluir (TODO)", Toast.LENGTH_SHORT).show(); // Placeholder
                    return true;
                }
                return false; // Não consome o evento se for read-only
            });
        }

        private void setCardColor(String corHex) {
            try {
                int backgroundColor = Color.parseColor(corHex != null && !corHex.isEmpty() ? corHex : "#FFFF00"); // Amarelo padrão
                cardView.setCardBackgroundColor(backgroundColor);
                textViewPostIt.setTextColor(isColorLight(backgroundColor) ? Color.BLACK : Color.WHITE);
                textViewLastModified.setTextColor(isColorLight(backgroundColor) ? Color.parseColor("#80000000") : Color.parseColor("#80FFFFFF")); // Cor semi-transparente
            } catch (IllegalArgumentException e) {
                cardView.setCardBackgroundColor(Color.YELLOW);
                textViewPostIt.setTextColor(Color.BLACK);
                textViewLastModified.setTextColor(Color.parseColor("#80000000"));
            }
        }

        private void setLastModified(Date lastModified) {
            if (lastModified != null) {
                // Formato mais conciso
                String dataFormatada = (String) DateFormat.format("dd/MM HH:mm", lastModified);
                textViewLastModified.setText(dataFormatada);
                textViewLastModified.setVisibility(View.VISIBLE);
            } else {
                textViewLastModified.setVisibility(View.GONE);
            }
        }

        private boolean isColorLight(int color) {
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return darkness < 0.5;
        }
    }

    /**
     * Implementação do DiffUtil para atualizações eficientes.
     */
    private static final DiffUtil.ItemCallback<PostIt> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PostIt>() {
                @Override
                public boolean areItemsTheSame(@NonNull PostIt oldItem, @NonNull PostIt newItem) {
                    // Assumindo que PostIt tem um getId() estável
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @SuppressLint("DiffUtilEquals") // Suprime aviso se PostIt.equals() for complexo
                @Override
                public boolean areContentsTheSame(@NonNull PostIt oldItem, @NonNull PostIt newItem) {
                    // Assumindo que PostIt tem um método equals() que compara todos os campos relevantes (texto, cor, data)
                    return oldItem.equals(newItem);
                }
            };
}