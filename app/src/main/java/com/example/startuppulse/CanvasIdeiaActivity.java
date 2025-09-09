package com.example.startuppulse;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
            if (etapaKey.equals(CanvasEtapa.CHAVE_STATUS) && "PUBLICADA".equals(ideia.getStatus())) {
                // Se o dono está na tela de status de uma ideia publicada, ele pode despublicar.
                binding.btnDespublicarIdeia.setVisibility(View.VISIBLE);
            } else if (isLastPage && "RASCUNHO".equals(ideia.getStatus()) && !isReadOnly) {
                // Se o dono está na última página de um rascunho, ele pode publicar.
                binding.btnPublicarIdeia.setVisibility(View.VISIBLE);
            }
        } else if (isMentor) {
            if (etapaKey.equals(CanvasEtapa.CHAVE_STATUS) && "PUBLICADA".equals(ideia.getStatus())) {
                // Se o mentor está na tela de status de uma ideia publicada, ele pode avaliar.
                binding.btnAvaliarIdeia.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Define as etapas (páginas) a serem exibidas com base no status da ideia.
     */
    private void setupEtapas(String status) {
        etapas = new ArrayList<>();
        if ("PUBLICADA".equals(status)) {
            etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status da Jornada", "Acompanhe a avaliação da sua ideia.", R.drawable.ic_flag));
        }
        if ("RASCUNHO".equals(status)) {
            etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_INICIO, "Capa da Ideia", "Dê um nome e uma breve descrição.", R.drawable.ic_lightbulb));
            etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_AMBIENTE_CHECK, "Check-up de Ambiente", "Prepare o ambiente para a criatividade.", R.drawable.ic_ambient));
        }

        // Adiciona todos os blocos do Business Model Canvas
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_PROPOSTA_VALOR, "Proposta de Valor", "O que torna sua ideia única?", R.drawable.ic_proposta_valor));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_SEGMENTO_CLIENTES, "Segmento de Clientes", "Para quem é esta ideia?", R.drawable.ic_segmento_clientes));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_CANAIS, "Canais", "Como você chegará até seus clientes?", R.drawable.ic_canais));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES, "Relacionamento", "Como você vai interagir?", R.drawable.ic_relacionamento));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FONTES_RENDA, "Fontes de Renda", "Como sua ideia vai gerar dinheiro?", R.drawable.ic_fontes_renda));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS, "Recursos Principais", "O que é essencial para funcionar?", R.drawable.ic_recursos));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_ATIVIDADES_CHAVE, "Atividades-Chave", "Quais são as ações mais importantes?", R.drawable.ic_atividades));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS, "Parcerias Principais", "Quem pode te ajudar?", R.drawable.ic_parcerias));
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS, "Estrutura de Custos", "Quais serão seus principais custos?", R.drawable.ic_custos));

        // Adiciona as novas etapas de preparação para investimento
        etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_EQUIPE, "Equipe", "Apresente os membros da sua equipe.", R.drawable.ic_person));

        // Adiciona a etapa final
        if ("RASCUNHO".equals(status)) {
            etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Publicar", "A sua ideia está pronta para descolar.", R.drawable.ic_rocket_launch));
        } else {
            etapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Resumo", "Revise os detalhes da sua ideia publicada.", R.drawable.ic_rocket_launch));
        }
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = user != null && user.getUid().equals(ideia.getOwnerId());
        this.isReadOnly = openAsReadOnly || (isOwner && !"RASCUNHO".equals(ideia.getStatus()));
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
        // Agora verificamos por FINE_LOCATION, que é mais precisa e necessária para getCurrentLocation()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarProcessoDePublicacao();
        } else {
            // Se não tivermos a permissão, pedimo-la. O resultado será tratado no launcher.
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void iniciarProcessoDePublicacao() {
        // A verificação do GPS continua a ser o primeiro passo crucial
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

        showLoadingDialog("A obter a sua localização precisa...");

        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // SUCESSO! Obtivemos uma localização.
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            String cidade = null;
                            String estado = null;
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                cidade = address.getLocality();
                                estado = address.getAdminArea();
                            }
                            // CHAMAMOS O MATCHMAKING MESMO QUE A CIDADE OU ESTADO SEJAM NULOS
                            iniciarMatchmakingDeMentor(cidade, estado);

                        } catch (IOException e) {
                            // Se o geocoder falhar, ainda tentamos o matchmaking sem cidade/estado
                            iniciarMatchmakingDeMentor(null, null);
                        }
                    } else {
                        hideLoadingDialog();
                        Toast.makeText(this, "Não foi possível obter a sua localização. Tente novamente num local com melhor sinal.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    hideLoadingDialog();
                    Toast.makeText(this, "Erro ao obter localização: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void iniciarMatchmakingDeMentor(@Nullable String cidade, @Nullable String estado) {
        updateLoadingDialog("A procurar o mentor ideal...");
        Log.d(TAG_MATCHMAKING, "--- INICIANDO MATCHMAKING ---");

        final List<String> areasDaIdeia = (ideia != null) ? ideia.getAreasNecessarias() : new ArrayList<>();
        final String ownerId = (ideia != null) ? ideia.getOwnerId() : null;

        Log.d(TAG_MATCHMAKING, "Ideia precisa das áreas: " + areasDaIdeia.toString());
        Log.d(TAG_MATCHMAKING, "Localização: Cidade=" + cidade + ", Estado=" + estado);

        if (areasDaIdeia == null || areasDaIdeia.isEmpty()) {
            Log.d(TAG_MATCHMAKING, "Nenhuma área de especialização definida. A saltar diretamente para a busca por proximidade.");
            procurarMentorApenasPorProximidade(cidade, estado);
            return;
        }

        Log.d(TAG_MATCHMAKING, "Etapa 1: A procurar mentores por ÁREA...");
        firestoreHelper.findMentoresByAreas(areasDaIdeia, ownerId, r -> {
            if (!r.isOk()) {
                Log.e(TAG_MATCHMAKING, "Erro na busca por área: " + r.error.getMessage());
                procurarMentorApenasPorProximidade(cidade, estado);
                return;
            }

            List<Mentor> mentoresPorArea = r.data != null ? r.data : new ArrayList<>();
            Log.d(TAG_MATCHMAKING, "Encontrados " + mentoresPorArea.size() + " mentores com as áreas correspondentes.");

            if (!mentoresPorArea.isEmpty()) {
                updateLoadingDialog("A otimizar a seleção do mentor...");
                List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeELocal(mentoresPorArea, areasDaIdeia, cidade, estado);
                String mentorEscolhidoId = ordenados.get(0).getId();
                Log.d(TAG_MATCHMAKING, "DECISÃO: Mentor escolhido por área e proximidade. ID: " + mentorEscolhidoId);
                publicarIdeiaComMentor(mentorEscolhidoId);
            } else {
                Log.d(TAG_MATCHMAKING, "Nenhum mentor encontrado por área. A iniciar fallback por proximidade.");
                procurarMentorApenasPorProximidade(cidade, estado);
            }
        });
    }

    /**
     * Metodo de fallback que procura um mentor apenas por localização (cidade e depois estado).
     */
    private void procurarMentorApenasPorProximidade(@Nullable String cidade, @Nullable String estado) {
        updateLoadingDialog("A procurar por proximidade...");
        Log.d(TAG_MATCHMAKING, "Etapa 2 (Fallback): A procurar o mentor mais próximo.");
        final String ownerId = (ideia != null) ? ideia.getOwnerId() : null;

        if (cidade != null && !cidade.isEmpty()) {
            Log.d(TAG_MATCHMAKING, "A tentar encontrar na cidade: " + cidade);
            firestoreHelper.findMentorByCity(cidade, ownerId, rCity -> {
                if (rCity.isOk() && rCity.data != null) {
                    publicarIdeiaComMentor(rCity.data.getId());
                } else {
                    procurarMentorNoEstadoFallback(estado);
                }
            });
        } else {
            procurarMentorNoEstadoFallback(estado);
        }
    }

    private void procurarMentorNoEstadoFallback(@Nullable String estado) {
        if (estado != null && !estado.isEmpty()) {
            Log.d(TAG_MATCHMAKING, "Nenhum mentor encontrado na cidade. A expandir para o estado: " + estado);
            firestoreHelper.findMentorByState(estado, ideia.getOwnerId(), rState -> {
                if (rState.isOk() && rState.data != null) {
                    publicarIdeiaComMentor(rState.data.getId());
                } else {
                    publicarIdeiaSemMentor();
                }
            });
        } else {
            Log.w(TAG_MATCHMAKING, "Nenhuma cidade ou estado disponível para busca por proximidade.");
            publicarIdeiaSemMentor();
        }
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
        Log.e(TAG_MATCHMAKING, "A executar 'publicarIdeiaSemMentor'. NENHUM MENTOR FOI VINCULADO.");
        updateLoadingDialog("Nenhum mentor compatível encontrado. Publicando para avaliação geral...");
        loadingHandler.postDelayed(() ->
                firestoreHelper.publicarIdeia(ideia.getId(), null, r -> {
                    hideLoadingDialog();
                    if (r.isOk()) {
                        Toast.makeText(this, "Ideia publicada para avaliação geral!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Erro ao publicar ideia.", Toast.LENGTH_SHORT).show();
                    }
                }), 2000);
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
        Fragment frag = getSupportFragmentManager().findFragmentByTag("f" + binding.viewPagerCanvas.getCurrentItem());
        if (frag instanceof CanvasBlockFragment) {
            ((CanvasBlockFragment) frag).atualizarDadosIdeia(this.ideia);
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
    /**
     * Este metodo é exigido pela interface CanvasBlockListener.
     * No nosso caso, ele pode ficar vazio, pois as atualizações da lista de post-its
     * já são tratadas automaticamente pelo listener em tempo real do Firestore (attachIdeiaListener).
     */

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