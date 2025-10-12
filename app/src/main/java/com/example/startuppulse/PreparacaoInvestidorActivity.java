package com.example.startuppulse;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.ActivityPreparacaoInvestidorBinding;

import java.util.ArrayList;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.LayoutInflater;
import com.google.android.material.textfield.TextInputEditText;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.view.ViewGroup;

import android.app.ProgressDialog;
import android.net.Uri;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PreparacaoInvestidorActivity extends AppCompatActivity {

    private ActivityPreparacaoInvestidorBinding binding;
    private FirestoreHelper firestoreHelper;
    private Ideia ideia;
    private String ideiaId;

    private EquipeEditAdapter equipeAdapter;
    private ArrayList<MembroEquipe> equipeList = new ArrayList<>();
    private ArrayList<Metrica> metricasList = new ArrayList<>();

    private Uri pitchDeckUri; // Para armazenar a URI do arquivo selecionado
    private String pitchDeckUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreparacaoInvestidorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreHelper = new FirestoreHelper();

        ideiaId = getIntent().getStringExtra("IDEIA_ID");
        if (ideiaId == null || ideiaId.isEmpty()) {
            Toast.makeText(this, "Erro: ID da ideia não encontrado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerView();
        setupClickListeners();

        loadIdeiaData();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        equipeAdapter = new EquipeEditAdapter(equipeList, this::removerMembro);
        binding.recyclerEquipe.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEquipe.setAdapter(equipeAdapter);
    }

    private void setupClickListeners() {
        binding.btnAdicionarMembro.setOnClickListener(v -> showAddMembroDialog());
        binding.btnAnexarPitch.setOnClickListener(v -> anexarPitchDeck());
        binding.btnAdicionarMetrica.setOnClickListener(v -> adicionarNovaMetricaView(null));
        binding.btnSalvarLiberarAcesso.setOnClickListener(v -> salvarEFinalizar());
    }

    private void loadIdeiaData() {
        // Exibe um loading (implementação futura)
        firestoreHelper.findIdeiaById(ideiaId, result -> {
            // Esconde o loading (implementação futura)
            if (result.isOk() && result.data != null) {
                this.ideia = result.data;
                updateUI();
            } else {
                Toast.makeText(this, "Falha ao carregar dados da ideia.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (ideia == null) return;

        if (ideia.getEquipe() != null) {
            equipeList.clear();
            equipeList.addAll(ideia.getEquipe());
            equipeAdapter.notifyDataSetChanged();
        }

        if (ideia.getMetricas() != null) {
            metricasList.clear();
            metricasList.addAll(ideia.getMetricas());
            binding.layoutMetricasContainer.removeAllViews(); // Limpa views antigas
            for (Metrica metrica : metricasList) {
                adicionarNovaMetricaView(metrica);
            }
        }

        if (ideia.getPitchDeckUrl() != null && !ideia.getPitchDeckUrl().isEmpty()) {
            binding.textPitchDeckStatus.setText("Arquivo anexado com sucesso!");
            binding.textPitchDeckStatus.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        }
    }

    private void showAddMembroDialog() {
        // Infla o layout customizado
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_membro, null);
        final TextInputEditText inputNome = dialogView.findViewById(R.id.edit_text_nome_membro);
        final TextInputEditText inputFuncao = dialogView.findViewById(R.id.edit_text_funcao_membro);
        final TextInputEditText inputLinkedin = dialogView.findViewById(R.id.edit_text_linkedin_membro);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Adicionar Membro da Equipe")
                .setView(dialogView)
                .setPositiveButton("Adicionar", (dialog, which) -> {
                    String nome = inputNome.getText().toString().trim();
                    String funcao = inputFuncao.getText().toString().trim();
                    String linkedin = inputLinkedin.getText().toString().trim(); // <-- OBTÉM O VALOR

                    if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(funcao)) {
                        Toast.makeText(this, "Nome e função são obrigatórios.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    MembroEquipe novoMembro = new MembroEquipe(nome, funcao, "");
                    novoMembro.setLinkedinUrl(linkedin);
                    equipeList.add(novoMembro);
                    equipeAdapter.notifyItemInserted(equipeList.size() - 1);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void removerMembro(MembroEquipe membro) {
        equipeList.remove(membro);
        equipeAdapter.notifyDataSetChanged(); // Notifica o adapter da remoção
        Toast.makeText(this, membro.getNome() + " foi removido(a).", Toast.LENGTH_SHORT).show();
    }

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    pitchDeckUri = uri;
                    binding.textPitchDeckStatus.setText("Arquivo selecionado. Pronto para upload.");
                    binding.textPitchDeckStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
                    // Inicia o upload imediatamente após a seleção
                    uploadPitchDeck();
                }
            }
    );

    private void anexarPitchDeck() {
        // Permite selecionar PDFs e apresentações PowerPoint
        filePickerLauncher.launch("*/*");
    }

    private void uploadPitchDeck() {
        if (pitchDeckUri == null) return;

        // Mostra um diálogo de progresso
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Enviando Pitch Deck...");
        progressDialog.setMessage("Por favor, aguarde.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Define o caminho no Firebase Storage: pitch_decks/ideiaId/nome_original_do_arquivo
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("pitch_decks/" + ideiaId + "/" + pitchDeckUri.getLastPathSegment());

        storageRef.putFile(pitchDeckUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Se o upload for bem-sucedido, pega a URL de download
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        pitchDeckUrl = downloadUri.toString();
                        binding.textPitchDeckStatus.setText("Upload concluído com sucesso!");
                        binding.textPitchDeckStatus.setTextColor(ContextCompat.getColor(this, R.color.green_success));
                        progressDialog.dismiss();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Falha no upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Atualiza o progresso no diálogo
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Enviando... " + (int) progress + "%");
                });
    }

    private void adicionarNovaMetricaView(@Nullable Metrica metrica) {
        // Infla a view do item de métrica
        LayoutInflater inflater = LayoutInflater.from(this);
        View metricaView = inflater.inflate(R.layout.item_metrica_edit, binding.layoutMetricasContainer, false);

        TextInputEditText nomeMetrica = metricaView.findViewById(R.id.edit_text_nome_metrica);
        TextInputEditText valorMetrica = metricaView.findViewById(R.id.edit_text_valor_metrica);
        ImageButton btnRemover = metricaView.findViewById(R.id.btn_remover_metrica);

        // Se uma métrica existente for passada (ao carregar os dados), preenche os campos
        if (metrica != null) {
            nomeMetrica.setText(metrica.getNome());
            valorMetrica.setText((int) metrica.getValor());
        }

        // Configura o botão de remoção
        btnRemover.setOnClickListener(v -> {
            // Remove a view do seu pai (o LinearLayout)
            ((ViewGroup) metricaView.getParent()).removeView(metricaView);
        });

        // Adiciona a nova view ao container
        binding.layoutMetricasContainer.addView(metricaView);
    }

    private void salvarEFinalizar() {
        ArrayList<Metrica> metricasFinais = new ArrayList<>();
        LinearLayout container = binding.layoutMetricasContainer;
        for (int i = 0; i < container.getChildCount(); i++) {
            View metricaView = container.getChildAt(i);
            TextInputEditText nomeMetricaInput = metricaView.findViewById(R.id.edit_text_nome_metrica);
            TextInputEditText valorMetricaInput = metricaView.findViewById(R.id.edit_text_valor_metrica);

            String nome = nomeMetricaInput.getText().toString().trim();
            String valor = valorMetricaInput.getText().toString().trim();

            if (!nome.isEmpty() && !valor.isEmpty()) {
                metricasFinais.add(new Metrica(nome, valor));
            }
        }

        if (ideia == null) return;
        ideia.setEquipe(equipeList);
        ideia.setMetricas(metricasFinais);
        if (pitchDeckUrl != null) { // Apenas atualiza se um novo pitch foi enviado
            ideia.setPitchDeckUrl(pitchDeckUrl);
        }
        ideia.setProntaParaInvestidores(true);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Salvando dados...");
        progressDialog.show();

        firestoreHelper.updateIdeia(ideia, result -> {
            progressDialog.dismiss();
            if (result.isOk()) {
                Toast.makeText(this, "Sua ideia está pronta para investidores!", Toast.LENGTH_LONG).show();
                finish(); // Fecha a activity e volta para a tela de status
            } else {
                Toast.makeText(this, "Erro ao salvar: " + result.err.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}