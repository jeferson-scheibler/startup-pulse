package com.example.startuppulse.ui.avaliacao;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.R;
import com.example.startuppulse.data.models.Avaliacao;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AvaliacaoAdapter extends RecyclerView.Adapter<AvaliacaoAdapter.CriterioViewHolder> {

    private final List<CriterioAvaliacao> criterios;
    private final Runnable onNotaChanged;

    public AvaliacaoAdapter(List<CriterioAvaliacao> criterios, Runnable onNotaChanged) {
        this.criterios = criterios;
        this.onNotaChanged = onNotaChanged;
    }

    @NonNull
    @Override
    public CriterioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_criterio_avaliacao, parent, false);
        return new CriterioViewHolder(view, onNotaChanged);
    }

    @Override
    public void onBindViewHolder(@NonNull CriterioViewHolder holder, int position) {
        holder.bind(criterios.get(position));
    }

    @Override
    public int getItemCount() {
        return criterios.size();
    }

    /**
     * Coleta os dados da UI e os converte em uma lista de objetos Avaliacao,
     * pronta para ser enviada ao ViewModel e salva no Firestore.
     */
    public List<Avaliacao> getAvaliacoesAsList() {
        List<Avaliacao> avaliacoesFinais = new ArrayList<>();
        for (CriterioAvaliacao c : criterios) {
            avaliacoesFinais.add(new Avaliacao(c.nome, c.nota, c.feedback));
        }
        return avaliacoesFinais;
    }

    // --- ViewHolder ---

    static class CriterioViewHolder extends RecyclerView.ViewHolder {
        final TextView nome;
        final TextView descricao;
        final TextView notaDisplay;
        final Slider sliderNota;
        final EditText feedback;
        private final Runnable onNotaChangedCallback;
        private TextWatcher textWatcher;

        CriterioViewHolder(@NonNull View itemView, Runnable onNotaChangedCallback) {
            super(itemView);
            this.onNotaChangedCallback = onNotaChangedCallback;
            nome = itemView.findViewById(R.id.text_criterio_nome);
            descricao = itemView.findViewById(R.id.text_criterio_descricao);
            notaDisplay = itemView.findViewById(R.id.text_criterio_nota_display);
            sliderNota = itemView.findViewById(R.id.slider_nota);
            feedback = itemView.findViewById(R.id.edit_text_feedback);
        }

        void bind(final CriterioAvaliacao criterio) {
            nome.setText(criterio.nome);
            descricao.setText(criterio.descricao);

            // Configuração do Slider
            sliderNota.setValue(criterio.nota);
            notaDisplay.setText(String.format(Locale.getDefault(), "%.1f", criterio.nota));

            sliderNota.clearOnChangeListeners();
            sliderNota.addOnChangeListener((slider, value, fromUser) -> {
                criterio.nota = value;
                notaDisplay.setText(String.format(Locale.getDefault(), "%.1f", value));
                if (fromUser && onNotaChangedCallback != null) {
                    onNotaChangedCallback.run();
                }
            });

            // Configuração do EditText de Feedback
            if (textWatcher != null) {
                feedback.removeTextChangedListener(textWatcher);
            }
            feedback.setText(criterio.feedback);
            textWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    criterio.feedback = s.toString();
                }
            };
            feedback.addTextChangedListener(textWatcher);
        }
    }
}