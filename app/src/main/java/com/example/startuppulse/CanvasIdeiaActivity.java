package com.example.startuppulse;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.ActivityCanvasIdeiaBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Activity principal para criar, editar e visualizar uma Ideia.
 * Utiliza um ViewPager2 para navegar entre as diferentes etapas do canvas.
 */
public class CanvasIdeiaActivity extends AppCompatActivity implements
        CanvasInicioFragment.InicioStateListener,
        CanvasBlockFragment.CanvasBlockListener,
        AmbienteCheckFragment.AmbienteCheckListener,
        CanvasEquipeFragment.EquipeListener {

    // --- Propriedades ---
    private ActivityCanvasIdeiaBinding binding;
    private FirestoreHelper firestoreHelper;
    private Ideia ideia;
    private List<CanvasEtapa> etapas;
    private ListenerRegistration ideiaListener;
    private boolean isReadOnly = false;

    private AlertDialog loadingDialog;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private ActivityResultLauncher<Intent> avaliacaoLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());

    private static final String TAG_MATCHMAKING = "MentorMatchmaking";
    private static final String PREFS_NAME = "StartupPulsePrefs";
    private static final String KEY_AMBIENTE_CHECK_CONCLUIDO = "ambienteCheckConcluido";

    public Ideia getIdeiaAtual() {
        return this.ideia;
    }

    // --- Ciclo de Vida da Activity ---
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Infla o layout usando ViewBinding, a forma profissional e segura
        binding = ActivityCanvasIdeiaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa componentes
        initialize();

        // Determina se a activity deve iniciar com uma nova ideia ou uma existente
        handleIntent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove o listener do Firestore para evitar memory leaks
        if (ideiaListener != null) {
            ideiaListener.remove();
        }
    }


    // --- Configuração Inicial ---

    /**
     * Inicializa os componentes principais da Activity.
     */
    private void initialize() {
        firestoreHelper = new FirestoreHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        isReadOnly = getIntent().getBooleanExtra("isReadOnly", false);
        setupLocationPermissionLauncher();
        setupAvaliacaoLauncher();
    }

    /**
     * Processa o Intent de entrada para carregar a ideia correta.
     */
    private void handleIntent() {
        ideia = (Ideia) getIntent().getSerializableExtra("ideia");
        String ideiaId = getIntent().getStringExtra("ideia_id");

        if (ideia != null && ideia.getId() == null) {
            setupForNewIdeia();
        } else if (ideiaId != null) {
            setupForExistingIdeia(ideiaId, isReadOnly);
        } else {
            Toast.makeText(this, "Erro: Nenhuma ideia ou ID fornecido.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Configura a UI para uma nova ideia (que é sempre editável).
     */
    private void setupForNewIdeia() {
        this.isReadOnly = false;
        showLoading(false);
        verificarEExecutarAmbienteCheck();
        setupEtapas(ideia.getStatus());
        setupUI();
        setupListeners();
    }

    /**
     * Inicia o listener do Firestore para carregar e observar uma ideia existente.
     */
    private void setupForExistingIdeia(String ideiaId, boolean openAsReadOnly) {
        showLoading(true);
        attachIdeiaListener(ideiaId, openAsReadOnly);
    }


    // --- Configuração da UI e Listeners ---

    /**
     * Configura o ViewPager, Adapter e Tabs.
     */
    private void setupUI() {
        CanvasPagerAdapter adapter = new CanvasPagerAdapter(this, etapas, ideia, isReadOnly);
        binding.viewPagerCanvas.setAdapter(adapter);
        binding.viewPagerCanvas.setUserInputEnabled(true);
        setupTabs();
    }

    /**
     * Conecta o TabLayout ao ViewPager2 para exibir as abas de navegação.
     */
    private void setupTabs() {
        new TabLayoutMediator(binding.tabLayout, binding.viewPagerCanvas, (tab, position) -> {
            // Configuração do ícone e texto da tab
            tab.setText(etapas.get(position).getTitulo());
        }).attach();
    }

    /**
     * Configura todos os listeners de clique para os botões da Activity.
     */
    private void setupListeners() {
        binding.viewPagerCanvas.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateActionButtonsVisibility(position);
            }
        });

        binding.btnVoltar.setOnClickListener(v -> {
            if (!isReadOnly) saveAndFinish(true);
            else finish();
        });

        binding.btnAnterior.setOnClickListener(v -> {
            int previousItem = Math.max(0, binding.viewPagerCanvas.getCurrentItem() - 1);
            if (!isReadOnly) {
                saveProgress(() -> binding.viewPagerCanvas.setCurrentItem(previousItem));
            } else {
                binding.viewPagerCanvas.setCurrentItem(previousItem);
            }
        });

        binding.btnProximo.setOnClickListener(v -> handleNextButtonClick());
        binding.btnPublicarIdeia.setOnClickListener(v -> verificarPermissaoEPublicar());

        binding.btnDespublicarIdeia.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Despublicar Ideia")
                        .setMessage("A sua ideia voltará a ser um rascunho privado. Deseja continuar?")
                        .setPositiveButton("Sim, despublicar", (dialog, which) -> despublicarIdeia())
                        .setNegativeButton("Cancelar", null)
                        .show()
        );

        binding.btnAvaliarIdeia.setOnClickListener(v -> {
            if (ideia != null && ideia.getId() != null) {
                Intent intent = new Intent(this, AvaliacaoActivity.class);
                intent.putExtra("ideia_id", ideia.getId());
                avaliacaoLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Erro ao carregar dados da ideia.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Lógica centralizada para o clique no botão "Próximo".
     */
    private void handleNextButtonClick() {
        int currentItem = binding.viewPagerCanvas.getCurrentItem();
        CanvasPagerAdapter adapter = (CanvasPagerAdapter) binding.viewPagerCanvas.getAdapter();
        if (adapter == null || etapas.size() <= currentItem) return;

        String etapaKey = etapas.get(currentItem).getChave();
        Runnable advancePage = () -> binding.viewPagerCanvas.setCurrentItem(Math.min(adapter.getItemCount() - 1, currentItem + 1));

        switch (etapaKey) {
            case CanvasEtapa.CHAVE_INICIO:
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + currentItem);
                if (currentFragment instanceof CanvasInicioFragment) {
                    CanvasInicioFragment inicioFragment = (CanvasInicioFragment) currentFragment;
                    if (inicioFragment.validateSelectionOrToast()) {
                        inicioFragment.persistAreasToIdeia();
                        validateAndAdvanceFromFirstPage();
                    }
                }
                break;
            case CanvasEtapa.CHAVE_AMBIENTE_CHECK:
                if (binding.btnProximo.isEnabled()) {
                    advancePage.run();
                }
                break;
            case CanvasEtapa.CHAVE_STATUS:
            case CanvasEtapa.CHAVE_EQUIPE:
                advancePage.run();
                break;
            default:
                if (validateAndAdvanceFromBlockPage(etapaKey)) {
                    if (!isReadOnly) {
                        saveProgress(advancePage);
                    } else {
                        advancePage.run();
                    }
                }
        }
    }
    // --- Lógica de Negócio e de UI ---

    /**
     * Atualiza a visibilidade e o estado dos botões de ação com base na página atual.
     */
    private void updateActionButtonsVisibility(int position) {
        CanvasPagerAdapter adapter = (CanvasPagerAdapter) binding.viewPagerCanvas.getAdapter();
        if (adapter == null || ideia == null || etapas == null || etapas.isEmpty()) return;

        String etapaKey = etapas.get(position).getChave();
        boolean isLastPage = (position == adapter.getItemCount() - 1);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = user != null && user.getUid().equals(ideia.getOwnerId());
        boolean isMentor = user != null && ideia != null && user.getUid().equals(ideia.getMentorId());

        // --- Lógica dos Botões de Navegação (Anterior/Próximo) ---
        binding.btnAnterior.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);

        // O botão "Próximo" deve desaparecer na última página E na página de status
        if (isLastPage) {
            binding.btnProximo.setVisibility(View.INVISIBLE);
        } else {
            binding.btnProximo.setVisibility(View.VISIBLE);
        }

        // Lógica específica para a etapa de "Ambiente Check"
        if (!isReadOnly && etapaKey.equals(CanvasEtapa.CHAVE_AMBIENTE_CHECK)) {
            binding.btnProximo.setEnabled(false);
            binding.btnProximo.setAlpha(0.5f);
        } else {
            binding.btnProximo.setEnabled(true);
            binding.btnProximo.setAlpha(1.0f);
        }

        // --- Lógica dos Botões de Ação (Publicar/Despublicar/Avaliar) ---
        // Começamos por esconder todos para evitar estados incorretos
        binding.btnPublicarIdeia.setVisibility(View.GONE);
        binding.btnDespublicarIdeia.setVisibility(View.GONE);
        binding.btnAvaliarIdeia.setVisibility(View.GONE);

        // Agora, mostramos o botão correto com base no contexto
        if (isOwner) {
            if (isLastPage && ideia.getStatus() == Ideia.Status.RASCUNHO && !isReadOnly) {
                // Se for a última página de um RASCUNHO e editável, mostra o botão "Publicar".
                binding.btnPublicarIdeia.setVisibility(View.VISIBLE);
            } else if (ideia.getStatus() == Ideia.Status.EM_AVALIACAO){
                binding.btnPublicarIdeia.setVisibility(View.INVISIBLE);
                binding.btnDespublicarIdeia.setVisibility(View.VISIBLE);
            }
        } else if (isMentor) {
            if (etapaKey.equals(CanvasEtapa.CHAVE_STATUS) && ideia.getStatus() != Ideia.Status.RASCUNHO) {
                binding.btnAvaliarIdeia.setVisibility(View.VISIBLE);
            }
        }
    }

    // --- LÓGICA DE DESACOPLAMENTO DO AMBIENTE CHECK ---
    private void verificarEExecutarAmbienteCheck() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean ambienteCheckConcluido = prefs.getBoolean(KEY_AMBIENTE_CHECK_CONCLUIDO, false);

        if (!ambienteCheckConcluido) {
            // Se nunca foi feito, mostra o fragmento como um diálogo
            AmbienteCheckFragment ambienteCheckFragment = AmbienteCheckFragment.newInstance();
            ambienteCheckFragment.show(getSupportFragmentManager(), "AmbienteCheckFragmentDialog");

            // Marca como concluído para não mostrar novamente
            prefs.edit().putBoolean(KEY_AMBIENTE_CHECK_CONCLUIDO, true).apply();
        }
    }

    // --- LÓGICA DO NOVO FLUXO EM FASES ---

    private void setupEtapas(Ideia.Status status) {
        etapas = new ArrayList<>();

        switch (status) {
            case RASCUNHO:
                // Fase 1: Ideação
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_INICIO, "Capa", R.drawable.ic_lightbulb));
                // Adiciona os 9 blocos do canvas
                adicionarBlocosCanvas();
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Publicar", R.drawable.ic_rocket_launch));
                break;

            case EM_AVALIACAO:
                // Fase 2: Validação
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                break;

            case AVALIADA_APROVADA:
                // Fase 3: Tração e Crescimento (Aprovada)
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                // Adiciona os 9 blocos do canvas para consulta/edição
                adicionarBlocosCanvas();
                // DESBLOQUEIA AS NOVAS ETAPAS
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_EQUIPE, "Equipa", R.drawable.ic_person));
                // TODO: Adicionar etapa de Métricas e Pitch Deck aqui no futuro
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Reavaliar", R.drawable.ic_rocket_launch));
                break;

            case AVALIADA_REPROVADA:
                // Fase 3: Tração e Crescimento (Reprovada)
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                // Adiciona os 9 blocos para que o utilizador possa corrigir
                adicionarBlocosCanvas();
                etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Reavaliar", R.drawable.ic_rocket_launch));
                break;
        }
    }

    /**
     * Metodo auxiliar para adicionar os 9 blocos do Business Model Canvas à lista de etapas.
     */
    private void adicionarBlocosCanvas() {
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_PROPOSTA_VALOR, "Proposta de Valor", "O que torna sua ideia única?", R.drawable.ic_proposta_valor));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_SEGMENTO_CLIENTES, "Segmento de Clientes", "Para quem é esta ideia?", R.drawable.ic_segmento_clientes));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_CANAIS, "Canais", "Como você chegará até seus clientes?", R.drawable.ic_canais));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES, "Relacionamento", "Como você vai interagir?", R.drawable.ic_relacionamento));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FONTES_RENDA, "Fontes de Renda", "Como sua ideia vai gerar dinheiro?", R.drawable.ic_fontes_renda));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS, "Recursos Principais", "O que é essencial para funcionar?", R.drawable.ic_recursos));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_ATIVIDADES_CHAVE, "Atividades-Chave", "Quais são as ações mais importantes?", R.drawable.ic_atividades));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS, "Parcerias Principais", "Quem pode te ajudar?", R.drawable.ic_parcerias));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS, "Estrutura de Custos", "Quais serão seus principais custos?", R.drawable.ic_custos));
    }
    private void showLoading(boolean isLoading) {
        binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.contentGroup.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    // --- Firestore e Lógica de Dados ---
    /**
     * Anexa um listener ao documento da ideia no Firestore para atualizações em tempo real.
     */
    private void attachIdeiaListener(String ideiaId, boolean openAsReadOnly) {
        if (ideiaListener != null) ideiaListener.remove();
        if (ideiaId == null) return;

        ideiaListener = firestoreHelper.listenToIdeia(ideiaId, r -> {
            if (!r.isOk() || r.data == null) {
                handleIdeiaLoadError((Exception) r.error);
                return;
            }

            boolean isFirstLoad = (this.ideia == null);
            if (isFirstLoad) {
                this.ideia = r.data;
            } else {
                Ideia ideiaAtualizadaDoFirestore = r.data;
                this.ideia.setPostIts(ideiaAtualizadaDoFirestore.getPostIts());
                this.ideia.setStatus(ideiaAtualizadaDoFirestore.getStatus());
            }

            determineReadOnlyState(openAsReadOnly);

            if (isFirstLoad) {
                setupEtapas(this.ideia.getStatus());
                setupUI();
                setupListeners();
                showLoading(false);
            } else {
                notificarFragmentoAtual();
            }
            updateActionButtonsVisibility(binding.viewPagerCanvas.getCurrentItem());
        });
    }

    private void handleIdeiaLoadError(Exception error) {
        showLoading(false);
        String msg = (error != null) ? "Erro: " + error.getMessage() : "Esta ideia não existe mais.";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Determina o estado final de `isReadOnly` com base na intenção de abertura e no status da ideia.
     */
    private void determineReadOnlyState(boolean openAsReadOnly) {
        if (openAsReadOnly) {
            this.isReadOnly = true;
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = user != null && user.getUid().equals(ideia.getOwnerId());

        // Se o utilizador for o dono, a ideia só é editável se for um RASCUNHO.
        // Qualquer outro estado (EM_AVALIACAO, etc.) é apenas para leitura.
        if (isOwner) {
            this.isReadOnly = (ideia.getStatus() != Ideia.Status.RASCUNHO);
        } else {
            // Se não for o dono, é sempre apenas para leitura.
            this.isReadOnly = true;
        }
    }

    private void saveProgress(@Nullable Runnable onComplete) {
        if (!isReadOnly && ideia != null && ideia.getId() != null) {
            firestoreHelper.updateIdeia(ideia, r -> {
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
        if (!isReadOnly && ideia != null && ideia.getId() != null && "RASCUNHO".equals(ideia.getStatus())) {
            firestoreHelper.updateIdeia(ideia, r -> {
                if (r.isOk()) {
                    Toast.makeText(this, "Rascunho salvo!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao salvar rascunho.", Toast.LENGTH_SHORT).show();
                }
                if (shouldFinish) finish();
            });
        } else if (shouldFinish) {
            finish();
        }
    }

    // --- Validações e Ações das Etapas ---

    private void validateAndAdvanceFromFirstPage() {
        if (ideia == null || ideia.getNome() == null || ideia.getNome().trim().isEmpty() ||
                ideia.getDescricao() == null || ideia.getDescricao().trim().isEmpty()) {
            Toast.makeText(this, "Preencha o nome e a descrição da ideia.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ideia.getId() == null) {
            String novoId = firestoreHelper.getNewIdeiaId();
            ideia.setId(novoId);
            ideia.setOwnerId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
            saveProgress(() -> {
                attachIdeiaListener(novoId, false);
                binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1, false);
            });
        } else {
            if (!isReadOnly) saveProgress(() -> binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1));
            else binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1);
        }
    }

    private boolean validateAndAdvanceFromBlockPage(String etapaChave) {
        if (isReadOnly || !ideia.getPostItsPorChave(etapaChave).isEmpty()) {
            return true;
        }
        Toast.makeText(this, "Adicione pelo menos um post-it para avançar.", Toast.LENGTH_SHORT).show();
        return false;
    }


    // --- Lógica de Publicação ---

    private void verificarPermissaoEPublicar() {
        // Verifica se a permissão de localização fina já foi concedida
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarProcessoDePublicacao();
        } else {
            // Se não, solicita a permissão. O resultado é tratado pelo launcher.
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void iniciarProcessoDePublicacao() {
        // 1. Verifica se o GPS está ativo
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar o mentor ideal, precisamos da sua localização. Por favor, ative o GPS.")
                    .setPositiveButton("Ativar GPS", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", (dialog, which) -> Toast.makeText(this, "Publicação cancelada.", Toast.LENGTH_SHORT).show())
                    .show();
            return;
        }

        // 2. Tenta obter a localização atual
        showLoadingDialog("A obter a sua localização...");
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, this::iniciarMatchmakingDeMentor)
                .addOnFailureListener(this, e -> {
                    Log.w(TAG_MATCHMAKING, "Falha ao obter localização. Tentando matchmaking sem ela.", e);
                    // Mesmo com falha, continua o processo. O matchmaking saberá lidar com a localização nula.
                    iniciarMatchmakingDeMentor(null);
                });
    }

    private void iniciarMatchmakingDeMentor(@Nullable Location localizacaoUsuario) {
        updateLoadingDialog("A procurar o mentor ideal...");
        Log.d(TAG_MATCHMAKING, "--- INICIANDO MATCHMAKING ---");

        final List<String> areasDaIdeia = (ideia != null) ? ideia.getAreasNecessarias() : new ArrayList<>();
        final String ownerId = (ideia != null) ? ideia.getOwnerId() : null;

        Log.d(TAG_MATCHMAKING, "Áreas da ideia: " + areasDaIdeia);
        if (localizacaoUsuario != null) {
            Log.d(TAG_MATCHMAKING, "Localização: Lat=" + localizacaoUsuario.getLatitude() + ", Lng=" + localizacaoUsuario.getLongitude());
        } else {
            Log.w(TAG_MATCHMAKING, "Localização não disponível para este matchmaking.");
        }

        // --- ETAPA 1: BUSCA COMBINADA (ÁREA + PROXIMIDADE) ---
        if (areasDaIdeia != null && !areasDaIdeia.isEmpty()) {
            Log.d(TAG_MATCHMAKING, "Etapa 1: Buscando mentores por área de afinidade.");
            firestoreHelper.findMentoresByAreas(areasDaIdeia, ownerId, r -> {
                if (r.isOk() && r.data != null && !r.data.isEmpty()) {
                    Log.d(TAG_MATCHMAKING, "Sucesso Etapa 1: " + r.data.size() + " mentores encontrados. Ordenando...");
                    List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(r.data, areasDaIdeia, localizacaoUsuario);

                    // --- DIAGNÓSTICO E CORREÇÃO ---
                    if (ordenados != null && !ordenados.isEmpty()) {
                        Mentor mentorEscolhido = ordenados.get(0);
                        String mentorId = (mentorEscolhido != null) ? mentorEscolhido.getId() : null;

                        Log.d(TAG_MATCHMAKING, "Mentor no topo da lista: " + ((mentorEscolhido != null) ? mentorEscolhido.toString() : "objeto nulo"));

                        if (mentorId != null && !mentorId.isEmpty()) {
                            Log.i(TAG_MATCHMAKING, "MENTOR ESCOLHIDO COM SUCESSO. ID: " + mentorId);
                            if (this.ideia != null) {
                                this.ideia.setMatchmakingLog("Mentor encontrado por afinidade de área e proximidade.");
                            }
                            publicarIdeiaComMentor(mentorId);
                        } else {
                            // O mentor existe mas o ID é nulo. Isso indica um problema no modelo Mentor ou na deserialização.
                            Log.e(TAG_MATCHMAKING, "FALHA CRÍTICA: O mentor escolhido está com ID nulo. Verifique o modelo 'Mentor.java' e o Firestore.");
                            // Como fallback, tentamos o próximo da lista antes de desistir.
                            if (ordenados.size() > 1) {
                                Log.d(TAG_MATCHMAKING, "Tentando o segundo mentor da lista como fallback...");
                                Mentor segundoMentor = ordenados.get(1);
                                String segundoMentorId = (segundoMentor != null) ? segundoMentor.getId() : null;
                                if (segundoMentorId != null && !segundoMentorId.isEmpty()) {
                                    Log.i(TAG_MATCHMAKING, "MENTOR (fallback da lista) ESCOLHIDO COM SUCESSO. ID: " + segundoMentorId);
                                    if (this.ideia != null) {
                                        this.ideia.setMatchmakingLog("Mentor encontrado por afinidade de área e proximidade (fallback da lista).");
                                    }
                                    publicarIdeiaComMentor(segundoMentorId);
                                    return; // Evita continuar
                                }
                            }
                            // Se mesmo o segundo falhar, ou não houver segundo, publica sem mentor.
                            publicarIdeiaSemMentor();
                        }
                    } else {
                        Log.w(TAG_MATCHMAKING, "A ordenação retornou uma lista nula ou vazia. Indo para fallback.");
                        procurarMentorApenasPorProximidade(localizacaoUsuario, ownerId);
                    }

                } else {
                    // Se falhar, vai para o fallback (Etapa 2)
                    Log.d(TAG_MATCHMAKING, "Nenhum mentor encontrado por área. Indo para fallback de proximidade.");
                    procurarMentorApenasPorProximidade(localizacaoUsuario, ownerId);
                }
            });
        } else {
            // Se a ideia não tem áreas, vai direto para o fallback (Etapa 2)
            Log.d(TAG_MATCHMAKING, "Nenhuma área definida na ideia. Indo direto para fallback de proximidade.");
            procurarMentorApenasPorProximidade(localizacaoUsuario, ownerId);
        }
    }


    /**
     * ETAPA 2 (FALLBACK): Busca um mentor apenas por proximidade (cidade, depois estado).
     */
    private void procurarMentorApenasPorProximidade(@Nullable Location localizacaoUsuario, @Nullable String ownerId) {
        updateLoadingDialog("Nenhum mentor por área... procurando por proximidade...");
        Log.d(TAG_MATCHMAKING, "Etapa 2 (Fallback): Buscando por proximidade.");

        if (localizacaoUsuario == null) {
            Log.w(TAG_MATCHMAKING, "Localização nula. Impossível buscar por proximidade. Publicando sem mentor.");
            publicarIdeiaSemMentor();
            return;
        }

        final String cidade = getCidadeFromLocation(localizacaoUsuario);

        if (cidade != null && !cidade.isEmpty()) {
            Log.d(TAG_MATCHMAKING, "Fallback: Tentando encontrar na cidade: " + cidade);
            firestoreHelper.findMentoresByCity(cidade, ownerId, rCity -> {
                if (rCity.isOk() && rCity.data != null && !rCity.data.isEmpty()) {
                    Log.d(TAG_MATCHMAKING, "Sucesso Fallback: " + rCity.data.size() + " mentores na cidade. Ordenando...");
                    List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(rCity.data, new ArrayList<>(), localizacaoUsuario);

                    // --- CORREÇÃO AQUI ---
                    if (this.ideia != null) {
                        this.ideia.setMatchmakingLog("Mentor encontrado por proximidade na cidade (fallback).");
                    }

                    publicarIdeiaComMentor(ordenados.get(0).getId());
                } else {
                    procurarMentorNoEstadoFallback(localizacaoUsuario, ownerId);
                }
            });
        } else {
            procurarMentorNoEstadoFallback(localizacaoUsuario, ownerId);
        }
    }

    private void procurarMentorNoEstadoFallback(@Nullable Location localizacaoUsuario, @Nullable String ownerId) {
        final String estado = (localizacaoUsuario != null) ? getEstadoFromLocation(localizacaoUsuario) : null;

        if (estado != null && !estado.isEmpty()) {
            Log.d(TAG_MATCHMAKING, "Fallback: Tentando encontrar no estado: " + estado);
            firestoreHelper.findMentoresByState(estado, ownerId, rState -> {
                if (rState.isOk() && rState.data != null && !rState.data.isEmpty()) {
                    Log.d(TAG_MATCHMAKING, "Sucesso Fallback: " + rState.data.size() + " mentores no estado. Ordenando...");
                    List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(rState.data, new ArrayList<>(), localizacaoUsuario);

                    // --- CORREÇÃO AQUI ---
                    if (this.ideia != null) {
                        this.ideia.setMatchmakingLog("Mentor encontrado por proximidade no estado (fallback).");
                    }

                    publicarIdeiaComMentor(ordenados.get(0).getId());
                } else {
                    publicarIdeiaSemMentor();
                }
            });
        } else {
            publicarIdeiaSemMentor();
        }
    }

    private String getCidadeFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            Log.e(TAG_MATCHMAKING, "Erro de Geocoder ao obter cidade", e);
        }
        return null;
    }

    private String getEstadoFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAdminArea();
            }
        } catch (IOException e) {
            Log.e(TAG_MATCHMAKING, "Erro de Geocoder ao obter estado", e);
        }
        return null;
    }

    private void publicarIdeiaComMentor(String mentorId) {
        Log.i(TAG_MATCHMAKING, "A executar 'publicarIdeiaComMentor'. MENTOR VINCULADO: " + mentorId);
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
        Log.e(TAG_MATCHMAKING, "NENHUM MENTOR COMPATÍVEL ENCONTRADO.");
        hideLoadingDialog(); // Esconde o dialog de "carregando"

        new AlertDialog.Builder(this)
                .setTitle("Nenhum Mentor Encontrado")
                .setMessage("Não encontramos um mentor com o perfil ideal no momento. Deseja publicar sua ideia para uma avaliação geral ou tentar a busca novamente?")
                .setPositiveButton("Publicar Mesmo Assim", (dialog, which) -> {
                    showLoadingDialog("Publicando para avaliação geral...");
                    // Usamos um postDelayed para o usuário perceber a mudança na mensagem
                    loadingHandler.postDelayed(() ->
                            firestoreHelper.publicarIdeia(ideia.getId(), null, r -> {
                                hideLoadingDialog();
                                if (r.isOk()) {
                                    Toast.makeText(this, "Ideia publicada para avaliação geral!", Toast.LENGTH_LONG).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, "Erro ao publicar ideia.", Toast.LENGTH_SHORT).show();
                                }
                            }), 1000);
                })
                .setNeutralButton("Refazer Busca", (dialog, which) -> {
                    // Simplesmente chama o início do processo de publicação de novo
                    verificarPermissaoEPublicar();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Toast.makeText(this, "Publicação cancelada.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false) // Impede que o usuário feche o diálogo clicando fora
                .show();
    }

    private void despublicarIdeia() {
        showLoadingDialog("Convertendo para rascunho...");
        firestoreHelper.unpublishIdeia(ideia.getId(), r -> {
            hideLoadingDialog();
            if (r.isOk()) {
                Toast.makeText(this, "Ideia revertida para rascunho!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Erro ao despublicar.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // --- Helpers e Callbacks ---

    @Override
    public void onDataChanged() {
        // Quando um post-it é adicionado/removido, o fragmento notifica-nos.
        // A UI já foi atualizada pelo Firestore listener, mas forçamos um save aqui.
        saveProgress(null);
    }

    private void notificarFragmentoAtual() {
        if (binding == null) return;
        Fragment frag = getSupportFragmentManager().findFragmentByTag("f" + binding.viewPagerCanvas.getCurrentItem());
        if (frag instanceof CanvasBlockFragment) {
            ((CanvasBlockFragment) frag).atualizarDadosIdeia(this.ideia);
        } else if (frag instanceof IdeiaStatusFragment) { // Adiciona a verificação para o StatusFragment
            ((IdeiaStatusFragment) frag).atualizarDadosIdeia(this.ideia);
        }
    }

    private void setupLocationPermissionLauncher() {
        requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) iniciarProcessoDePublicacao();
            else Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_LONG).show();
        });
    }

    private void setupAvaliacaoLauncher() {
        avaliacaoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Status da ideia atualizado!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onFieldsChanged(Ideia ideiaAtualizada) {
        if (this.ideia != null && ideiaAtualizada != null) {
            this.ideia.setNome(ideiaAtualizada.getNome());
            this.ideia.setDescricao(ideiaAtualizada.getDescricao());
            this.ideia.setAreasNecessarias(ideiaAtualizada.getAreasNecessarias());
        }
    }

    @Override
    public void onAmbienteIdealDetectado(boolean isIdeal) {
        if (etapas.get(binding.viewPagerCanvas.getCurrentItem()).getChave().equals(CanvasEtapa.CHAVE_AMBIENTE_CHECK)) {
            binding.btnProximo.setEnabled(isIdeal);
            binding.btnProximo.setAlpha(isIdeal ? 1.0f : 0.5f);
        }
    }

    @Override
    public void onPularCheck() {
        if (etapas.get(binding.viewPagerCanvas.getCurrentItem()).getChave().equals(CanvasEtapa.CHAVE_AMBIENTE_CHECK)) {
            binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1);
        }
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

    @SuppressLint("SetTextI18n")
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
    public void onEquipeChanged() {
        saveProgress(null);
    }
}