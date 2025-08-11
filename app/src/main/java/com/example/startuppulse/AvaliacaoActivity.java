package com.example.startuppulse;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.common.Result;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvaliacaoActivity extends AppCompatActivity {

    private static final String TAG = "AVALIACAO";

    // UI
    private RecyclerView recyclerViewCriterios;
    private TextView textAverageScore;
    private MaterialButton btnEnviarAvaliacao;
    private MaterialToolbar toolbar;

    // Dados
    private AvaliacaoAdapter adapter;
    private String ideiaId;
    private final List<CriterioAvaliacao> criteriosList = new ArrayList<>();
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: iniciando AvaliacaoActivity");
        setContentView(R.layout.activity_avaliacao);

        firestoreHelper = new FirestoreHelper();
        ideiaId = getIntent().getStringExtra("ideia_id");

        if (ideiaId == null || ideiaId.trim().isEmpty()) {
            Toast.makeText(this, "Erro: ID da ideia não foi fornecido.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupToolbar();
        bindUiViews();
        setupCriterios();
        setupRecyclerView();
        setupClickListeners();
        updateAverageScore();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Avaliar Ideia");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindUiViews() {
        textAverageScore = findViewById(R.id.text_average_score);
        recyclerViewCriterios = findViewById(R.id.recycler_view_criterios);
        btnEnviarAvaliacao = findViewById(R.id.btn_enviar_avaliacao);
    }

    private void setupCriterios() {
        criteriosList.clear();
        criteriosList.add(new CriterioAvaliacao("Problema e Solução", "A ideia resolve um problema real de forma eficaz?"));
        criteriosList.add(new CriterioAvaliacao("Mercado Potencial", "O mercado para esta solução é grande e acessível?"));
        criteriosList.add(new CriterioAvaliacao("Originalidade", "Qual o nível de inovação e diferenciação da ideia?"));
        criteriosList.add(new CriterioAvaliacao("Modelo de Negócio", "Existe um caminho claro para a sustentabilidade financeira?"));
    }

    private void setupRecyclerView() {
        adapter = new AvaliacaoAdapter(criteriosList, this::updateAverageScore);
        recyclerViewCriterios.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCriterios.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnEnviarAvaliacao.setOnClickListener(v -> {
            if (!validarCampos()) return;
            // Confirmação antes de enviar
            new AlertDialog.Builder(this)
                    .setTitle("Enviar avaliação?")
                    .setMessage("Após enviar, o autor verá seu feedback.")
                    .setPositiveButton("Enviar", (d, w) -> enviarAvaliacao())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private boolean validarCampos() {
        // Opcional: exigir feedback quando nota < 5 (exemplo de UX)
        for (CriterioAvaliacao c : criteriosList) {
            if (c.nota < 0f || c.nota > 10f) {
                Toast.makeText(this, "Notas devem estar entre 0 e 10.", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (c.nota < 5f && (c.feedback == null || c.feedback.trim().isEmpty())) {
                Toast.makeText(this, "Explique rapidamente por que a nota de \"" + c.nome + "\" foi baixa.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void updateAverageScore() {
        if (criteriosList.isEmpty()) return;
        float total = 0f;
        for (CriterioAvaliacao c : criteriosList) total += c.nota;
        float media = total / criteriosList.size();
        textAverageScore.setText(String.format(Locale.getDefault(), "%.1f", media));
    }

    private void enviarAvaliacao() {
        btnEnviarAvaliacao.setEnabled(false);
        btnEnviarAvaliacao.setText("A enviar...");

        List<Map<String, Object>> avaliacoesParaSalvar = adapter.getAvaliacoes();

        firestoreHelper.salvarAvaliacao(ideiaId, avaliacoesParaSalvar, r -> {
            if (r.isOk()) {
                Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                btnEnviarAvaliacao.setEnabled(true);
                btnEnviarAvaliacao.setText("Enviar Avaliação");
                String msg = (r.error != null && r.error.getMessage() != null)
                        ? r.error.getMessage()
                        : "Erro desconhecido";
                Toast.makeText(this, "Erro ao enviar avaliação: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ================= Modelo interno =================
    private static class CriterioAvaliacao {
        String nome;
        String descricao;
        float nota = 5.0f;   // valor inicial
        String feedback = ""; // opcional

        CriterioAvaliacao(String nome, String descricao) {
            this.nome = nome;
            this.descricao = descricao;
        }
    }

    // ================= Adapter =================
    private static class AvaliacaoAdapter extends RecyclerView.Adapter<AvaliacaoAdapter.CriterioViewHolder> {

        private final List<CriterioAvaliacao> criterios;
        private final Runnable onNotaChanged;

        AvaliacaoAdapter(List<CriterioAvaliacao> criterios, Runnable onNotaChanged) {
            this.criterios = criterios;
            this.onNotaChanged = onNotaChanged;
        }

        @NonNull
        @Override
        public CriterioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_criterio_avaliacao, parent, false);
            return new CriterioViewHolder(v, onNotaChanged);
        }

        @Override
        public void onBindViewHolder(@NonNull CriterioViewHolder holder, int position) {
            holder.bind(criterios.get(position));
        }

        @Override
        public int getItemCount() {
            return criterios.size();
        }

        List<Map<String, Object>> getAvaliacoes() {
            List<Map<String, Object>> out = new ArrayList<>();
            for (CriterioAvaliacao c : criterios) {
                Map<String, Object> m = new HashMap<>();
                m.put("criterio", c.nome);
                m.put("nota", c.nota);
                m.put("feedback", c.feedback);
                out.add(m);
            }
            return out;
        }

        static class CriterioViewHolder extends RecyclerView.ViewHolder {
            final TextView nome;
            final TextView descricao;
            final TextView notaDisplay;
            final Slider sliderNota;
            final EditText feedback;
            private final Runnable onNotaChangedCallback;
            private TextWatcher watcher;

            CriterioViewHolder(@NonNull View itemView, Runnable onNotaChangedCallback) {
                super(itemView);
                this.onNotaChangedCallback = onNotaChangedCallback;
                nome = itemView.findViewById(R.id.text_criterio_nome);
                descricao = itemView.findViewById(R.id.text_criterio_descricao);
                notaDisplay = itemView.findViewById(R.id.text_criterio_nota_display);
                sliderNota = itemView.findViewById(R.id.slider_nota);
                feedback = itemView.findViewById(R.id.edit_text_feedback);
            }

            void bind(final CriterioAvaliacao c) {
                nome.setText(c.nome);
                descricao.setText(c.descricao);

                // Garante range/valor
                if (sliderNota.getValueFrom() != 0f) sliderNota.setValueFrom(0f);
                if (sliderNota.getValueTo() != 10f) sliderNota.setValueTo(10f);
                if (c.nota < 0f) c.nota = 0f;
                if (c.nota > 10f) c.nota = 10f;

                sliderNota.setValue(c.nota);
                notaDisplay.setText(String.format(Locale.getDefault(), "%.1f", c.nota));

                sliderNota.clearOnChangeListeners();
                sliderNota.addOnChangeListener((s, value, fromUser) -> {
                    c.nota = value;
                    notaDisplay.setText(String.format(Locale.getDefault(), "%.1f", value));
                    if (fromUser && onNotaChangedCallback != null) onNotaChangedCallback.run();
                });

                if (watcher != null) feedback.removeTextChangedListener(watcher);
                feedback.setText(c.feedback);
                watcher = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) { c.feedback = s.toString(); }
                };
                feedback.addTextChangedListener(watcher);
            }
        }
    }
}