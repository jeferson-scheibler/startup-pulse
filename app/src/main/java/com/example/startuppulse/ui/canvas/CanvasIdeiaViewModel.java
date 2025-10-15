package com.example.startuppulse.ui.canvas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.MembroEquipe;
import com.example.startuppulse.R;
import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.CanvasEtapa;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.Mentor;
import com.example.startuppulse.MentorMatchService;
import com.example.startuppulse.data.IdeiaRepository;
import com.example.startuppulse.data.MentorRepository;
import com.example.startuppulse.util.DialogEvent;
import com.example.startuppulse.util.Event;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import androidx.lifecycle.Transformations;

@HiltViewModel
public class CanvasIdeiaViewModel extends ViewModel {

    private static final String TAG_MATCHMAKING = "MentorMatchmakingVM";

    // --- Injeção de Dependências ---
    private final IdeiaRepository repository;
    private final AuthRepository authRepository;
    private final MentorRepository mentorRepository;

    // --- Estados da UI (observáveis pelo Fragment) ---
    private final MutableLiveData<Ideia> _ideia = new MutableLiveData<>();
    public final LiveData<Ideia> ideia = _ideia;

    private final MutableLiveData<List<CanvasEtapa>> _etapas = new MutableLiveData<>();
    public final LiveData<List<CanvasEtapa>> etapas = _etapas;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(true);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isReadOnly = new MutableLiveData<>(false);
    public final LiveData<Boolean> isReadOnly = _isReadOnly;

    private final MutableLiveData<Event<Boolean>> _closeScreen = new MutableLiveData<>();
    public final LiveData<Event<Boolean>> closeScreen = _closeScreen;

    // --- Eventos (para ações que acontecem uma única vez) ---
    private final MutableLiveData<Event<String>> _toastMessage = new MutableLiveData<>();
    public final LiveData<Event<String>> toastMessage = _toastMessage;

    private final MutableLiveData<Event<DialogEvent>> _dialogEvent = new MutableLiveData<>();
    public final LiveData<Event<DialogEvent>> dialogEvent = _dialogEvent;
    public final LiveData<Boolean> isPublishEnabled;
    private final MutableLiveData<String> _mentorNome = new MutableLiveData<>();
    public final LiveData<String> mentorNome = _mentorNome;
    public final LiveData<Boolean> isMentorPodeAvaliar;

    private final MutableLiveData<CanvasUIState> _uiState = new MutableLiveData<>();
    public final LiveData<CanvasUIState> uiState = _uiState;

    private final List<String> etapasObrigatorias = Arrays.asList(
            CanvasEtapa.CHAVE_PROPOSTA_VALOR,
            CanvasEtapa.CHAVE_SEGMENTO_CLIENTES,
            CanvasEtapa.CHAVE_CANAIS,
            CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES,
            CanvasEtapa.CHAVE_FONTES_RENDA,
            CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS,
            CanvasEtapa.CHAVE_ATIVIDADES_CHAVE,
            CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS,
            CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS
    );


    @Inject
    public CanvasIdeiaViewModel(IdeiaRepository repository, AuthRepository authRepository, MentorRepository mentorRepository) {
        this.repository = repository;
        this.authRepository = authRepository;
        this.mentorRepository = mentorRepository;

        isPublishEnabled = Transformations.map(_ideia, this::isIdeiaValidaParaPublicar);

        _ideia.observeForever(ideia -> {
            if (ideia != null && ideia.getMentorId() != null && !ideia.getMentorId().isEmpty()) {
                fetchMentorName(ideia.getMentorId());
            }
        });

        isMentorPodeAvaliar = Transformations.map(_ideia, ideia -> {
            if (ideia == null) return false;

            String currentUserId = authRepository.getCurrentUserId();
            boolean isMentorDesignado = currentUserId != null && currentUserId.equals(ideia.getMentorId());
            boolean isProntaParaAvaliacao = ideia.getStatus() == Ideia.Status.EM_AVALIACAO;

            return isMentorDesignado && isProntaParaAvaliacao;
        });
    }

    private void fetchMentorName(String mentorId) {
        mentorRepository.findMentorById(mentorId, result -> {
            if (result.isOk() && result.data != null) {
                _mentorNome.postValue(result.data.getNome());
            } else {
                _mentorNome.postValue(null);
            }
        });
    }

