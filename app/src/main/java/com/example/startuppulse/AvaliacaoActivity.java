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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * VERSÃO REFEITA E ROBUSTA - 07/07/2025
 *
 * Esta classe foi reescrita para ser mais segura e para incluir logs de diagnóstico detalhados.
 * Se a tela fechar inesperadamente, verifique o Logcat filtrando pela tag "AVALIACAO_DEBUG".
 */
public class AvaliacaoActivity extends AppCompatActivity {

    private static final String TAG = "AVALIACAO";

    // Componentes da UI
    private RecyclerView recyclerViewCriterios;
    private TextView textAverageScore;
    private MaterialButton btnEnviarAvaliacao;
    private MaterialToolbar toolbar;

    // Dados e Helpers
    private AvaliacaoAdapter adapter;
    private String ideiaId;
    private final List<CriterioAvaliacao> criteriosList = new ArrayList<>();
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Iniciando a AvaliacaoActivity.");

        try {
            setContentView(R.layout.activity_avaliacao);

            firestoreHelper = new FirestoreHelper();

            ideiaId = getIntent().getStringExtra("ideia_id");

            if (ideiaId == null || ideiaId.trim().isEmpty()) {
                Toast.makeText(this, "Erro: ID da ideia não foi fornecido.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // 3. Configurar a UI
            setupToolbar();
            bindUiViews();
            setupCriterios();
            setupRecyclerView();
            setupClickListeners();

            // 4. Atualizar a UI com os dados iniciais
            updateAverageScore();

            Log.d(TAG, "onCreate: Configuração da tela concluída com sucesso.");

        } catch (Exception e) {
            // Se qualquer coisa der errado (ex: um ID de view não encontrado no XML), este bloco irá capturar.
            Log.e(TAG, "onCreate: OCORREU UMA EXCEÇÃO INESPERADA! A tela pode fechar.", e);
            Toast.makeText(this, "Ocorreu um erro crítico ao abrir a avaliação.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        Log.d(TAG, "setupToolbar: Toolbar configurada.");
    }

    private void bindUiViews() {
        textAverageScore = findViewById(R.id.text_average_score);
        recyclerViewCriterios = findViewById(R.id.recycler_view_criterios);
        btnEnviarAvaliacao = findViewById(R.id.btn_enviar_avaliacao);
        Log.d(TAG, "bindUiViews: Views da UI vinculadas com sucesso.");
    }

    private void setupCriterios() {
        criteriosList.clear(); // Garante que a lista está limpa antes de adicionar
        criteriosList.add(new CriterioAvaliacao("Problema e Solução", "A ideia resolve um problema real de forma eficaz?"));
        criteriosList.add(new CriterioAvaliacao("Mercado Potencial", "O mercado para esta solução é grande e acessível?"));
        criteriosList.add(new CriterioAvaliacao("Originalidade", "Qual o nível de inovação e diferenciação da ideia?"));
        criteriosList.add(new CriterioAvaliacao("Modelo de Negócio", "Existe um caminho claro para a sustentabilidade financeira?"));
        Log.d(TAG, "setupCriterios: Lista de critérios de avaliação criada.");
    }

    private void setupRecyclerView() {
        adapter = new AvaliacaoAdapter(criteriosList, this::updateAverageScore);
        recyclerViewCriterios.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCriterios.setAdapter(adapter);
        Log.d(TAG, "setupRecyclerView: RecyclerView e Adapter configurados.");
    }

    private void setupClickListeners() {
        btnEnviarAvaliacao.setOnClickListener(v -> enviarAvaliacao());
        Log.d(TAG, "setupClickListeners: Listener do botão de enviar configurado.");
    }

    private void updateAverageScore() {
        if (criteriosList.isEmpty()) return;

        float total = 0;
        for (CriterioAvaliacao criterio : criteriosList) {
            total += criterio.nota;
        }
        float media = total / criteriosList.size();
        textAverageScore.setText(String.format(Locale.US, "%.1f", media));
        Log.d(TAG, "updateAverageScore: Média calculada e exibida: " + media);
    }

    private void enviarAvaliacao() {
        Log.d(TAG, "enviarAvaliacao: Botão de enviar clicado. Tentando salvar no Firestore.");
        btnEnviarAvaliacao.setEnabled(false);
        btnEnviarAvaliacao.setText("A enviar...");

        List<Map<String, Object>> avaliacoesParaSalvar = adapter.getAvaliacoes();

        firestoreHelper.salvarAvaliacao(ideiaId, avaliacoesParaSalvar, new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AvaliacaoActivity.this, "Avaliação enviada com sucesso!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AvaliacaoActivity.this, "Erro ao enviar avaliação: " + e.getMessage(), Toast.LENGTH_LONG).show();
                btnEnviarAvaliacao.setEnabled(true);
                btnEnviarAvaliacao.setText("Enviar Avaliação");
            }
        });
    }

    // ===================================================================
    // ==     Classe interna para o Modelo de Dados do Critério         ==
    // ===================================================================
    private static class CriterioAvaliacao {
        String nome;
        String descricao;
        float nota = 5.0f;
        String feedback = "";

        CriterioAvaliacao(String nome, String descricao) {
            this.nome = nome;
            this.descricao = descricao;
        }
    }

    // ===================================================================
    // ==     Classe interna para o Adaptador do RecyclerView           ==
    // ===================================================================
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

        public List<Map<String, Object>> getAvaliacoes() {
            List<Map<String, Object>> lista = new ArrayList<>();
            for (CriterioAvaliacao c : criterios) {
                Map<String, Object> map = new HashMap<>();
                map.put("criterio", c.nome);
                map.put("nota", c.nota);
                map.put("feedback", c.feedback);
                lista.add(map);
            }
            return lista;
        }

        static class CriterioViewHolder extends RecyclerView.ViewHolder {
            TextView nome, descricao, notaDisplay;
            Slider sliderNota;
            EditText feedback;
            Runnable onNotaChangedCallback;
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
                sliderNota.setValue(criterio.nota);
                notaDisplay.setText(String.format(Locale.US, "%.1f", criterio.nota));
                feedback.setText(criterio.feedback);

                sliderNota.clearOnChangeListeners();
                sliderNota.addOnChangeListener((slider, value, fromUser) -> {
                    criterio.nota = value;
                    notaDisplay.setText(String.format(Locale.US, "%.1f", value));
                    if (fromUser && onNotaChangedCallback != null) {
                        onNotaChangedCallback.run();
                    }
                });

                if (textWatcher != null) {
                    feedback.removeTextChangedListener(textWatcher);
                }
                textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        criterio.feedback = s.toString();
                    }
                };
                feedback.addTextChangedListener(textWatcher);
            }
        }
    }
}