package com.example.startuppulse;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

/**
 * VERSÃO FINAL E CORRIGIDA do Adapter.
 * Esta versão utiliza a classe PostIt, garantindo segurança de tipos e
 * resolvendo o problema de serialização.
 */
public class PostItAdapter extends RecyclerView.Adapter<PostItAdapter.PostItViewHolder> {

    // 1. A interface agora trabalha com o objeto PostIt, que é mais seguro.
    public interface OnPostItClickListener {
        void onPostItClick(PostIt postit);
        void onPostItLongClick(PostIt postit);
    }

    // 2. A lista interna agora armazena objetos PostIt.
    private final List<PostIt> postIts;
    private final OnPostItClickListener clickListener;

    // 3. O construtor aceita uma List<PostIt>.
    public PostItAdapter(List<PostIt> postIts, OnPostItClickListener clickListener) {
        this.postIts = new ArrayList<>(postIts); // Cria uma cópia para evitar problemas de referência
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PostItViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_postit, parent, false);
        return new PostItViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostItViewHolder holder, int position) {
        // 4. Pega um objeto PostIt da lista.
        PostIt postIt = postIts.get(position);
        holder.bind(postIt, clickListener);
    }

    @Override
    public int getItemCount() {
        return postIts.size();
    }

    // 5. O método de atualização também espera uma List<PostIt>.
    public void updatePostIts(List<PostIt> novosPostIts) {
        this.postIts.clear();
        if (novosPostIts != null) {
            this.postIts.addAll(novosPostIts);
        }
        notifyDataSetChanged(); // Notifica o RecyclerView sobre a mudança de dados.
    }

    // ===================================================================
    // ==                     ViewHolder Interno                        ==
    // ===================================================================
    static class PostItViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPostIt,textViewLastModified;
        MaterialCardView cardView;

        public PostItViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewPostIt = itemView.findViewById(R.id.text_view_postit_content);
            cardView = itemView.findViewById(R.id.postit_card);
            textViewLastModified = itemView.findViewById(R.id.text_view_last_modified);
        }
        public void bind(final PostIt postit, final OnPostItClickListener listener) {
            textViewPostIt.setText(postit.getTexto());

            // Define a cor do post-it a partir do objeto
            String corHex = postit.getCor();
            try {
                if (corHex != null && !corHex.isEmpty()) {
                    cardView.setCardBackgroundColor(Color.parseColor(corHex));
                    // Ajusta a cor do texto para garantir o contraste
                    if ("#FFFFFF".equalsIgnoreCase(corHex) || "#FFFFFFFF".equalsIgnoreCase(corHex)) {
                        textViewPostIt.setTextColor(Color.BLACK);
                    } else {
                        textViewPostIt.setTextColor(Color.parseColor("#424242"));
                    }
                } else {
                    // Cor padrão se nenhuma for definida
                    cardView.setCardBackgroundColor(Color.parseColor("#F9F871"));
                    textViewPostIt.setTextColor(Color.parseColor("#424242"));
                }
            } catch (IllegalArgumentException e) {
                // Cor padrão em caso de erro no formato da cor
                cardView.setCardBackgroundColor(Color.parseColor("#F9F871"));
                textViewPostIt.setTextColor(Color.parseColor("#424242"));
            }

            // Configura os listeners para passar o objeto PostIt seguro
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostItClick(postit);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onPostItLongClick(postit);
                    return true; // Evento consumido
                }
                return false;
            });

            if (postit.getLastModified() != null) {
                // Formata a data para um formato amigável (ex: 07/07 22:30)
                android.text.format.DateFormat df = new android.text.format.DateFormat();
                String dataFormatada = (String) df.format("dd/MM HH:mm", postit.getLastModified());

                textViewLastModified.setText("Editado em " + dataFormatada);
                textViewLastModified.setVisibility(View.VISIBLE);
            } else {
                // Se nunca foi editado, o campo fica escondido
                textViewLastModified.setVisibility(View.GONE);
            }
        }
    }
}