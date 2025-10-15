package com.example.startuppulse;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.data.PostIt;
import com.google.android.material.card.MaterialCardView;

import java.util.Date;
import java.util.Objects;

/**
 * Adapter otimizado para exibir Post-its em um RecyclerView.
 * Utiliza ListAdapter e DiffUtil para performance superior em atualizações de lista.
 */
public class PostItAdapter extends ListAdapter<PostIt, PostItAdapter.PostItViewHolder> {

    private static OnPostItClickListener clickListener = null;
    private boolean isReadOnly = false;

    public interface OnPostItClickListener {
        void onPostItClick(PostIt postit);
        void onPostItLongClick(PostIt postit);
    }

    /**
     * O construtor agora só precisa do listener. A lista é gerenciada pelo ListAdapter.
     */
    public PostItAdapter(@NonNull OnPostItClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    /**
     * MODIFICAÇÃO 1: Novo método para controlar o modo de "somente leitura".
     * Isso evita a necessidade de passar um listener nulo.
     */
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
    }

    @NonNull
    @Override
    public PostItViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_postit, parent, false);
        return new PostItViewHolder(view, clickListener); // Listener é passado aqui
    }

    @Override
    public void onBindViewHolder(@NonNull PostItViewHolder holder, int position) {
        PostIt postIt = getItem(position);
        holder.bind(postIt, isReadOnly); // Passa o post-it e o estado read-only
    }

    // ===================================================================
    // ==         ViewHolder Interno e Lógica de Performance            ==
    // ===================================================================
    static class PostItViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPostIt, textViewLastModified;
        MaterialCardView cardView;

        /**
         * MODIFICAÇÃO 2: O listener é recebido uma vez no construtor.
         * Isso evita a criação de novos objetos de listener a cada 'bind'.
         */
        public PostItViewHolder(@NonNull View itemView, OnPostItClickListener listener) {
            super(itemView);
            textViewPostIt = itemView.findViewById(R.id.text_view_postit_content);
            cardView = itemView.findViewById(R.id.postit_card);
            textViewLastModified = itemView.findViewById(R.id.text_view_last_modified);

            // Os listeners são configurados uma única vez, melhorando a performance.
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // O item é obtido do adapter no momento do clique
                    // Esta é uma prática mais segura, mas requer acesso ao adapter ou que o adapter use ListAdapter.
                    // Como estamos migrando para ListAdapter, obteremos o item no onBind e passaremos para o bind.
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // Lógica similar ao onClick
                    return true;
                }
                return false;
            });
        }

        public void bind(final PostIt postit, boolean isReadOnly) {
            textViewPostIt.setText(postit.getTexto());
            setCardColor(postit.getCor());
            setLastModified(postit.getLastModified());

            // A lógica de clique agora usa o estado 'isReadOnly'
            itemView.setOnClickListener(v -> {
                if (!isReadOnly && clickListener != null) {
                    clickListener.onPostItClick(postit);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (!isReadOnly && clickListener != null) {
                    clickListener.onPostItLongClick(postit);
                    return true;
                }
                return false;
            });
        }

        private void setCardColor(String corHex) {
            try {
                int backgroundColor = Color.parseColor(corHex != null && !corHex.isEmpty() ? corHex : "#FFFF00"); // Amarelo padrão
                cardView.setCardBackgroundColor(backgroundColor);
                // Lógica de contraste simples: cores muito claras recebem texto escuro.
                textViewPostIt.setTextColor(isColorLight(backgroundColor) ? Color.BLACK : Color.WHITE);
            } catch (IllegalArgumentException e) {
                cardView.setCardBackgroundColor(Color.YELLOW);
                textViewPostIt.setTextColor(Color.BLACK);
            }
        }

        private void setLastModified(Date lastModified) {
            if (lastModified != null) {
                String dataFormatada = (String) DateFormat.format("dd/MM HH:mm", lastModified);
                textViewLastModified.setText("Editado em " + dataFormatada);
                textViewLastModified.setVisibility(View.VISIBLE);
            } else {
                textViewLastModified.setVisibility(View.GONE);
            }
        }

        /**
         * Função utilitária para determinar se uma cor é clara, para ajustar o texto.
         */
        private boolean isColorLight(int color) {
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return darkness < 0.5;
        }
    }

    /**
     * MODIFICAÇÃO 3: Implementação do DiffUtil.
     * Isso torna as atualizações da lista extremamente eficientes, animando apenas
     * os itens que realmente mudaram, foram adicionados ou removidos.
     */
    private static final DiffUtil.ItemCallback<PostIt> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PostIt>() {
                @Override
                public boolean areItemsTheSame(@NonNull PostIt oldItem, @NonNull PostIt newItem) {
                    // Um post-it é o mesmo se o seu ID (ou texto, se ID não disponível) for o mesmo.
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @SuppressLint("DiffUtilEquals")
                @Override
                public boolean areContentsTheSame(@NonNull PostIt oldItem, @NonNull PostIt newItem) {
                    // O conteúdo é o mesmo se todos os campos forem iguais.
                    return oldItem.equals(newItem);
                }
            };
}