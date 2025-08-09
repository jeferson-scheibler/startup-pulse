package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CanvasIdeiaActivity extends AppCompatActivity implements CanvasInicioFragment.InicioStateListener, CanvasBlockFragment.CanvasBlockListener, AmbienteCheckFragment.AmbienteCheckListener {

    private ViewPager2 viewPager;
    private CanvasPagerAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private Ideia ideia;
    private List<CanvasEtapa> etapas;
    private ListenerRegistration ideiaListener;

    // Componentes da UI
    private ProgressBar loadingIndicator;
    private Group contentGroup;
    private MaterialButton btnProximo, btnAnterior, btnPublicar, btnDespublicar, btnAvaliar;
    private ImageView btnVoltar;

    private AlertDialog loadingDialog;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isReadOnlyMode = false;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> avaliacaoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas_ideia);

        firestoreHelper = new FirestoreHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loadingIndicator = findViewById(R.id.loading_indicator);
        contentGroup = findViewById(R.id.content_group);

        setupLocationPermissionLauncher();

        ideia = (Ideia) getIntent().getSerializableExtra("ideia");
        String ideiaId = getIntent().getStringExtra("ideia_id");
        boolean openAsReadOnly = getIntent().getBooleanExtra("isReadOnly", false);

        if (ideia != null && ideia.getId() == null) {
            setupForNewIdeia();
        } else if (ideiaId != null) {
            setupForExistingIdeia(ideiaId, openAsReadOnly);
        } else {
            Toast.makeText(this, "Erro: Nenhuma ideia ou ID fornecido.", Toast.LENGTH_SHORT).show();
            finish();
        }
        avaliacaoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Este código será executado quando a AvaliacaoActivity fechar
                    if (result.getResultCode() == RESULT_OK) {
                        // SUCESSO! A avaliação foi enviada.
                        // A melhor forma de atualizar a UI é forçar o recarregamento dos dados
                        // da ideia, re-anexando o listener do Firestore.
                        Log.d("CANVAS_IDEIA", "Resultado OK recebido da AvaliacaoActivity. A recarregar dados.");
                        Toast.makeText(this, "Status da ideia atualizado!", Toast.LENGTH_SHORT).show();

                        if (ideia != null && ideia.getId() != null) {
                            // Re-anexar o listener força uma nova leitura do banco de dados,
                            // o que atualiza a UI com o novo status "Avaliada".
                            attachIdeiaListener(ideia.getId(), isReadOnlyMode);
                        }
                    }
                }
        );
    }

    private void setupForNewIdeia() {
        showLoading(false);
        this.isReadOnlyMode = false;
        setupEtapas(ideia.getStatus());
        setupUI();
        setupButtonClickListeners();
    }

    private void setupForExistingIdeia(String ideiaId, boolean openAsReadOnly) {
        showLoading(true);
        attachIdeiaListener(ideiaId, openAsReadOnly);
    }

    private void setupUI() {
        viewPager = findViewById(R.id.view_pager_canvas);
        btnProximo = findViewById(R.id.btn_proximo);
        btnAnterior = findViewById(R.id.btn_anterior);
        btnVoltar = findViewById(R.id.btn_voltar);
        btnPublicar = findViewById(R.id.btn_publicar_ideia);
        btnDespublicar = findViewById(R.id.btn_despublicar_ideia);
        btnAvaliar = findViewById(R.id.btn_avaliar_ideia);
        btnAvaliar.setEnabled(false);

        adapter = new CanvasPagerAdapter(this, ideia, etapas, isReadOnlyMode);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(isReadOnlyMode);

        setupTabs();
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (btnAnterior == null) return;

                updateActionButtonsVisibility(position);
            }
        });
    }

    private void updateActionButtonsVisibility(int position) {
        boolean isLastPage = (position == adapter.getItemCount() - 1);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = user != null && ideia != null && user.getUid().equals(ideia.getOwnerId());
        boolean isMentor = user != null && ideia != null && user.getUid().equals(ideia.getMentorId());

        // Botão ANTERIOR
        btnAnterior.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);

        // Botão PRÓXIMO
        btnProximo.setVisibility(View.VISIBLE); // Sempre visível
        btnProximo.setEnabled(!isLastPage);    // Desativado na última página
        btnProximo.setAlpha(isLastPage ? 0.5f : 1.0f);

        // Lógica para o check-up de ambiente (sobrepõe a lógica do Próximo se necessário)
        if (etapas != null && etapas.size() > position && !isReadOnlyMode) {
            String etapaKey = etapas.get(position).getKey();
            if ("AMBIENTE_CHECK".equals(etapaKey)) {
                btnProximo.setEnabled(false);
                btnProximo.setAlpha(0.5f);
            }
        }

        if (isMentor && "PUBLICADA".equals(ideia.getStatus())) {
            btnPublicar.setVisibility(View.GONE);
            btnDespublicar.setVisibility(View.GONE);
            btnAvaliar.setVisibility(View.VISIBLE);
            btnAvaliar.setEnabled(true);
        } else if (isOwner) {
            if ("PUBLICADA".equals(ideia.getStatus())) {
                btnPublicar.setVisibility(View.GONE);
                btnDespublicar.setVisibility(View.VISIBLE);
                btnAvaliar.setVisibility(View.GONE);
            } else if (isLastPage){ // Rascunho
                btnPublicar.setVisibility(View.VISIBLE);
                btnDespublicar.setVisibility(View.GONE);
                btnAvaliar.setVisibility(View.GONE);
            } else {
                btnPublicar.setVisibility(View.GONE);
                btnDespublicar.setVisibility(View.GONE);
                btnAvaliar.setVisibility(View.GONE);
            }
        } else {
            // Se não for dono nem mentor, esconde tudo
            btnPublicar.setVisibility(View.GONE);
            btnDespublicar.setVisibility(View.GONE);
            btnAvaliar.setVisibility(View.GONE);
        }
    }

    private void setupButtonClickListeners() {
        btnVoltar.setOnClickListener(v -> {
            if (!isReadOnlyMode) {
                saveAndFinish(true);
            } else {
                finish();
            }
        });
        btnAnterior.setOnClickListener(v -> {
            if (!isReadOnlyMode) {
                saveProgress(() -> viewPager.setCurrentItem(viewPager.getCurrentItem() - 1));
            } else {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });
        btnPublicar.setOnClickListener(v -> verificarPermissaoEPublicar());
        btnDespublicar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Despublicar Ideia")
                    .setMessage("A sua ideia voltará a ser um rascunho privado. Deseja continuar?")
                    .setPositiveButton("Sim, despublicar", (dialog, which) -> despublicarIdeia())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        btnAvaliar.setOnClickListener(v -> {
            if (ideia != null && ideia.getId() != null) {
                Intent intent = new Intent(this, AvaliacaoActivity.class);
                intent.putExtra("ideia_id", ideia.getId());
                avaliacaoLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Erro: Não foi possível carregar os dados da ideia para avaliação.", Toast.LENGTH_SHORT).show();
            }
        });

        btnProximo.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            String etapaKey = etapas.get(currentItem).getKey();
            
            Runnable advanceAction = () -> viewPager.setCurrentItem(currentItem + 1);

            switch (etapaKey) {
                case "INICIO":
                    validateAndAdvanceFromFirstPage();
                    break;
                case "AMBIENTE_CHECK":
                    if (btnProximo.isEnabled()) {
                        advanceAction.run();
                    }
                    break;
                case "STATUS":
                    advanceAction.run();
                    break;
                default:
                    if (validateAndAdvanceFromBlockPage(etapaKey)) {
                        if (!isReadOnlyMode) {
                            saveProgress(advanceAction);
                        } else {
                            advanceAction.run();
                        }
                    }
                    break;
            }
        });
    }

    private void attachIdeiaListener(String ideiaId, boolean openAsReadOnly) {
        if (ideiaListener != null) ideiaListener.remove();
        if (ideiaId == null) return;

        ideiaListener = firestoreHelper.listenToIdeia(ideiaId, new FirestoreHelper.IdeiaUnicaListener() {
            @Override
            public void onIdeiaCarregada(Ideia ideiaCarregada) {
                boolean isFirstLoad = (CanvasIdeiaActivity.this.ideia == null);

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                boolean isOwner = user != null && user.getUid().equals(ideiaCarregada.getOwnerId());
                isReadOnlyMode = openAsReadOnly || (isOwner && !"RASCUNHO".equals(ideiaCarregada.getStatus()));

                CanvasIdeiaActivity.this.ideia = ideiaCarregada;

                if (isFirstLoad) {
                    setupEtapas(ideiaCarregada.getStatus());
                    setupUI();
                    setupButtonClickListeners();
                    showLoading(false);
                } else {
                    notificarFragmentoAtual();
                }
                updateActionButtonsVisibility(viewPager.getCurrentItem());
            }
            @Override
            public void onError(Exception e) {
                showLoading(false);
                Toast.makeText(CanvasIdeiaActivity.this, "Erro ao carregar ideia: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupLocationPermissionLauncher() {
        requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                iniciarProcessoDePublicacao();
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingIndicator.setVisibility(View.VISIBLE);
            contentGroup.setVisibility(View.GONE);
        } else {
            loadingIndicator.setVisibility(View.GONE);
            contentGroup.setVisibility(View.VISIBLE);
        }
    }

    private void setupEtapas(String status) {
        etapas = new ArrayList<>();
        if ("PUBLICADA".equals(status)) {
            etapas.add(new CanvasEtapa("STATUS", "Status da Jornada", "Acompanhe a avaliação da sua ideia.", R.drawable.ic_flag));
        }
        if ("RASCUNHO".equals(status)) {
            etapas.add(new CanvasEtapa("INICIO", "Capa da Ideia", "Dê um nome e uma breve descrição.", R.drawable.ic_lightbulb));
            etapas.add(new CanvasEtapa("AMBIENTE_CHECK", "Check-up de Ambiente", "Prepare o ambiente para a criatividade.", R.drawable.ic_ambient));
        }
        etapas.add(new CanvasEtapa("PROPOSTA_VALOR", "Proposta de Valor", "O que torna sua ideia única?", R.drawable.ic_proposta_valor));
        etapas.add(new CanvasEtapa("SEGMENTO_CLIENTES", "Segmento de Clientes", "Para quem é esta ideia?", R.drawable.ic_segmento_clientes));
        etapas.add(new CanvasEtapa("CANAIS", "Canais", "Como você chegará até seus clientes?", R.drawable.ic_canais));
        etapas.add(new CanvasEtapa("RELACIONAMENTO_CLIENTES", "Relacionamento", "Como você vai interagir?", R.drawable.ic_relacionamento));
        etapas.add(new CanvasEtapa("FONTES_RENDA", "Fontes de Renda", "Como sua ideia vai gerar dinheiro?", R.drawable.ic_fontes_renda));
        etapas.add(new CanvasEtapa("RECURSOS_PRINCIPAIS", "Recursos Principais", "O que é essencial para funcionar?", R.drawable.ic_recursos));
        etapas.add(new CanvasEtapa("ATIVIDADES_CHAVE", "Atividades-Chave", "Quais são as ações mais importantes?", R.drawable.ic_atividades));
        etapas.add(new CanvasEtapa("PARCERIAS_PRINCIPAIS", "Parcerias Principais", "Quem pode te ajudar?", R.drawable.ic_parcerias));
        etapas.add(new CanvasEtapa("ESTRUTURA_CUSTOS", "Estrutura de Custos", "Quais serão seus principais custos?", R.drawable.ic_custos));
        if ("RASCUNHO".equals(status)) {
            etapas.add(new CanvasEtapa("FINAL", "Publicar", "A sua ideia está pronta para descolar.", R.drawable.ic_rocket_launch));
        }
    }

    private void validateAndAdvanceFromFirstPage() {
        if (ideia.getNome() == null || ideia.getNome().trim().isEmpty() || ideia.getDescricao() == null || ideia.getDescricao().trim().isEmpty()) {
            Toast.makeText(this, "Preencha o nome e a descrição da ideia.", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentItem = viewPager.getCurrentItem();
        if (ideia.getId() == null) {
            // ---- NOVA LÓGICA OFFLINE-FIRST ----
            // 1. Gera um ID local para o novo documento.
            String novoIdeiaId = firestoreHelper.getNewIdeiaId();
            ideia.setId(novoIdeiaId);
            ideia.setOwnerId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

            // 2. Salva a ideia em segundo plano. O Firestore gere a fila offline.
            saveProgress(null); // Passamos null para não executar nenhuma ação de UI aqui

            // 3. Anexa o listener e navega imediatamente.
            attachIdeiaListener(novoIdeiaId, false);
            viewPager.setCurrentItem(currentItem + 1);

        } else {
            if (!isReadOnlyMode) {
                saveProgress(() -> viewPager.setCurrentItem(currentItem + 1));
            } else {
                viewPager.setCurrentItem(currentItem + 1);
            }
        }
    }

    private boolean validateAndAdvanceFromBlockPage(String etapaChave) {
        if (ideia.getPostItsPorChave(etapaChave) == null || ideia.getPostItsPorChave(etapaChave).isEmpty()) {
            Toast.makeText(this, "Adicione pelo menos um post-it para avançar.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void verificarPermissaoEPublicar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarProcessoDePublicacao();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private void iniciarProcessoDePublicacao() {
        showLoadingDialog("A obter a sua localização...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            hideLoadingDialog();
            Toast.makeText(this, "Permissão de localização necessária.", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            loadingHandler.postDelayed(() -> {
                if (location != null) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String cidade = addresses.get(0).getLocality();
                            String estado = addresses.get(0).getAdminArea();
                            procurarMentorNaCidade(cidade, estado);
                        } else {
                            publicarIdeiaSemMentor();
                        }
                    } catch (IOException e) {
                        publicarIdeiaSemMentor();
                    }
                } else {
                    publicarIdeiaSemMentor();
                }
            }, 2000);
        });
    }

    private void procurarMentorNaCidade(String cidade, String estado) {
        updateLoadingDialog(String.format("A procurar mentores em %s...", cidade));
        loadingHandler.postDelayed(() -> {
            firestoreHelper.findMentorByCity(cidade,ideia.getOwnerId(), new FirestoreHelper.MentorListener() {
                @Override
                public void onMentorEncontrado(Mentor mentor) {
                    updateLoadingDialog("Mentor encontrado! A publicar a sua ideia...");
                    loadingHandler.postDelayed(() -> publicarIdeiaComMentor(mentor.getId()), 2000);
                }
                @Override
                public void onNenhumMentorEncontrado() {
                    procurarMentorNoEstado(estado);
                }
                @Override
                public void onError(Exception e) {
                    hideLoadingDialog();
                    Toast.makeText(CanvasIdeiaActivity.this, "Erro ao procurar mentor.", Toast.LENGTH_SHORT).show();
                }
            });
        }, 2500);
    }

    private void procurarMentorNoEstado(String estado) {
        updateLoadingDialog("Nenhum mentor na sua cidade. A expandir para " + estado + "...");
        loadingHandler.postDelayed(() -> {
            firestoreHelper.findMentorByState(estado, ideia.getOwnerId(), new FirestoreHelper.MentorListener() {
                @Override
                public void onMentorEncontrado(Mentor mentor) {
                    updateLoadingDialog("Mentor encontrado! A publicar a sua ideia...");
                    loadingHandler.postDelayed(() -> publicarIdeiaComMentor(mentor.getId()), 2000);
                }
                @Override
                public void onNenhumMentorEncontrado() {
                    publicarIdeiaSemMentor();
                }
                @Override
                public void onError(Exception e) {
                    hideLoadingDialog();
                    Toast.makeText(CanvasIdeiaActivity.this, "Erro ao procurar mentor.", Toast.LENGTH_SHORT).show();
                }
            });
        }, 2500);
    }

    private void publicarIdeiaComMentor(String mentorId) {
        firestoreHelper.publicarIdeia(ideia.getId(), mentorId, new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess() {
                hideLoadingDialog();
                Toast.makeText(CanvasIdeiaActivity.this, "Ideia publicada e enviada para um mentor!", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override
            public void onFailure(Exception e) {
                hideLoadingDialog();
                Toast.makeText(CanvasIdeiaActivity.this, "Erro ao publicar ideia.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void publicarIdeiaSemMentor() {
        updateLoadingDialog("Nenhum mentor na sua região. A publicar para avaliação geral...");
        loadingHandler.postDelayed(() -> {
            firestoreHelper.publicarIdeia(ideia.getId(), null, new FirestoreHelper.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    hideLoadingDialog();
                    Toast.makeText(CanvasIdeiaActivity.this, "Ideia publicada para avaliação geral!", Toast.LENGTH_LONG).show();
                    finish();
                }
                @Override
                public void onFailure(Exception e) {
                    hideLoadingDialog();
                    Toast.makeText(CanvasIdeiaActivity.this, "Erro ao publicar ideia.", Toast.LENGTH_SHORT).show();
                }
            });
        }, 2000);
    }

    private void despublicarIdeia() {
        showLoadingDialog("A converter para rascunho...");
        firestoreHelper.unpublishIdeia(ideia.getId(), new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess() {
                hideLoadingDialog();
                Toast.makeText(CanvasIdeiaActivity.this, "Ideia revertida para rascunho! Pode editá-la no seu perfil.", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override
            public void onFailure(Exception e) {
                hideLoadingDialog();
                Toast.makeText(CanvasIdeiaActivity.this, "Erro ao despublicar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_loading, null);
            TextView loadingText = dialogView.findViewById(R.id.loading_text);
            if (loadingText != null) {
                loadingText.setText(message);
            }
            builder.setView(dialogView);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        } else {
            TextView loadingText = loadingDialog.findViewById(R.id.loading_text);
            if (loadingText != null) {
                loadingText.setText(message);
            }
        }
        loadingDialog.show();
    }

    private void updateLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            TextView loadingText = loadingDialog.findViewById(R.id.loading_text);
            if (loadingText != null) {
                loadingText.setText(message);
            }
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onAmbienteIdealDetectado(boolean isIdeal) {
        if (viewPager == null) return;
        int currentItem = viewPager.getCurrentItem();
        if (etapas.size() > currentItem) {
            String etapaKey = etapas.get(currentItem).getKey();
            if ("AMBIENTE_CHECK".equals(etapaKey)) {
                btnProximo.setEnabled(isIdeal);
                btnProximo.setAlpha(isIdeal ? 1.0f : 0.5f);
            }
        }
    }

    @Override
    public void onPularCheck() {
        if (viewPager == null) return;
        int currentItem = viewPager.getCurrentItem();
        if (etapas.size() > currentItem) {
            String etapaKey = etapas.get(currentItem).getKey();
            if ("AMBIENTE_CHECK".equals(etapaKey)) {
                viewPager.setCurrentItem(currentItem + 1);
            }
        }
    }

    private void notificarFragmentoAtual() {
        if (viewPager == null) return;
        Fragment fragmentoAtual = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (fragmentoAtual instanceof CanvasBlockFragment) {
            ((CanvasBlockFragment) fragmentoAtual).atualizarDadosIdeia(this.ideia);
        }
    }

    private void saveProgress(Runnable onComplete) {
        if (ideia != null && ideia.getId() != null) {
            firestoreHelper.updateIdeia(ideia.getId(), ideia, new FirestoreHelper.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CanvasIdeiaActivity.this, "Erro ao salvar progresso.", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }, this);
        } else if (onComplete != null) {
            onComplete.run();
        }
    }

    private void saveAndFinish(boolean shouldFinish) {
        // Verifica se a ideia existe e se o seu status é "RASCUNHO"
        if (ideia != null && ideia.getId() != null && "RASCUNHO".equals(ideia.getStatus())) {

            // Se for um rascunho, salva e mostra a mensagem
            firestoreHelper.updateIdeia(ideia.getId(), ideia, new FirestoreHelper.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CanvasIdeiaActivity.this, "Rascunho salvo com sucesso!", Toast.LENGTH_LONG).show();
                    if (shouldFinish) {
                        finish();
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CanvasIdeiaActivity.this, "Erro ao salvar o rascunho.", Toast.LENGTH_SHORT).show();
                    if (shouldFinish) {
                        finish();
                    }
                }
            }, this);
        } else if (shouldFinish) {
            // Para qualquer outro caso (ideia publicada, mentor a sair, etc.),
            // simplesmente fecha a tela sem salvar ou mostrar nenhuma mensagem.
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ideiaListener != null) {
            ideiaListener.remove();
        }
    }

    @Override
    public void onFieldsChanged(Ideia ideiaAtualizada) {
        if (this.ideia != null) {
            this.ideia.setNome(ideiaAtualizada.getNome());
            this.ideia.setDescricao(ideiaAtualizada.getDescricao());
        }
    }

    @Override
    public void onPostItAdded() {
        // A UI atualiza-se via listener, não é preciso fazer nada aqui.
    }
}