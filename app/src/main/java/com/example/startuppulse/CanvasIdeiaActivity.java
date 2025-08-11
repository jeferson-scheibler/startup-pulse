package com.example.startuppulse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.startuppulse.common.Result;
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

public class CanvasIdeiaActivity extends AppCompatActivity
        implements CanvasInicioFragment.InicioStateListener,
        CanvasBlockFragment.CanvasBlockListener,
        AmbienteCheckFragment.AmbienteCheckListener {

    private ViewPager2 viewPager;
    private CanvasPagerAdapter adapter;
    private FirestoreHelper firestoreHelper;
    private Ideia ideia;
    private List<CanvasEtapa> etapas;
    private ListenerRegistration ideiaListener;

    private ProgressBar loadingIndicator;
    private Group contentGroup;
    private MaterialButton btnProximo, btnAnterior, btnPublicar, btnDespublicar, btnAvaliar;
    private ImageView btnVoltar;

    private AlertDialog loadingDialog;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private ActivityResultLauncher<Intent> avaliacaoLauncher;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isReadOnlyMode = false;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas_ideia);

        firestoreHelper = new FirestoreHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadingIndicator = findViewById(R.id.loading_indicator);
        contentGroup     = findViewById(R.id.content_group);

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
            return;
        }

        avaliacaoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && ideia != null && ideia.getId() != null) {
                        Toast.makeText(this, "Status da ideia atualizado!", Toast.LENGTH_SHORT).show();
                        attachIdeiaListener(ideia.getId(), isReadOnlyMode); // força refresh
                    }
                }
        );
    }

    private void setupForNewIdeia() {
        showLoading(false);
        this.isReadOnlyMode = false;
        setupEtapas("RASCUNHO");
        setupUI();
        setupButtonClickListeners();
    }

    private void setupForExistingIdeia(String ideiaId, boolean openAsReadOnly) {
        showLoading(true);
        attachIdeiaListener(ideiaId, openAsReadOnly);
    }

    private void setupUI() {
        viewPager       = findViewById(R.id.view_pager_canvas);
        btnProximo      = findViewById(R.id.btn_proximo);
        btnAnterior     = findViewById(R.id.btn_anterior);
        btnVoltar       = findViewById(R.id.btn_voltar);
        btnPublicar     = findViewById(R.id.btn_publicar_ideia);
        btnDespublicar  = findViewById(R.id.btn_despublicar_ideia);
        btnAvaliar      = findViewById(R.id.btn_avaliar_ideia);
        btnAvaliar.setEnabled(false);

        adapter = new CanvasPagerAdapter(this, ideia, etapas, isReadOnlyMode);
        viewPager.setAdapter(adapter);

        // Swipe do ViewPager habilitado (leitura e edição). O gate da etapa fica no botão "Próximo".
        viewPager.setUserInputEnabled(true);

        setupTabs();
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (btnAnterior == null) return;
                updateActionButtonsVisibility(position);
            }
        });
    }

    private void updateActionButtonsVisibility(int position) {
        boolean isLastPage = (position == adapter.getItemCount() - 1);
        FirebaseUser user  = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner  = user != null && ideia != null && user.getUid().equals(ideia.getOwnerId());
        boolean isMentor = user != null && ideia != null && user.getUid().equals(ideia.getMentorId());

        btnAnterior.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);

        btnProximo.setVisibility(View.VISIBLE);
        btnProximo.setEnabled(!isLastPage);
        btnProximo.setAlpha(isLastPage ? 0.5f : 1.0f);

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
            } else if (isLastPage) {
                btnPublicar.setVisibility(View.VISIBLE);
                btnDespublicar.setVisibility(View.GONE);
                btnAvaliar.setVisibility(View.GONE);
            } else {
                btnPublicar.setVisibility(View.GONE);
                btnDespublicar.setVisibility(View.GONE);
                btnAvaliar.setVisibility(View.GONE);
            }
        } else {
            btnPublicar.setVisibility(View.GONE);
            btnDespublicar.setVisibility(View.GONE);
            btnAvaliar.setVisibility(View.GONE);
        }
    }

    private void setupButtonClickListeners() {
        btnVoltar.setOnClickListener(v -> {
            if (!isReadOnlyMode) saveAndFinish(true);
            else finish();
        });

        btnAnterior.setOnClickListener(v -> {
            if (!isReadOnlyMode) {
                saveProgress(() -> viewPager.setCurrentItem(Math.max(0, viewPager.getCurrentItem() - 1)));
            } else {
                viewPager.setCurrentItem(Math.max(0, viewPager.getCurrentItem() - 1));
            }
        });

        btnPublicar.setOnClickListener(v -> verificarPermissaoEPublicar());

        btnDespublicar.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Despublicar Ideia")
                        .setMessage("A sua ideia voltará a ser um rascunho privado. Deseja continuar?")
                        .setPositiveButton("Sim, despublicar", (dialog, which) -> despublicarIdeia())
                        .setNegativeButton("Cancelar", null)
                        .show()
        );

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

            Runnable advance = () -> viewPager.setCurrentItem(Math.min(adapter.getItemCount() - 1, currentItem + 1));

            switch (etapaKey) {
                case "INICIO":
                    validateAndAdvanceFromFirstPage();
                    break;
                case "AMBIENTE_CHECK":
                    if (btnProximo.isEnabled()) advance.run();
                    break;
                case "STATUS":
                    advance.run();
                    break;
                default:
                    if (validateAndAdvanceFromBlockPage(etapaKey)) {
                        if (!isReadOnlyMode) saveProgress(advance);
                        else advance.run();
                    }
            }
        });
    }

    private void attachIdeiaListener(String ideiaId, boolean openAsReadOnly) {
        if (ideiaListener != null) ideiaListener.remove();
        if (ideiaId == null) return;

        ideiaListener = firestoreHelper.listenToIdeia(ideiaId, r -> {
            if (!r.isOk()) {
                showLoading(false);
                Toast.makeText(this, "Erro ao carregar ideia: " + r.error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Ideia loaded = r.data; // pode ser null se removida
            if (loaded == null) {
                showLoading(false);
                Toast.makeText(this, "Esta ideia não existe mais.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            boolean firstLoad = (CanvasIdeiaActivity.this.ideia == null);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            boolean isOwner = user != null && user.getUid().equals(loaded.getOwnerId());

            // read-only se veio assim OU se o dono abriu algo que não é rascunho
            isReadOnlyMode = openAsReadOnly || (isOwner && !"RASCUNHO".equals(loaded.getStatus()));

            CanvasIdeiaActivity.this.ideia = loaded;

            if (firstLoad) {
                setupEtapas(loaded.getStatus());
                setupUI();
                setupButtonClickListeners();
                showLoading(false);
            } else {
                notificarFragmentoAtual();
            }
            updateActionButtonsVisibility(viewPager.getCurrentItem());
        });
    }

    private void setupLocationPermissionLauncher() {
        requestLocationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) iniciarProcessoDePublicacao();
                    else Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentGroup.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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
        if (ideia == null ||
                ideia.getNome() == null || ideia.getNome().trim().isEmpty() ||
                ideia.getDescricao() == null || ideia.getDescricao().trim().isEmpty()) {
            Toast.makeText(this, "Preencha o nome e a descrição da ideia.", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentItem = viewPager.getCurrentItem();
        if (ideia.getId() == null) {
            String novoId = firestoreHelper.getNewIdeiaId();
            ideia.setId(novoId);
            ideia.setOwnerId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

            saveProgress(null);            // deixa o Firestore sincronizar (offline-first)
            attachIdeiaListener(novoId, false);
            viewPager.setCurrentItem(currentItem + 1);
        } else {
            if (!isReadOnlyMode) saveProgress(() -> viewPager.setCurrentItem(currentItem + 1));
            else viewPager.setCurrentItem(currentItem + 1);
        }
    }

    private boolean validateAndAdvanceFromBlockPage(String etapaChave) {
        return !(ideia == null ||
                ideia.getPostItsPorChave(etapaChave) == null ||
                ideia.getPostItsPorChave(etapaChave).isEmpty())
                || showAndReturnFalse("Adicione pelo menos um post-it para avançar.");
    }

    private boolean showAndReturnFalse(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        return false;
    }

    private void verificarPermissaoEPublicar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarProcessoDePublicacao();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private void iniciarProcessoDePublicacao() {
        showLoadingDialog("A obter a sua localização...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
            firestoreHelper.findMentorByCity(cidade, ideia.getOwnerId(), r -> {
                if (!r.isOk()) {
                    hideLoadingDialog();
                    Toast.makeText(this, "Erro ao procurar mentor.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Mentor mentor = r.data;
                if (mentor != null) {
                    updateLoadingDialog("Mentor encontrado! A publicar a sua ideia...");
                    loadingHandler.postDelayed(() -> publicarIdeiaComMentor(mentor.getId()), 2000);
                } else {
                    procurarMentorNoEstado(estado);
                }
            });
        }, 2500);
    }

    private void procurarMentorNoEstado(String estado) {
        updateLoadingDialog("Nenhum mentor na sua cidade. A expandir para " + estado + "...");
        loadingHandler.postDelayed(() -> {
            firestoreHelper.findMentorByState(estado, ideia.getOwnerId(), r -> {
                if (!r.isOk()) {
                    hideLoadingDialog();
                    Toast.makeText(this, "Erro ao procurar mentor.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Mentor mentor = r.data;
                if (mentor != null) {
                    updateLoadingDialog("Mentor encontrado! A publicar a sua ideia...");
                    loadingHandler.postDelayed(() -> publicarIdeiaComMentor(mentor.getId()), 2000);
                } else {
                    publicarIdeiaSemMentor();
                }
            });
        }, 2500);
    }

    private void publicarIdeiaComMentor(String mentorId) {
        firestoreHelper.publicarIdeia(ideia.getId(), mentorId, r -> {
            hideLoadingDialog();
            if (r.isOk()) {
                Toast.makeText(this, "Ideia publicada e enviada para um mentor!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Erro ao publicar ideia.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void publicarIdeiaSemMentor() {
        updateLoadingDialog("Nenhum mentor na sua região. A publicar para avaliação geral...");
        loadingHandler.postDelayed(() ->
                        firestoreHelper.publicarIdeia(ideia.getId(), null, r -> {
                            hideLoadingDialog();
                            if (r.isOk()) {
                                Toast.makeText(this, "Ideia publicada para avaliação geral!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Erro ao publicar ideia.", Toast.LENGTH_SHORT).show();
                            }
                        })
                , 2000);
    }

    private void despublicarIdeia() {
        showLoadingDialog("A converter para rascunho...");
        firestoreHelper.unpublishIdeia(ideia.getId(), r -> {
            hideLoadingDialog();
            if (r.isOk()) {
                Toast.makeText(this, "Ideia revertida para rascunho! Pode editá-la no seu perfil.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Erro ao despublicar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            TextView loadingText = dialogView.findViewById(R.id.loading_text);
            if (loadingText != null) loadingText.setText(message);
            builder.setView(dialogView);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        } else {
            TextView loadingText = loadingDialog.findViewById(R.id.loading_text);
            if (loadingText != null) loadingText.setText(message);
        }
        loadingDialog.show();
    }

    private void updateLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            TextView loadingText = loadingDialog.findViewById(R.id.loading_text);
            if (loadingText != null) loadingText.setText(message);
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
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
        if (etapas.size() > currentItem && "AMBIENTE_CHECK".equals(etapas.get(currentItem).getKey())) {
            viewPager.setCurrentItem(currentItem + 1);
        }
    }

    private void notificarFragmentoAtual() {
        if (viewPager == null) return;
        Fragment frag = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (frag instanceof CanvasBlockFragment) {
            ((CanvasBlockFragment) frag).atualizarDadosIdeia(this.ideia);
        }
    }

    private void saveProgress(@Nullable Runnable onComplete) {
        if (ideia != null && ideia.getId() != null) {
            firestoreHelper.updateIdeia(ideia.getId(), ideia, r -> {
                if (!r.isOk()) {
                    Toast.makeText(this, "Erro ao salvar progresso.", Toast.LENGTH_SHORT).show();
                }
                if (onComplete != null) onComplete.run();
            });
        } else if (onComplete != null) {
            onComplete.run();
        }
    }

    private void saveAndFinish(boolean shouldFinish) {
        if (ideia != null && ideia.getId() != null && "RASCUNHO".equals(ideia.getStatus())) {
            firestoreHelper.updateIdeia(ideia.getId(), ideia, r -> {
                if (r.isOk()) {
                    Toast.makeText(this, "Rascunho salvo com sucesso!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Erro ao salvar o rascunho.", Toast.LENGTH_SHORT).show();
                }
                if (shouldFinish) finish();
            });
        } else if (shouldFinish) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ideiaListener != null) ideiaListener.remove();
    }

    @Override
    public void onFieldsChanged(Ideia ideiaAtualizada) {
        if (this.ideia != null && ideiaAtualizada != null) {
            this.ideia.setNome(ideiaAtualizada.getNome());
            this.ideia.setDescricao(ideiaAtualizada.getDescricao());
        }
    }

    @Override
    public void onPostItAdded() {
        // atualizações chegam pelo listener do Firestore
    }
}