    public boolean isCurrentUserOwner() {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return false;
        return authRepository.isCurrentUser(ideiaAtual.getOwnerId());
    }

    @SuppressLint("MissingPermission") // A permissão já foi checada no Fragment
    public void procurarNovoMentor(Context context) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) {
            _toastMessage.setValue(new Event<>("Não foi possível encontrar a ideia."));
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(100, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    // A lógica de matchmaking que estava no FirestoreHelper agora é chamada aqui
                    iniciarProcessoDePublicacao(location);
                })
                .addOnFailureListener(e -> {
                    // Se falhar em obter a localização, tenta o matchmaking sem ela
                    _toastMessage.setValue(new Event<>("Não foi possível obter localização. Procurando mentores..."));
                    iniciarProcessoDePublicacao(null);
                });
    }

    private boolean isIdeiaValidaParaPublicar(Ideia ideia) {
        if (ideia == null) {
            return false;
        }

        // 1. Valida a tela inicial
        boolean nomeOk = ideia.getNome() != null && !ideia.getNome().trim().isEmpty();
        boolean descricaoOk = ideia.getDescricao() != null && !ideia.getDescricao().trim().isEmpty();
        boolean areasOk = ideia.getAreasNecessarias() != null && ideia.getAreasNecessarias().size() >= 2;

        if (!nomeOk || !descricaoOk || !areasOk) {
            return false;
        }

        // 2. Valida se cada etapa obrigatória do canvas tem pelo menos um post-it
        for (String chave : etapasObrigatorias) {
            List<PostIt> postIts = ideia.getPostItsPorChave(chave);
            if (postIts == null || postIts.isEmpty()) {
                return false; // Se encontrar UMA etapa vazia, já retorna falso.
            }
        }

        // Se passou por todas as validações, a ideia está pronta.
        return true;
    }

    // --- Lógica de Carregamento e Estado ---

    public void loadIdeia(String ideiaId, boolean openAsReadOnly) {
        _isLoading.setValue(true);
        if (ideiaId == null) {
            Ideia novaIdeia = new Ideia();
            novaIdeia.setOwnerId(authRepository.getCurrentUserId());
            _ideia.setValue(novaIdeia);
            setupEtapas(novaIdeia.getStatus());
            // Constrói e emite o estado inicial para uma nova ideia
            updateUiState(novaIdeia, false);
            _isLoading.setValue(false);
        } else {
            repository.listenToIdeia(ideiaId, ideaResult -> {
                if (ideaResult == null || !ideaResult.isOk() || ideaResult.data == null) {
                    _toastMessage.setValue(new Event<>("Erro: Ideia não encontrada."));
                    _closeScreen.setValue(new Event<>(true));
                    return;
                }
                Ideia ideia = ideaResult.data;
                boolean isOwner = authRepository.isCurrentUser(ideia.getOwnerId());
                boolean readOnly = openAsReadOnly || (isOwner && ideia.getStatus() != Ideia.Status.RASCUNHO);

                _ideia.setValue(ideia);
                setupEtapas(ideia.getStatus());
                // Constrói e emite o estado completo para uma ideia existente
                updateUiState(ideia, readOnly);
                _isLoading.setValue(false);
            });
        }
    }

    private void updateUiState(Ideia ideia, boolean isReadOnly) {
        if (ideia == null) return;

        String currentUserId = authRepository.getCurrentUserId();
        boolean isOwner = authRepository.isCurrentUser(ideia.getOwnerId());
        boolean isMentorDesignado = currentUserId != null && currentUserId.equals(ideia.getMentorId());
        boolean isProntaParaAvaliacao = ideia.getStatus() == Ideia.Status.EM_AVALIACAO;
        boolean isMentorPodeAvaliar = isMentorDesignado && isProntaParaAvaliacao;

        CanvasUIState newState = new CanvasUIState(ideia, isReadOnly, isOwner, isMentorPodeAvaliar);
        _uiState.setValue(newState);
    }

    /**
     * MODIFICAÇÃO 1: O método não recebe mais a ideia. Ele usa a que está no LiveData.
     */
    public void saveAndFinish() {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) {
            _closeScreen.setValue(new Event<>(true));
            return;
        }

        // Só processa o salvamento se a ideia for um rascunho.
        if (ideiaAtual.getStatus() == Ideia.Status.RASCUNHO && !Boolean.TRUE.equals(_isReadOnly.getValue())) {

            // CORREÇÃO: Validação para impedir rascunhos vazios.
            boolean isNewIdeia = ideiaAtual.getId() == null || ideiaAtual.getId().isEmpty();
            boolean hasContent = (ideiaAtual.getNome() != null && !ideiaAtual.getNome().trim().isEmpty()) ||
                    (ideiaAtual.getDescricao() != null && !ideiaAtual.getDescricao().trim().isEmpty()) ||
                    (ideiaAtual.getAreasNecessarias() != null && !ideiaAtual.getAreasNecessarias().isEmpty());

            // Se for uma nova ideia e não tiver conteúdo mínimo, simplesmente não salva.
            if (isNewIdeia && !hasContent) {
                _toastMessage.setValue(new Event<>("Rascunho vazio descartado."));
                _closeScreen.setValue(new Event<>(true));
                return; // Interrompe a execução aqui.
            }

            // Se for uma ideia existente ou uma nova ideia com conteúdo, salva.
            repository.saveIdeia(ideiaAtual, success -> {
                if (success) {
                    _toastMessage.setValue(new Event<>("Rascunho salvo!"));
                } else {
                    _toastMessage.setValue(new Event<>("Erro ao salvar rascunho."));
                }
                _closeScreen.setValue(new Event<>(true));
            });

        } else {
            // Se não for um rascunho (ex: apenas visualizando uma ideia publicada), simplesmente fecha a tela.
            _closeScreen.setValue(new Event<>(true));
        }
    }

    // --- Lógica de Publicação e Matchmaking ---

    /**
     * MODIFICAÇÃO 2: O método não recebe mais a ideia.
     */
    public void iniciarProcessoDePublicacao(@Nullable Location location) {
        _dialogEvent.setValue(new Event<>(new DialogEvent(DialogEvent.Type.LOADING, "Procurando o mentor ideal...")));
        iniciarMatchmakingDeMentor(location);
    }

    private void iniciarMatchmakingDeMentor(@Nullable Location localizacaoUsuario) {
        Ideia ideiaParaPublicar = _ideia.getValue();
        if (ideiaParaPublicar == null) {
            _dialogEvent.setValue(new Event<>(new DialogEvent(DialogEvent.Type.HIDE, "")));
            _toastMessage.setValue(new Event<>("Erro: Não foi possível carregar a ideia para publicação."));
            return;
        }

        Log.d(TAG_MATCHMAKING, "--- INICIANDO MATCHMAKING ---");
        final List<String> areasDaIdeia = ideiaParaPublicar.getAreasNecessarias();
        final String ownerId = ideiaParaPublicar.getOwnerId();

        mentorRepository.findMentoresByAreas(areasDaIdeia, ownerId, result -> {
            if (result.isOk() && result.data != null && !result.data.isEmpty()) {
                List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(result.data, areasDaIdeia, localizacaoUsuario);
                if (ordenados != null && !ordenados.isEmpty()) {
                    Mentor mentorEscolhido = ordenados.get(0);
                    if (mentorEscolhido != null && mentorEscolhido.getId() != null) {
                        Log.i(TAG_MATCHMAKING, "MENTOR ESCOLHIDO. ID: " + mentorEscolhido.getId());
                        publicarIdeiaComMentor(mentorEscolhido.getId());
                        return;
                    }
                }
            }
            _dialogEvent.setValue(new Event<>(new DialogEvent(DialogEvent.Type.NO_MENTOR_FOUND, "")));
        });
    }

    /**
     * MODIFICAÇÃO 3: O método não recebe mais a ideia.
     */
    public void publicarIdeiaSemMentor() {
        Ideia ideiaParaPublicar = _ideia.getValue();
        if (ideiaParaPublicar == null) return; // Segurança

        _dialogEvent.setValue(new Event<>(new DialogEvent(DialogEvent.Type.LOADING, "Publicando para avaliação geral...")));
        ideiaParaPublicar.setStatus(Ideia.Status.EM_AVALIACAO);
        ideiaParaPublicar.setMentorId(null);

        repository.saveIdeia(ideiaParaPublicar, success -> {
            _dialogEvent.setValue(new Event<>(new DialogEvent(DialogEvent.Type.HIDE, "")));
            if (success) {
                _toastMessage.setValue(new Event<>("Ideia publicada para avaliação geral!"));
                _closeScreen.setValue(new Event<>(true));
            } else {
                _toastMessage.setValue(new Event<>("Erro ao publicar ideia."));
            }
        });
    }

    private void publicarIdeiaComMentor(String mentorId) {
        Ideia ideiaParaPublicar = _ideia.getValue();
        if (ideiaParaPublicar == null) return; // Segurança

        ideiaParaPublicar.setStatus(Ideia.Status.EM_AVALIACAO);
        ideiaParaPublicar.setMentorId(mentorId);

        repository.saveIdeia(ideiaParaPublicar, success -> {
            _dialogEvent.setValue(new Event<>(new DialogEvent(DialogEvent.Type.HIDE, "")));
            if (success) {
                _toastMessage.setValue(new Event<>("Ideia publicada e enviada para um mentor!"));
                _closeScreen.setValue(new Event<>(true));
            } else {
                _toastMessage.setValue(new Event<>("Erro ao publicar ideia."));
            }
        });
    }

    /**
     * MODIFICAÇÃO 4: O método não recebe mais a ideia.
     */
    public void despublicarIdeia() {
        Ideia ideiaParaDespublicar = _ideia.getValue();
        if (ideiaParaDespublicar == null) return; // Segurança

        ideiaParaDespublicar.setStatus(Ideia.Status.RASCUNHO);
        repository.saveIdeia(ideiaParaDespublicar, success -> {
            if (success) {
                _toastMessage.setValue(new Event<>("Ideia revertida para rascunho!"));
                _closeScreen.setValue(new Event<>(true));
            } else {
                _toastMessage.setValue(new Event<>("Erro ao despublicar."));
            }
        });
    }

    public void addPostIt(String etapaChave, String texto, String cor) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) {
            _toastMessage.setValue(new Event<>("Salve a ideia antes de adicionar post-its."));
            return;
        }

        // CORREÇÃO: O ViewModel cria o objeto PostIt.
        PostIt novoPostIt = new PostIt(texto, cor, new Date());

        // CORREÇÃO: A chamada agora corresponde exatamente à assinatura do repositório.
        repository.addPostitToIdeia(ideiaAtual.getId(), etapaChave, novoPostIt, success -> {
            if (!success) {
                _toastMessage.setValue(new Event<>("Erro ao adicionar post-it."));
            }
        });
    }

    public void updatePostIt(String etapaChave, PostIt postItAntigo, String novoTexto, String novaCor) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) return;

        // CORREÇÃO: O ViewModel cria o NOVO objeto PostIt com os dados atualizados.
        PostIt postItAtualizado = new PostIt(novoTexto, novaCor, new Date());
        postItAtualizado.setId(postItAntigo.getId()); // Preserva o ID original

        // CORREÇÃO: A chamada agora passa o objeto antigo e o novo, como esperado.
        repository.updatePostitInIdeia(ideiaAtual.getId(), etapaChave, postItAntigo, postItAtualizado, success -> {
            if (!success) {
                _toastMessage.setValue(new Event<>("Erro ao atualizar post-it."));
            }
        });
    }

    /**
     * MODIFICAÇÃO 5: Novo método para que o CanvasBlockFragment possa deletar um Post-it.
     */
    public void deletePostIt(String etapaChave, PostIt postIt) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) return;

        // A lógica de negócio (chamar o repositório) fica aqui
        repository.deletePostitFromIdeia(ideiaAtual.getId(), etapaChave, postIt, result -> {
            if (!result) {
                _toastMessage.setValue(new Event<>("Erro ao apagar post-it."));
            }
            // A UI será atualizada automaticamente pelo listener do `loadIdeia`.
        });
    }

    public void addMembroEquipe(MembroEquipe novoMembro) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        if (ideiaAtual.getEquipe() == null) {
            ideiaAtual.setEquipe(new ArrayList<>());
        }
        ideiaAtual.getEquipe().add(novoMembro);

        // Notifica os observers que a 'Ideia' (e sua lista interna de equipe) foi modificada.
        _ideia.setValue(ideiaAtual);
    }

    public void updateMembroEquipe(MembroEquipe membroExistente, String novoNome, String novaFuncao, String novoLinkedin) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getEquipe() == null) return;

        // Encontra o membro na lista e atualiza seus dados
        int index = ideiaAtual.getEquipe().indexOf(membroExistente);
        if (index != -1) {
            MembroEquipe membroParaAtualizar = ideiaAtual.getEquipe().get(index);
            membroParaAtualizar.setNome(novoNome);
            membroParaAtualizar.setFuncao(novaFuncao);
            membroParaAtualizar.setLinkedinUrl(novoLinkedin);

            // Notifica os observers da mudança.
            _ideia.setValue(ideiaAtual);
        }
    }

    // --- Configuração das Etapas (Lógica de Negócio) ---
    private void setupEtapas(Ideia.Status status) {
        List<CanvasEtapa> novasEtapas = new ArrayList<>();
        if (status == null) status = Ideia.Status.RASCUNHO;

        switch (status) {
            case RASCUNHO:
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_INICIO, "Capa", R.drawable.ic_lightbulb));
                adicionarBlocosCanvas(novasEtapas);
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Publicar", R.drawable.ic_rocket_launch));
                break;
            case EM_AVALIACAO:
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                break;
            case AVALIADA_APROVADA:
            case AVALIADA_REPROVADA:
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                adicionarBlocosCanvas(novasEtapas);
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_EQUIPE, "Equipe", R.drawable.ic_person));
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Reavaliar", R.drawable.ic_rocket_launch));
                break;
        }
        _etapas.setValue(novasEtapas);
    }

    private void adicionarBlocosCanvas(List<CanvasEtapa> lista) {
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_PROPOSTA_VALOR, "Proposta de Valor", "O que torna sua ideia única?", R.drawable.ic_proposta_valor));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_SEGMENTO_CLIENTES, "Segmento de Clientes", "Para quem é esta ideia?", R.drawable.ic_segmento_clientes));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_CANAIS, "Canais", "Como você chegará até seus clientes?", R.drawable.ic_canais));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES, "Relacionamento", "Como você vai interagir?", R.drawable.ic_relacionamento));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_FONTES_RENDA, "Fontes de Renda", "Como sua ideia vai gerar dinheiro?", R.drawable.ic_fontes_renda));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS, "Recursos Principais", "O que é essencial para funcionar?", R.drawable.ic_recursos));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_ATIVIDADES_CHAVE, "Atividades-Chave", "Quais são as ações mais importantes?", R.drawable.ic_atividades));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS, "Parcerias Principais", "Quem pode te ajudar?", R.drawable.ic_parcerias));
        lista.add(new CanvasEtapa(CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS, "Estrutura de Custos", "Quais serão seus principais custos?", R.drawable.ic_custos));
    }

    public void updateIdeiaBasics(String nome, String descricao, List<String> areas) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        // Verifica se houve realmente uma mudança para evitar atualizações desnecessárias do LiveData
        boolean mudouNome = !Objects.equals(ideiaAtual.getNome(), nome);
        boolean mudouDescricao = !Objects.equals(ideiaAtual.getDescricao(), descricao);

        // Compara listas sem se importar com a ordem
        Set<String> areasAtuais = (ideiaAtual.getAreasNecessarias() != null) ? new HashSet<>(ideiaAtual.getAreasNecessarias()) : new HashSet<>();
        Set<String> novasAreas = (areas != null) ? new HashSet<>(areas) : new HashSet<>();
        boolean mudouAreas = !areasAtuais.equals(novasAreas);

        if (mudouNome || mudouDescricao || mudouAreas) {
            ideiaAtual.setNome(nome);
            ideiaAtual.setDescricao(descricao);
            ideiaAtual.setAreasNecessarias(new ArrayList<>(novasAreas));
            _ideia.setValue(ideiaAtual); // Notifica os observers que a ideia foi atualizada
        }
    }
}