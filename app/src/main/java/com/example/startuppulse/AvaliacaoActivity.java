package com.example.startuppulse;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.example.startuppulse.data.Avaliacao;
import com.example.startuppulse.databinding.ActivityAvaliacaoBinding; // Import ViewBinding
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvaliacaoActivity extends AppCompatActivity {

    private ActivityAvaliacaoBinding binding; // Usa ViewBinding para segurança e clareza
    private AvaliacaoAdapter adapter;
    private String ideiaId;
    private final List<CriterioAvaliacao> criteriosList = new ArrayList<>();
    private FirestoreHelper firestoreHelper;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAvaliacaoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreHelper = new FirestoreHelper();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ideiaId = getIntent().getStringExtra("ideia_id");

        // Validação robusta na inicialização
        if (ideiaId == null || ideiaId.trim().isEmpty() || currentUser == null) {
            Toast.makeText(this, "Erro: Dados insuficientes para avaliar.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupToolbar();
        setupCriterios();
        setupRecyclerView();
        setupClickListeners();
        updateAverageScore();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Avaliar Ideia");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
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
        binding.recyclerViewCriterios.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewCriterios.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnEnviarAvaliacao.setOnClickListener(v -> {
            if (!validarCampos()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Enviar avaliação?")
                    .setMessage("Após o envio, o autor da ideia receberá seu feedback.")
                    .setPositiveButton("Enviar", (d, w) -> enviarAvaliacao())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private boolean validarCampos() {
        for (CriterioAvaliacao c : criteriosList) {
            if (c.nota < 5f && (c.feedback == null || c.feedback.trim().isEmpty())) {
                Toast.makeText(this, "Para notas baixas em \"" + c.nome + "\", um feedback é obrigatório.", Toast.LENGTH_LONG).show();
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
        binding.textAverageScore.setText(String.format(Locale.getDefault(), "%.1f", media));
    }

    private void enviarAvaliacao() {
        binding.btnEnviarAvaliacao.setEnabled(false);
        binding.btnEnviarAvaliacao.setText("Enviando...");

        List<Avaliacao> criteriosAvaliados = adapter.getAvaliacoesAsList();
        List<Map<String, Object>> avaliacoesParaSalvar = new ArrayList<>();
        for (Avaliacao aval : criteriosAvaliados) {
            Map<String, Object> avalMap = new HashMap<>();
            avalMap.put("nome", aval.getCriterio());
            avalMap.put("nota", aval.getNota());
            avalMap.put("feedback", aval.getFeedback());
            avaliacoesParaSalvar.add(avalMap);
        }


        firestoreHelper.salvarAvaliacao(ideiaId, avaliacoesParaSalvar, r -> {
            if (r.isOk()) {
                Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                binding.btnEnviarAvaliacao.setEnabled(true);
                binding.btnEnviarAvaliacao.setText("Enviar Avaliação");
                String msg = (r.error != null) ? r.error.getMessage() : "Erro desconhecido";
                Toast.makeText(this, "Erro ao enviar: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ================= Modelo interno (semelhante ao seu, mas agora `private`) =================
    private static class CriterioAvaliacao {
        String nome, descricao, feedback = "";
        float nota = 5.0f;
        CriterioAvaliacao(String nome, String descricao) { this.nome = nome; this.descricao = descricao; }
    }

    // ================= Adapter (atualizado para retornar List<Avaliacao>) =================
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
        public int getItemCount() { return criterios.size(); }

        /**
         * Novo método que retorna uma lista de objetos Avaliacao, fortemente tipada.
         */
        List<Avaliacao> getAvaliacoesAsList() {
            List<Avaliacao> out = new ArrayList<>();
            for (CriterioAvaliacao c : criterios) {
                out.add(new Avaliacao(c.nome, c.nota, c.feedback));
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