package com.example.startuppulse.ui.canvas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.R;
import com.example.startuppulse.data.models.CanvasEtapa;
import com.example.startuppulse.data.models.MembroEquipe;
import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.MentorMatchService;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IIdeiaRepository;
import com.example.startuppulse.data.repositories.IMentorRepository;
import com.example.startuppulse.data.models.PostIt;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.util.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CanvasIdeiaViewModel extends ViewModel {

    private static final String TAG = "CanvasViewModel_DEBUG";

    // --- Repositórios Injetados ---
    private final IIdeiaRepository ideiaRepository;
    private final IMentorRepository mentorRepository;
    private final IAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final FirebaseFirestore firestore;

    private ListenerRegistration ideiaListener;
    private String ideiaId;

    // --- LiveData para a UI ---

    private final MutableLiveData<Ideia> _ideia = new MutableLiveData<>();
    public final LiveData<Ideia> ideia = _ideia;


    // ===== CORREÇÃO: O LiveData de 'etapas' foi restaurado =====
    private final MutableLiveData<List<CanvasEtapa>> _etapas = new MutableLiveData<>();
    public final LiveData<List<CanvasEtapa>> etapas = _etapas;
    private final MutableLiveData<String> _mentorNome = new MutableLiveData<>();
    public final LiveData<String> mentorNome = _mentorNome;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(true);
    public final LiveData<Boolean> isLoading = _isLoading;
    private final MutableLiveData<Boolean> _isIaLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isIaLoading = _isIaLoading;

    // --- Eventos para Ações Únicas ---
    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public final LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _closeScreenEvent = new MutableLiveData<>();
    public final LiveData<Event<Boolean>> closeScreenEvent = _closeScreenEvent;
    public final LiveData<Boolean> isPublishEnabled;

    private final MutableLiveData<Event<PostIt>> _editPostItEvent = new MutableLiveData<>();
    public LiveData<Event<PostIt>> editPostItEvent = _editPostItEvent;

    private final MutableLiveData<Event<PostIt>> _deletePostItEvent = new MutableLiveData<>();
    public LiveData<Event<PostIt>> deletePostItEvent = _deletePostItEvent;

    private final MutableLiveData<Boolean> _isVoting = new MutableLiveData<>(false);
    public final LiveData<Boolean> isVoting = _isVoting;

    private final MutableLiveData<Float> _userVote = new MutableLiveData<>(0f);
    public final LiveData<Float> userVote = _userVote;

    private final MutableLiveData<Boolean> _matchLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> matchLoading = _matchLoading;
    private final MutableLiveData<Boolean> _isRematching = new MutableLiveData<>(false);
    public LiveData<Boolean> isRematching = _isRematching;

    private final MutableLiveData<String> _rematchMessage = new MutableLiveData<>();
    public LiveData<String> rematchMessage = _rematchMessage;

    private final MutableLiveData<String> _matchProgressMessage = new MutableLiveData<>();
    public final LiveData<String> matchProgressMessage = _matchProgressMessage;

    // Evento: solicita ao UI que pergunte ao usuário qual localização usar
    private final MutableLiveData<Event<MatchLocationChoiceRequest>> _matchLocationRequest = new MutableLiveData<>();
    public final LiveData<Event<MatchLocationChoiceRequest>> matchLocationRequest = _matchLocationRequest;

    // Evento: lista de candidatos (para UI exibir uma escolha)
    private final MutableLiveData<Event<List<User>>> _matchCandidatesEvent = new MutableLiveData<>();
    public final LiveData<Event<List<User>>> matchCandidatesEvent = _matchCandidatesEvent;

    // Evento: resultado do match (sucesso/falha) - UI decide mostrar toast/dialog
    private final MutableLiveData<Event<MatchResult>> _matchResultEvent = new MutableLiveData<>();
    public final LiveData<Event<MatchResult>> matchResultEvent = _matchResultEvent;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private final List<String> etapasObrigatorias = Arrays.asList(
            CanvasEtapa.CHAVE_PROPOSTA_VALOR, CanvasEtapa.CHAVE_SEGMENTO_CLIENTES,
            CanvasEtapa.CHAVE_CANAIS, CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES,
            CanvasEtapa.CHAVE_FONTES_RENDA, CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS,
            CanvasEtapa.CHAVE_ATIVIDADES_CHAVE, CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS,
            CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS
    );

    public static class MatchLocationChoiceRequest {
        private final boolean hasIdeaLocation;
        private final boolean hasUserLocation;
        private final Location ideaLocation;
        private final Location userLocation;

        public MatchLocationChoiceRequest(@Nullable Location ideaLocation, @Nullable Location userLocation) {
            this.ideaLocation = ideaLocation;
            this.userLocation = userLocation;
            this.hasIdeaLocation = (ideaLocation != null);
            this.hasUserLocation = (userLocation != null);
        }

        public boolean hasIdeaLocation() {
            return hasIdeaLocation;
        }

        public boolean hasUserLocation() {
            return hasUserLocation;
        }

        @Nullable
        public Location getIdeaLocation() {
            return ideaLocation;
        }

        @Nullable
        public Location getUserLocation() {
            return userLocation;
        }
    }

    @Inject
    public CanvasIdeiaViewModel(IIdeiaRepository ideiaRepository, IMentorRepository mentorRepository, IAuthRepository authRepository, IUserRepository userRepository, FirebaseFirestore firestore) {
        this.ideiaRepository = ideiaRepository;
        this.mentorRepository = mentorRepository;
        this.authRepository = authRepository;
        this.firestore = firestore;
        this.userRepository = userRepository;
        isPublishEnabled = Transformations.map(_ideia, this::isIdeiaValidaParaPublicar);
        _etapas.setValue(new ArrayList<>());
        _isIaLoading.setValue(false);
        _isVoting.setValue(false);
        _userVote.setValue(0f);
    }

    public void loadIdeia(@Nullable String ideiaId) {
        Log.d(TAG, "loadIdeia: Método chamado com ideiaId = " + ideiaId);

        this.ideiaId = ideiaId;
        _userVote.setValue(0f);

        if (ideiaListener != null) {
            ideiaListener.remove();
        }
        _isLoading.setValue(true);

        if (ideiaId == null) {
            Log.d(TAG, "loadIdeia: ID é nulo. Criando uma nova ideia em branco.");

            String currentUserId = authRepository.getCurrentUserId();
            if (currentUserId == null) {
                // Caso de segurança: usuário deslogou no meio do processo
                Log.e(TAG, "loadIdeia: Usuário não está logado. Encerrando.");
                _toastEvent.setValue(new Event<>("Erro: Usuário não autenticado."));
                _closeScreenEvent.setValue(new Event<>(true));
                _isLoading.setValue(false);
                return;
            }

            authRepository.getUserProfile(currentUserId, result -> {
                if (result instanceof Result.Success) {
                    User currentUser = ((Result.Success<User>) result).data;

                    String autorNome = (currentUser != null && currentUser.getNome() != null) ? currentUser.getNome() : "Autor Desconhecido";

                    // Agora sim, cria a nova ideia com todos os dados
                    Ideia novaIdeia = new Ideia();
                    novaIdeia.setId(ideiaRepository.getNewIdeiaId());
                    novaIdeia.setOwnerId(currentUserId);
                    novaIdeia.setAutorNome(autorNome);

                    this.ideiaId = novaIdeia.getId();

                    _ideia.setValue(novaIdeia);
                    setupEtapas(novaIdeia);
                    _isLoading.setValue(false);

                } else {
                    // Falha ao buscar o perfil do usuário
                    Log.e(TAG, "loadIdeia: Falha ao buscar perfil do usuário para nova ideia.", (result instanceof Result.Error) ? ((Result.Error<User>) result).error : null);
                    _toastEvent.setValue(new Event<>("Erro ao carregar dados do usuário."));
                    _closeScreenEvent.setValue(new Event<>(true));
                    _isLoading.setValue(false);
                }
            });
        } else {
            Log.d(TAG, "loadIdeia: ID não é nulo. A escutar a ideia no repositório.");
            ideiaListener = ideiaRepository.listenToIdeia(ideiaId, result -> {
                _isLoading.setValue(false);
                if (result instanceof Result.Success) {
                    Ideia ideiaAtualizada = ((Result.Success<Ideia>) result).data;
                    Log.d(TAG, "listenToIdeia Callback: Sucesso. Ideia recebida com nome = " + (ideiaAtualizada != null ? ideiaAtualizada.getNome() : "nulo"));
                    _ideia.setValue(ideiaAtualizada);

                    if (ideiaAtualizada != null) {
                        setupEtapas(ideiaAtualizada);
                        garantirMembroIdealizador(ideiaAtualizada);
                        if (ideiaAtualizada.getMentorId() != null && !ideiaAtualizada.getMentorId().isEmpty()) {
                            fetchMentorName(ideiaAtualizada.getMentorId());
                        }
                        loadUserVote();
                    }
                } else if (result instanceof Result.Error) {
                    Log.e(TAG, "listenToIdeia Callback: Erro ao carregar ideia.", ((Result.Error<Ideia>) result).error);
                    _toastEvent.setValue(new Event<>("Erro ao carregar ideia: " + ((Result.Error<Ideia>) result).error.getMessage()));
                    _closeScreenEvent.setValue(new Event<>(true));
                }
            });
        }
    }

    public static class MatchResult {
        public final boolean success;
        public final String message;
        public MatchResult(boolean success, String message) { this.success = success; this.message = message; }
    }



    private void loadUserVote() {
        String currentUserId = authRepository.getCurrentUserId();
        Ideia ideiaAtual = _ideia.getValue();

        // Só tenta carregar se for um visitante e a ideia estiver carregada
        if (currentUserId == null || ideiaAtual == null || isCurrentUserOwner() || isCurrentUserTheMentor()) {
            _userVote.postValue(0f); // Garante que está zerado se não for visitante
            return;
        }

        // Busca o documento do voto específico deste usuário nesta ideia
        firestore.collection("ideias").document(ideiaAtual.getId())
                .collection("votosComunidade").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Voto encontrado, pega o valor
                            Double votoDouble = document.getDouble("voto");
                            _userVote.postValue(votoDouble != null ? votoDouble.floatValue() : 0f);
                        } else {
                            // Documento não existe = usuário não votou ainda
                            _userVote.postValue(0f);
                        }
                    } else {
                        // Erro ao buscar o voto (não crítico, assume que não votou)
                        Log.w(TAG, "Erro ao buscar voto do usuário.", task.getException());
                        _userVote.postValue(0f);
                    }
                });
    }

    public void votarNaComunidade(float voto) {
        Ideia ideiaAtual = _ideia.getValue();
        String currentUserId = authRepository.getCurrentUserId();
        if (ideiaAtual == null || currentUserId == null || ideiaId == null) { // Usa o ideiaId da classe
            _toastEvent.setValue(new Event<>("Erro ao obter dados para votar."));
            return;
        }
        // Impede dono e mentor de votar
        if (isCurrentUserOwner() || isCurrentUserTheMentor()) {
            _toastEvent.setValue(new Event<>("Dono e Mentor não podem votar."));
            return;
        }
        // Impede votos repetidos (opcional, mas bom para UI)
        if (_userVote.getValue() != null && Math.abs(_userVote.getValue() - voto) < 0.01f) {
            _toastEvent.setValue(new Event<>("Você já deu essa avaliação."));
            return;
        }


        _isVoting.setValue(true); // <<< Mostra loading

        // 1. Buscar perfil do usuário para obter áreas
        authRepository.getUserProfile(currentUserId, userResult -> {
            if (userResult instanceof Result.Success) {
                User currentUser = ((Result.Success<User>) userResult).data;
                List<String> userAreas = (currentUser != null && currentUser.getAreasDeInteresse() != null)
                        ? currentUser.getAreasDeInteresse() : new ArrayList<>();
                List<String> ideiaAreas = ideiaAtual.getAreasNecessarias() != null
                        ? ideiaAtual.getAreasNecessarias() : new ArrayList<>();

                // 2. Calcular Peso
                int peso = calcularPesoVoto(userAreas, ideiaAreas);
                Log.d(TAG, "Calculando peso para voto: UserAreas=" + userAreas.size() + ", IdeiaAreas=" + ideiaAreas.size() + " -> Peso=" + peso);


                // 3. Chamar o Repositório para salvar
                ideiaRepository.salvarVotoComunidade(ideiaId, currentUserId, voto, peso, saveResult -> {
                    _isVoting.setValue(false); // <<< Esconde loading
                    if (saveResult instanceof Result.Success) {
                        _toastEvent.setValue(new Event<>("Voto registrado!"));
                        _userVote.setValue((float) voto); // <<< Atualiza o LiveData do voto do usuário
                        // A Cloud Function cuidará da média. O listener da ideia pegará a atualização.
                    } else {
                        Log.e(TAG, "Erro ao salvar voto", ((Result.Error<Void>)saveResult).error);
                        _toastEvent.setValue(new Event<>("Erro ao registrar voto. Tente novamente."));
                    }
                });

            } else {
                _isVoting.setValue(false); // <<< Esconde loading em caso de erro
                Log.e(TAG, "Erro ao buscar perfil do usuário para votar", ((Result.Error<User>)userResult).error);
                _toastEvent.setValue(new Event<>("Erro ao buscar seu perfil para votar."));
            }
        });
    }

    private int calcularPesoVoto(List<String> userAreas, List<String> ideiaAreas) {
        if (userAreas == null || ideiaAreas == null || userAreas.isEmpty() || ideiaAreas.isEmpty()) {
            return 1; // Leigo
        }
        // Usa conjuntos para eficiência na interseção
        java.util.Set<String> userAreaSet = new java.util.HashSet<>(userAreas);
        java.util.Set<String> ideiaAreaSet = new java.util.HashSet<>(ideiaAreas);

        userAreaSet.retainAll(ideiaAreaSet); // Mantém apenas os elementos comuns
        long commonAreas = userAreaSet.size();

        if (commonAreas > 1) {
            return 3; // Especialista
        } else if (commonAreas == 1) {
            return 2; // Relacionado
        } else {
            return 1; // Leigo
        }
    }

    private boolean isIdeiaValidaParaPublicar(Ideia ideia) {
        if (ideia == null) return false;

        // 1. Valida a capa (Título, Descrição, Áreas)
        boolean nomeOk = ideia.getNome() != null && !ideia.getNome().trim().isEmpty();
        boolean descricaoOk = ideia.getDescricao() != null && !ideia.getDescricao().trim().isEmpty();
        boolean areasOk = ideia.getAreasNecessarias() != null && ideia.getAreasNecessarias().size() >= 2;

        if (!nomeOk || !descricaoOk || !areasOk) return false;

        // 2. Valida se cada bloco obrigatório do canvas tem pelo menos um post-it
        for (String chave : etapasObrigatorias) {
            List<PostIt> postIts = ideia.getPostItsPorChave(chave);
            if (postIts == null || postIts.isEmpty()) {
                return false; // Encontrou uma etapa vazia, a validação falha.
            }
        }

        // Se todas as validações passaram, a ideia pode ser publicada.
        return true;
    }

    /**
     * Reverte uma ideia publicada de volta para o estado de Rascunho.
     */
    public void despublicarIdeia() {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) {
            _toastEvent.setValue(new Event<>("Erro: Ideia não encontrada."));
            return;
        }

        // Delega a ação para o repositório
        ideiaRepository.unpublishIdeia(ideiaAtual.getId(), result -> {
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Ideia revertida para rascunho."));
                _closeScreenEvent.postValue(new Event<>(true));
                // O listener em tempo real cuidará de atualizar a UI para o estado de rascunho.
            } else {
                _toastEvent.setValue(new Event<>("Erro ao despublicar a ideia."));
            }
        });
    }

    public void addPostIt(@NonNull String etapaChave, @NonNull String texto, @NonNull String cor) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) {
            _toastEvent.setValue(new Event<>("Salve a ideia antes de adicionar post-its."));
            return;
        }
        // Só permite adicionar se for rascunho
        if (ideiaAtual.getStatus() != Ideia.Status.RASCUNHO) {
            _toastEvent.setValue(new Event<>("Não é possível adicionar post-its a uma ideia publicada."));
            return;
        }


        PostIt novoPostIt = new PostIt(texto, cor, new Date());
        // O ID do PostIt pode ser gerado aqui ou no repositório, se necessário
        // Se o repo gerar, você pode precisar ajustar a lógica se precisar do ID imediatamente.
        // novoPostIt.setId(UUID.randomUUID().toString()); // Exemplo se precisar gerar aqui

        // Mostra o loading enquanto salva
        _isLoading.setValue(true);

        ideiaRepository.addPostitToIdeia(ideiaAtual.getId(), etapaChave, novoPostIt, result -> {
            _isLoading.setValue(false); // Esconde o loading
            if (result instanceof Result.Error) {
                _toastEvent.postValue(new Event<>("Erro ao adicionar post-it: " + ((Result.Error<Void>)result).error.getMessage()));
            }
            // No sucesso, não fazemos nada aqui. O listener do Firestore (listenToIdeia)
            // que está ativo deve receber a atualização e atualizar o _ideia LiveData,
            // que por sua vez atualizará a UI.
        });
    }

    public void requestEditPostIt(PostIt postIt) {
        _editPostItEvent.setValue(new Event<>(postIt));
    }

    public void requestDeletePostIt(PostIt postIt) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia != null && currentIdeia.getStatus() == Ideia.Status.RASCUNHO) {
            _deletePostItEvent.setValue(new Event<>(postIt));
        }
    }

    public void saveEditedPostIt(@NonNull String etapaChave, @NonNull PostIt originalPostIt, @NonNull String newText, @NonNull String newColor) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null || currentIdeia.getId() == null || originalPostIt == null || etapaChave == null) {
            _toastEvent.setValue(new Event<>("Erro: Dados inválidos para salvar post-it."));
            return;
        }
        // Só permite editar se for rascunho
        if (currentIdeia.getStatus() != Ideia.Status.RASCUNHO) {
            _toastEvent.setValue(new Event<>("Não é possível editar post-its de uma ideia publicada."));
            return;
        }

        // Cria o *novo* objeto PostIt com os dados atualizados
        // Mantém o ID e o timestamp original, atualiza o lastModified
        PostIt updatedPostIt = new PostIt(newText, newColor, originalPostIt.getTimestamp());
        updatedPostIt.setId(originalPostIt.getId()); // Garante que o ID seja o mesmo
        updatedPostIt.setLastModified(new Date());

        _isLoading.setValue(true);

        ideiaRepository.updatePostitInIdeia(currentIdeia.getId(), etapaChave, originalPostIt, updatedPostIt, new ResultCallback<Void>() {
            @Override
            public void onResult(Result<Void> result) {
                _isLoading.setValue(false);
                if (result instanceof Result.Error) {
                    _toastEvent.postValue(new Event<>("Erro ao atualizar post-it: " + ((Result.Error<Void>)result).error.getMessage()));
                }
                // No sucesso, o listener do Firestore atualizará a UI.
            }
        });
    }

    public void updateIdeiaBasics(String nome, String descricao, List<String> areas) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        // --- CORREÇÃO: SÓ SALVA SE FOR RASCUNHO ---
        // Impede a alteração de dados básicos se a ideia já estiver publicada
        if (ideiaAtual.getStatus() != Ideia.Status.RASCUNHO) {
            Log.w(TAG, "updateIdeiaBasics: Tentativa de alterar ideia não-rascunho. Ignorado.");
            return;
        }
        // --- FIM DA CORREÇÃO ---

        // Atualiza os campos do objeto de ideia atual
        ideiaAtual.setNome(nome);
        ideiaAtual.setDescricao(descricao); // Usando o campo 'bio' do novo modelo Mentor/Ideia
        ideiaAtual.setAreasNecessarias(areas);

        // Emite o objeto modificado. O LiveData notificará os observers (UI otimista).
        _ideia.setValue(ideiaAtual);

        // --- CORREÇÃO PRINCIPAL: SALVAR NO DEBOUNCE ---
        // Agora, também persiste essa mudança no repositório.
        // O repositório usará 'set' (saveIdeia), que funciona para CRIAR ou ATUALIZAR.
        ideiaRepository.saveIdeia(ideiaAtual, result -> {
            if (result instanceof Result.Error) {
                // Se falhar, registra o erro. O debounce tentará salvar de novo na próxima.
                Log.e(TAG, "Falha ao salvar (debounce) updateIdeiaBasics", ((Result.Error<Void>) result).error);
                // _toastEvent.postValue(new Event<>("Falha ao salvar rascunho.")); // Opcional
            } else if (result instanceof Result.Success) {
                // Sucesso.
                // Se a ideia era NOVA (ideiaListener == null), agora ela existe no banco.
                // Precisamos iniciar o listener para ela.
                garantirListenerAtivo();
            }
        });
        // --- FIM DA CORREÇÃO ---
    }

    private void garantirListenerAtivo() {
        // Checa se o listener está nulo E se a ideia já tem um ID (o que ela terá)
        if (ideiaListener == null && _ideia.getValue() != null && _ideia.getValue().getId() != null) {
            String ideiaId = _ideia.getValue().getId();
            Log.d(TAG, "garantirListenerAtivo: Primeiro salvamento detectado. Ativando listener para a ideia ID: " + ideiaId);

            // Chamar loadIdeia(ideiaId) fará com que o ViewModel
            // se registre para ouvir mudanças em tempo real no documento
            // que acabamos de criar.
            loadIdeia(ideiaId);
        }
    }

    public void addMembroEquipe(@NonNull MembroEquipe novoMembro) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        if (ideiaAtual.getEquipe() == null) {
            ideiaAtual.setEquipe(new ArrayList<>());
        }
        ideiaAtual.getEquipe().add(novoMembro);

        // Notifica os observers que a 'Ideia' (e sua lista interna de equipe) foi modificada.
        _ideia.setValue(ideiaAtual);
    }

    public void updateMembroEquipe(@NonNull MembroEquipe membroExistente, @NonNull String novoNome, @NonNull String novaFuncao, @NonNull String novoLinkedin) {
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

    private void setupEtapas(Ideia ideia) {
        if (ideia == null) return;
        Ideia.Status status = ideia.getStatus() != null ? ideia.getStatus() : Ideia.Status.RASCUNHO;

        List<CanvasEtapa> novasEtapas = new ArrayList<>();
        switch (status) {
            case RASCUNHO:
                // Em modo rascunho, mostramos tudo: Capa, Blocos e a tela Final de publicação.
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_INICIO, "Capa", R.drawable.ic_lightbulb));
                adicionarBlocosCanvas(novasEtapas);
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_FINAL, "Publicar", R.drawable.ic_rocket_launch));
                break;

            case EM_AVALIACAO:
                // Em avaliação, mostramos apenas o Status e os Blocos do Canvas (em modo leitura).
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                adicionarBlocosCanvas(novasEtapas);
                break;

            case AVALIADA_APROVADA:
            case AVALIADA_REPROVADA:
                // Após a avaliação, mostramos Status, Blocos e a tela de Equipe.
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_STATUS, "Status", R.drawable.ic_flag));
                adicionarBlocosCanvas(novasEtapas);
                novasEtapas.add(new CanvasEtapa(CanvasEtapa.CHAVE_EQUIPE, "Equipe", R.drawable.ic_person));
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

    private void fetchMentorName(String mentorId) {
        if (mentorId == null || mentorId.isEmpty()) {
            _mentorNome.postValue("Mentor não encontrado");
            return;
        }

        // Agora buscamos o User (onde está o nome e a foto). UserRepository popula mentorData quando aplicável.
        userRepository.getUserProfile(mentorId, result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user != null && user.getNome() != null && !user.getNome().isEmpty()) {
                    _mentorNome.postValue(user.getNome());
                } else {
                    _mentorNome.postValue("Mentor sem nome");
                }
            } else {
                _mentorNome.postValue("Mentor não encontrado");
            }
        });
    }


    public boolean isCurrentUserOwner() {
        Ideia ideiaAtual = _ideia.getValue();
        return ideiaAtual != null && authRepository.isCurrentUser(ideiaAtual.getOwnerId());
    }

    // --- LÓGICA DE MATCHMAKING (PORTADA DO FIRESTOREHELPER) ---

    @SuppressLint("MissingPermission")
    public void procurarNovoMentor(@NonNull Context context, @Nullable Location userLocation) {
        Ideia ideiaAtual = _ideia.getValue();

        if (ideiaAtual == null) {
            _toastEvent.setValue(new Event<>("Erro: Nenhuma ideia carregada."));
            return;
        }

        Log.d(TAG, "procurarNovoMentor: Iniciando processo de matching...");
        _matchProgressMessage.postValue("Preparando busca por mentores...");

        // 🔹 Se não há localização nem da ideia nem do usuário, busca sem proximidade
        Location ideiaLocation = ideiaAtual.getLocalizacao();
        if (ideiaLocation == null && userLocation == null) {
            Log.d(TAG, "Sem localização disponível — buscando por afinidade de áreas.");
            _matchLoading.postValue(true);
            buscarMentoresCompativeis(ideiaAtual, null);
            return;
        }

        // 🔹 Caso haja uma ou mais localizações disponíveis, perguntar ao usuário qual usar
        _matchProgressMessage.postValue("Selecionando fonte de localização...");
        _matchLocationRequest.postValue(new Event<>(new MatchLocationChoiceRequest(ideiaLocation, userLocation)));
    }

    public void handleMatchLocationChoice(@NonNull String choice, @Nullable Location chosenLocation, boolean iniciarBusca) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) {
            _toastEvent.postValue(new Event<>("Erro: Nenhuma ideia ativa para match."));
            return;
        }

        Log.d(TAG, "handleMatchLocationChoice: escolha=" + choice + ", iniciarBusca=" + iniciarBusca);
        if (!iniciarBusca) return;
        _matchLoading.postValue(true);
        _matchProgressMessage.postValue("Buscando mentores próximos...");
        buscarMentoresCompativeis(ideiaAtual, chosenLocation);
    }

    // closeOnSuccess indica se, ao achar/vincular mentor, a tela deve ser fechada (true para fluxo de publicação normal, false para re-match)
    private void buscarMentoresPorLocalidadePrimeiro(@NonNull Ideia ideia, @Nullable Location baseLocation, boolean closeOnSuccess) {
        Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: iniciando. ideia=" + ideia.getId() + " baseLocation=" + (baseLocation==null?"null":(baseLocation.getLatitude()+","+baseLocation.getLongitude())));
        _matchProgressMessage.setValue("Buscando mentores próximos...");

        // 1) buscar mentores via userRepository (onde isMentor==true)
        userRepository.getMentores(result -> {
            if (!(result instanceof Result.Success)) {
                Log.e(TAG, "buscarMentoresPorLocalidadePrimeiro: erro ao obter mentores do userRepository.", (result instanceof Result.Error) ? ((Result.Error) result).error : null);
                _matchLoading.setValue(false);
                _matchResultEvent.postValue(new Event<>(new MatchResult(false, "Erro ao buscar mentores.")));
                return;
            }

            List<User> users = ((Result.Success<List<User>>) result).data;
            if (users == null || users.isEmpty()) {
                Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: nenhum usuário mentor retornado.");
                _matchLoading.setValue(false);
                publicarIdeiaSemMentor(ideia); // fallback
                return;
            }

            // Filtrar: isMentor==true e excluir autor
            List<User> mentorsFiltered = new ArrayList<>();
            for (User u : users) {
                if (u == null) continue;
                if (!u.isMentor()) continue;
                if (ideia.getOwnerId() != null && ideia.getOwnerId().equals(u.getId())) continue; // excluir dono
                // também opcional: checar status do perfil do mentor (ativoPublic)
                mentorsFiltered.add(u);
            }

            Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: candidatos após filtro inicial = " + mentorsFiltered.size());

            if (mentorsFiltered.isEmpty()) {
                Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: 0 candidatos após filtro -> publicar sem mentor");
                _matchLoading.setValue(false);
                publicarIdeiaSemMentor(ideia);
                return;
            }

            // Se baseLocation não é null, priorizar por proximidade: criar lista com distância calculada
            final List<Pair<User, Double>> candidatesWithDistance = new ArrayList<>();
            for (User u : mentorsFiltered) {
                double distMeters = Double.MAX_VALUE;
                try {
                    // assume que o mentorData (Mentor) tem latitude/longitude armazenados no User -> user.getMentorData().getLatitude()
                    if (u.getMentorData() != null && u.getMentorData().getLatitude() != 0 && u.getMentorData().getLongitude() != 0 && baseLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(baseLocation.getLatitude(), baseLocation.getLongitude(),
                                u.getMentorData().getLatitude(), u.getMentorData().getLongitude(), results);
                        distMeters = results[0];
                    }
                } catch (Exception e) {
                    Log.w(TAG, "buscarMentoresPorLocalidadePrimeiro: erro ao calcular distancia para userId=" + u.getId() + " -> " + e.getMessage());
                }
                candidatesWithDistance.add(new Pair<>(u, distMeters));
            }

            // Ordena por distância crescente
            Collections.sort(candidatesWithDistance, Comparator.comparingDouble(p -> p.second));

            // Defina um raio inicial (ex.: 50km = 50_000m). Se nenhum dentro do raio, aumenta para 200km etc.
            final double INITIAL_RADIUS = 50_000; // metros
            final double WIDE_RADIUS = 200_000; // metros

            List<User> withinRadius = new ArrayList<>();
            for (Pair<User, Double> p : candidatesWithDistance) {
                if (p.second <= INITIAL_RADIUS) withinRadius.add(p.first);
            }
            if (withinRadius.isEmpty()) {
                for (Pair<User, Double> p : candidatesWithDistance) {
                    if (p.second <= WIDE_RADIUS) withinRadius.add(p.first);
                }
            }
            // Se ainda vazio, considerar todos (busca global)
            if (withinRadius.isEmpty()) {
                Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: nenhum mentor dentro do raio. Considerando todos os candidatos para ordenação por afinidade.");
                for (Pair<User, Double> p : candidatesWithDistance) withinRadius.add(p.first);
            } else {
                Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: candidatos dentro do raio = " + withinRadius.size());
            }

            // Agora, se há mais de 1 candidato -> ordenar por afinidade de áreas (intersecção)
            if (withinRadius.size() > 1) {
                _matchProgressMessage.postValue("Ordenando por afinidade de áreas...");
                // calcular afinidade: quantas áreas em comum entre mentor.getAreasDeInteresse() e ideia.getAreasNecessarias()
                withinRadius.sort((u1, u2) -> {
                    int score1 = calcularAfinidade(u1.getAreasDeInteresse(), ideia.getAreasNecessarias());
                    int score2 = calcularAfinidade(u2.getAreasDeInteresse(), ideia.getAreasNecessarias());
                    // maior afinidade primeiro; se empate, manter ordem anterior (que é por proximidade)
                    int cmp = Integer.compare(score2, score1);
                    return (cmp != 0) ? cmp : 0;
                });

                Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: ordenado por afinidade.");
                // Se houver mais de um candidato, enviar evento para UI permitir escolher
                if (withinRadius.size() > 1) {
                    Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: disparando evento de escolha com " + withinRadius.size() + " candidatos.");
                    _matchCandidatesEvent.postValue(new Event<>(withinRadius));
                    _matchLoading.setValue(false); // UI exibirá escolha e continuará o fluxo
                    return;
                }
            }

            // Se chegou aqui e há exatamente 1 candidato (ou consideramos todos -> 1),
            // vincular automaticamente.
            if (!withinRadius.isEmpty()) {
                User escolhido = withinRadius.get(0);
                Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: mentor escolhido automaticamente -> " + escolhido.getId());
                // chama vincular — passa closeOnSuccess para decidir se fecha a tela
                vincularMelhorMentor(ideia, escolhido, "Auto-match (localidade primeiro)");
                return;
            }

            // Safety fallback
            Log.d(TAG, "buscarMentoresPorLocalidadePrimeiro: fluxo inesperado, publicando sem mentor.");
            _matchLoading.setValue(false);
            publicarIdeiaSemMentor(ideia);
        });
    }

    // --- Helper: calcular afinidade entre duas listas de areas ---
    private int calcularAfinidade(@Nullable List<String> mentorAreas, @Nullable List<String> ideiaAreas) {
        if (mentorAreas == null || ideiaAreas == null || mentorAreas.isEmpty() || ideiaAreas.isEmpty()) return 0;
        java.util.Set<String> s1 = new java.util.HashSet<>();
        for (String s : mentorAreas) if (s != null) s1.add(s.trim().toLowerCase());
        int common = 0;
        for (String s : ideiaAreas) {
            if (s != null && s1.contains(s.trim().toLowerCase())) common++;
        }
        return common;
    }

    private void buscarMentoresCompativeis(@NonNull Ideia ideia, @Nullable Location location) {
        Log.d(TAG, "Iniciando busca de mentores compatíveis...");

        _matchLoading.postValue(true);
        _matchProgressMessage.postValue("Buscando mentores próximos à localização...");

        List<String> areasDaIdeia = ideia.getAreasNecessarias() != null ?
                ideia.getAreasNecessarias() : new ArrayList<>();

        // 🔹 1. Busca todos os mentores registrados no Firestore
        mentorRepository.getAllMentores(new ResultCallback<List<User>>() {
            @Override
            public void onResult(Result<List<User>> result) {
                if (result instanceof Result.Success) {
                    List<User> mentores = ((Result.Success<List<User>>) result).data;
                    Log.d(TAG, "Total de mentores retornados: " + mentores.size());

                    if (mentores == null || mentores.isEmpty()) {
                        Log.w(TAG, "Nenhum mentor encontrado no Firestore.");
                        publicarIdeiaSemMentor(ideia);
                        return;
                    }

                    // 🔹 Filtra mentores válidos (ativos, públicos e não o dono da ideia)
                    List<User> mentoresValidos = new ArrayList<>();
                    for (User user : mentores) {
                        if (user == null) continue;
                        if (!user.isMentor()) continue;
                        if (ideia.getOwnerId() != null && ideia.getOwnerId().equals(user.getId())) continue;

                        Mentor mentorData = user.getMentorData();
                        if (mentorData == null) continue;
                        if (!mentorData.isActivePublic()) continue; // apenas mentores ativos

                        mentoresValidos.add(user);
                    }

                    if (mentoresValidos.isEmpty()) {
                        Log.w(TAG, "Nenhum mentor ativo e público disponível.");
                        publicarIdeiaSemMentor(ideia);
                        return;
                    }

                    // 🔹 Ordena por afinidade + proximidade
                    List<User> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(
                            mentoresValidos,
                            areasDaIdeia,
                            location != null ? location : getIdeiaLocation(ideia)
                    );

                    if (ordenados.isEmpty()) {
                        Log.w(TAG, "Nenhum mentor qualificado após ordenação.");
                        publicarIdeiaSemMentor(ideia);
                        return;
                    }

                    // 🔹 Escolhe o melhor candidato
                    User melhor = ordenados.get(0);

                    double distancia = MentorMatchService.calcularDistancia(
                            melhor,
                            location != null ? location : getIdeiaLocation(ideia)
                    );

                    String nomeMentor = (melhor.getNome() != null) ? melhor.getNome() : melhor.getId();
                    if (distancia == -1) {
                        Log.d(TAG, "Distância não calculada (mentor ou localização nula).");
                    } else {
                        Log.d(TAG, "Melhor mentor: " + nomeMentor + " (distância: " + String.format("%.2f", distancia) + "m)");
                    }

                    if (distancia < 100000) { // dentro de 100km
                        vincularMelhorMentor(ideia, melhor, "Mentor encontrado por proximidade.");
                    } else {
                        Log.d(TAG, "Nenhum mentor suficientemente próximo. Tentando busca por áreas...");
                        buscarMentoresPorArea(ideia, areasDaIdeia, location);
                    }

                } else if (result instanceof Result.Error) {
                    Exception e = ((Result.Error<?>) result).error;
                    Log.e(TAG, "Erro ao buscar mentores: " + (e != null ? e.getMessage() : "erro desconhecido"));
                    publicarIdeiaSemMentor(ideia);
                }
            }
        });
    }



    /**
     * Busca mentores por área (fallback quando não há proximidade).
     */
    private void buscarMentoresPorArea(@NonNull Ideia ideia, @NonNull List<String> areas, @Nullable Location location) {
        _matchProgressMessage.postValue("Buscando mentores por áreas de interesse...");

        mentorRepository.findMentoresByAreas(areas, ideia.getOwnerId(), result -> {
            if (result instanceof Result.Success && !((Result.Success<List<Mentor>>) result).data.isEmpty()) {
                List<Mentor> mentores = ((Result.Success<List<Mentor>>) result).data;

                // 🔹 Converte cada Mentor para User (mantendo compatibilidade com o serviço de matching)
                List<User> users = new ArrayList<>();
                for (Mentor m : mentores) {
                    if (m == null) continue;

                    User u = new User();
                    u.setId(m.getId()); // mesmo ID do mentor
                    u.setMentorData(m); // anexa dados de localização, disponibilidade etc.
                    users.add(u);
                }

                if (users.isEmpty()) {
                    Log.w(TAG, "Nenhum mentor convertido para usuário. Abortando vinculação.");
                    publicarIdeiaSemMentor(ideia);
                    return;
                }

                // 🔹 Ordena por afinidade + proximidade
                List<User> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(
                        users, areas, location
                );

                if (ordenados.isEmpty()) {
                    Log.w(TAG, "Nenhum mentor qualificado após ordenação. Publicando sem mentor.");
                    publicarIdeiaSemMentor(ideia);
                    return;
                }

                // 🔹 Escolhe o melhor candidato (primeiro da lista)
                User melhor = ordenados.get(0);
                String nomeMentor = (melhor.getNome() != null) ? melhor.getNome() : melhor.getId();

                Log.d(TAG, "Mentor selecionado por área: " + nomeMentor);

                // 🔹 Vincula e prossegue com fluxo normal
                vincularMelhorMentor(ideia, melhor, "Mentor encontrado por área.");

            } else {
                Log.w(TAG, "Nenhum mentor encontrado por área. Publicando sem mentor.");
                publicarIdeiaSemMentor(ideia);
            }
        });
    }


    /**
     * Recupera a localização armazenada na própria ideia.
     */
    @Nullable
    private Location getIdeiaLocation(@NonNull Ideia ideia) {
        if (ideia.getLatitude() == null || ideia.getLongitude() == null) return null;
        Location l = new Location("ideia");
        l.setLatitude(ideia.getLatitude());
        l.setLongitude(ideia.getLongitude());
        return l;
    }


    /**
     * Encapsula a busca de mentores compatíveis (versão para re-match ou match inicial)
     */
    private void buscarMentoresCompativeis(@Nullable Location location, boolean isRematch) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        _matchProgressMessage.postValue("Buscando mentores...");

        List<String> areasDaIdeia = Objects.requireNonNull(ideia.getValue()).getAreasNecessarias() != null ?
                ideia.getValue().getAreasNecessarias() : new ArrayList<>();


        mentorRepository.getAllMentores(new ResultCallback<List<User>>() {
            @Override
            public void onResult(Result<List<User>> result) {
                if (result instanceof Result.Success) {
                    List<User> mentores = ((Result.Success<List<User>>) result).data;

                    if (mentores == null || mentores.isEmpty()) {
                        Log.w(TAG, "Nenhum mentor encontrado no Firestore.");
                        publicarIdeiaSemMentor(Objects.requireNonNull(ideia.getValue()));
                        return;
                    }

                    List<User> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(
                            mentores, areasDaIdeia, location != null ? location : getIdeiaLocation(Objects.requireNonNull(ideia.getValue()))
                    );

                    if (ordenados.isEmpty()) {
                        publicarIdeiaSemMentor(Objects.requireNonNull(ideia.getValue()));
                        return;
                    }

                    User melhor = ordenados.get(0);
                    double distancia = MentorMatchService.calcularDistancia(melhor, location);

                    if (distancia < 100000) {
                        vincularMelhorMentor(ideia.getValue(), melhor, "Mentor encontrado por proximidade.");
                    } else {
                        buscarMentoresPorArea(ideia.getValue(), areasDaIdeia, location);
                    }

                } else if (result instanceof Result.Error) {
                    Exception e = ((Result.Error<?>) result).error;
                    Log.e(TAG, "Erro ao buscar mentores: " + (e != null ? e.getMessage() : "erro desconhecido"));
                    publicarIdeiaSemMentor(Objects.requireNonNull(ideia.getValue()));
                }
            }
        });
    }

    /**
     * Salva a ideia com a localização atual (se houver) e inicia o processo de match.
     */
    public void publicarIdeiaComLocalizacaoAtualizada(@NonNull Context context, @NonNull Ideia ideia, @Nullable Location location) {
        _isLoading.postValue(true);
        _matchProgressMessage.postValue("Publicando ideia...");

        Log.d(TAG, "Salvando ideia com localização antes do match...");

        ideiaRepository.saveIdeia(ideia, result -> {
            if (result instanceof Result.Success) {
                Log.d(TAG, "Ideia salva com sucesso. Iniciando busca de mentores...");
                _matchProgressMessage.postValue("Buscando mentores próximos...");

                _matchProgressMessage.postValue("Selecionando localização...");
                _matchLocationRequest.postValue(new Event<>(new MatchLocationChoiceRequest(
                        ideia.getLocalizacao(), // localização da ideia, se houver
                        location                // localização atual do usuário, se houver
                )));

            } else {
                Exception e = ((Result.Error<?>) result).error;
                Log.e(TAG, "Erro ao salvar ideia: " + (e != null ? e.getMessage() : "erro desconhecido"));
                _toastEvent.postValue(new Event<>("Falha ao salvar a ideia antes da publicação."));
                _isLoading.postValue(false);
            }
        });
    }



    /**
     * Vincula um mentor à ideia e controla a UI conforme o tipo de match.
     */
    private void vincularMentor(Ideia ideia, User mentor, boolean isRematch) {
        ideia.setMentorId(mentor.getId());
        ideia.setStatus(Ideia.Status.EM_AVALIACAO);

        ideiaRepository.publicarIdeia(ideia.getId(), mentor.getId(), result -> {
            _isLoading.postValue(false);
            _isRematching.postValue(false);

            if (result instanceof Result.Success) {
                _toastEvent.postValue(new Event<>("Mentor vinculado com sucesso: " + mentor.getNome()));

                if (!isRematch) {
                    // Match inicial: fecha a tela
                    _closeScreenEvent.postValue(new Event<>(true));
                }
            } else {
                _toastEvent.postValue(new Event<>("Erro ao vincular o mentor."));
            }
        });
    }


    private void buscarTodosOsMentores(@NonNull Ideia ideia, @Nullable Location location, @NonNull List<String> areasDaIdeia) {
        _matchProgressMessage.postValue("Buscando mentores disponíveis...");
        Log.d(TAG, "buscarTodosOsMentores: buscando sem filtro de área...");

        userRepository.getMentores(result -> {
            if (result instanceof Result.Success) {
                List<User> mentores = ((Result.Success<List<User>>) result).data;
                List<User> candidatos = new ArrayList<>();

                for (User u : mentores) {
                    if (u == null || !u.isMentor()) continue;
                    if (ideia.getOwnerId() != null && ideia.getOwnerId().equals(u.getId())) continue;
                    candidatos.add(u);
                }

                if (!candidatos.isEmpty()) {
                    List<User> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(candidatos, areasDaIdeia, location);
                    _matchCandidatesEvent.postValue(new Event<>(ordenados));
                    _matchProgressMessage.postValue("Mentores sugeridos por proximidade.");
                    _matchLoading.postValue(false);
                    return;
                }
            }

            Log.w(TAG, "Nenhum mentor encontrado. Publicando sem mentor.");
            publicarIdeiaSemMentor(ideia);
        });
    }

    private void vincularMelhorMentor(Ideia ideia, @NonNull User userMentor, @NonNull String origemLog) {
        Log.d(TAG, "vincularMelhorMentor: tentativa de vincular userMentorId=" + userMentor.getId() + " origem=" + origemLog);
        if (userMentor.getId() == null || userMentor.getId().equals(ideia.getOwnerId())) {
            Log.w(TAG, "vincularMelhorMentor: abortando - mentor inválido ou é o próprio autor.");
            publicarIdeiaSemMentor(ideia);
            return;
        }

        ideia.setMentorId(userMentor.getId());
        ideia.setStatus(Ideia.Status.EM_AVALIACAO);

        // show progress
        _matchLoading.setValue(true);
        _matchProgressMessage.setValue("Vinculando mentor...");

        ideiaRepository.publicarIdeia(ideia.getId(), userMentor.getId(), result -> {
            _matchLoading.postValue(false);
            if (result instanceof Result.Success) {
                Log.i(TAG, "vincularMelhorMentor: sucesso ao vincular mentor " + userMentor.getId());
                _matchResultEvent.postValue(new Event<>(new MatchResult(true, "Mentor vinculado: " + (userMentor.getNome() != null ? userMentor.getNome() : userMentor.getId()))));
                // se closeOnSuccess == true, UI pode observar matchResultEvent e fechar a tela.
                _matchResultEvent.postValue(new Event<>(new MatchResult(true, "Mentor vinculado!")));
            } else {
                Exception e = ((Result.Error<Void>) result).error;
                Log.e(TAG, "vincularMelhorMentor: erro ao vincular mentor -> " + (e != null ? e.getMessage() : "null"));
                _matchResultEvent.postValue(new Event<>(new MatchResult(false, "Erro ao vincular mentor.")));
            }
        });
    }

    /**
     * Publica a ideia no estado EM_AVALIACAO, mas sem um mentor vinculado.
     * Isso permite que o usuário procure um mentor mais tarde.
     */
    private void publicarIdeiaSemMentor(Ideia ideia) {
        ideia.setStatus(Ideia.Status.EM_AVALIACAO);
        ideia.setMentorId(null);

        _matchProgressMessage.postValue("Publicando ideia sem mentor...");
        Log.d(TAG, "publicarIdeiaSemMentor: Publicando...");

        ideiaRepository.publicarIdeia(ideia.getId(), null, result -> {
            _matchLoading.postValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.postValue(new Event<>("Ideia publicada! Nenhum mentor encontrado."));
            } else {
                _toastEvent.postValue(new Event<>("Erro ao publicar ideia."));
            }
        });
    }

    /**
     * Verifica se o usuário atualmente logado é o mentor designado para a ideia carregada.
     * @return true se o usuário for o mentor, false caso contrário.
     */
    public boolean isCurrentUserTheMentor() {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getMentorId() == null) return false;
        return authRepository.isCurrentUser(ideiaAtual.getMentorId());
    }

    // --- Ações de UI ---

    public void saveAndFinish() {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) {
            _closeScreenEvent.setValue(new Event<>(true));
            return;
        }

        // Apenas processa o salvamento se a ideia for um rascunho.
        if (ideiaAtual.getStatus() == Ideia.Status.RASCUNHO) {
            // Verifica se é uma ideia que já existe no Firestore (não é nova)
            // A lógica aqui é: uma ideia que já foi ouvida pelo listener não é "nova".
            // Para ser mais robusto, poderíamos ter uma flag `isNew`.
            // Por enquanto, vamos assumir que se o listener não for nulo, a ideia existe.
            boolean isExistingDraft = (ideiaListener != null);

            // Validação de conteúdo mínimo
            boolean hasTitle = ideiaAtual.getNome() != null && !ideiaAtual.getNome().trim().isEmpty();
            boolean hasDescription = ideiaAtual.getDescricao() != null && !ideiaAtual.getDescricao().trim().isEmpty();
            boolean hasAreas = ideiaAtual.getAreasNecessarias() != null && !ideiaAtual.getAreasNecessarias().isEmpty();

            // A variável agora checa se os 3 requisitos são verdadeiros
            boolean meetsMinimumRequirements = hasTitle && hasDescription && hasAreas;

            if (!isExistingDraft && !meetsMinimumRequirements) {
                // Se for um RASCUNHO NOVO e SEM CONTEÚDO, descarta.
                _toastEvent.setValue(new Event<>("Rascunho vazio descartado."));
                _closeScreenEvent.setValue(new Event<>(true));
                return; // Interrompe a execução antes de salvar.
            }

            // Se for um rascunho existente OU um novo com conteúdo, salva.
            ideiaRepository.saveIdeia(ideiaAtual, result -> {
                if (result instanceof Result.Success) {
                    _toastEvent.setValue(new Event<>("Rascunho salvo!"));
                } else {
                    _toastEvent.setValue(new Event<>("Erro ao salvar rascunho."));
                }
                _closeScreenEvent.setValue(new Event<>(true));
            });
        } else {
            // Se não for um rascunho (apenas visualização), simplesmente fecha a tela.
            _closeScreenEvent.setValue(new Event<>(true));
        }
    }

    public void deletePostIt(@NonNull String etapaChave, @NonNull PostIt postItToDelete) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null || currentIdeia.getId() == null || etapaChave == null || postItToDelete == null) {
            Log.e(TAG, "deletePostIt: Tentativa de deletar com dados nulos.");
            _toastEvent.setValue(new Event<>("Erro interno ao tentar excluir post-it."));
            return;
        }
        // Só permite deletar se for rascunho
        if (currentIdeia.getStatus() != Ideia.Status.RASCUNHO) {
            _toastEvent.setValue(new Event<>("Não é possível excluir post-its de uma ideia publicada."));
            return;
        }

        _isLoading.setValue(true);

        ideiaRepository.deletePostitFromIdeia(currentIdeia.getId(), etapaChave, postItToDelete, new ResultCallback<Void>() {
            @Override
            public void onResult(Result<Void> result) {
                _isLoading.setValue(false);
                if (result instanceof Result.Error) {
                    _toastEvent.postValue(new Event<>("Erro ao excluir post-it: " + ((Result.Error<Void>)result).error.getMessage()));
                }
                // No sucesso, o listener do Firestore atualizará a UI.
            }
        });
    }

    private void garantirMembroIdealizador(Ideia ideia) {
        boolean deveTerEquipe = ideia.getStatus() == Ideia.Status.AVALIADA_APROVADA || ideia.getStatus() == Ideia.Status.AVALIADA_REPROVADA;
        boolean equipeVazia = ideia.getEquipe() == null || ideia.getEquipe().isEmpty();

        if (deveTerEquipe && equipeVazia) {
            authRepository.getUserProfile(ideia.getOwnerId(), result -> {
                if (result instanceof Result.Success) {
                    User owner = ((Result.Success<User>) result).data;
                    if (owner != null) {
                        MembroEquipe idealizador = new MembroEquipe(owner.getNome(), "Idealizador", "");
                        // Garante que a lista não seja nula antes de adicionar
                        if (ideia.getEquipe() == null) {
                            ideia.setEquipe(new ArrayList<>());
                        }
                        ideia.getEquipe().add(idealizador);
                        _ideia.postValue(ideia); // Usa postValue pois está em callback
                    } else {
                        Log.e(TAG, "garantirMembroIdealizador: Dono da ideia (User) retornado como nulo.");
                    }
                } else {
                    Log.e(TAG, "garantirMembroIdealizador: Falha ao buscar perfil do dono da ideia.");
                    // Não emitir a ideia aqui, pois falhou em obter o dono
                }
            });
        }
    }

    /**
     * Força o salvamento dos dados atuais da ideia no repositório.
     * Útil antes de ações críticas como 'publicar' ou 'solicitar IA'.
     */
    private void salvarCanvasAtual() {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        // Só salva se for um rascunho
        if (ideiaAtual.getStatus() == Ideia.Status.RASCUNHO) {
            Log.d(TAG, "salvarCanvasAtual: Forçando salvamento da ideia ID: " + ideiaAtual.getId());
            ideiaRepository.saveIdeia(ideiaAtual, result -> {
                if (result instanceof Result.Error) {
                    Log.e(TAG, "salvarCanvasAtual: Falha ao salvar.", ((Result.Error<Void>) result).error);
                } else {
                    Log.d(TAG, "salvarCanvasAtual: Salvo com sucesso.");
                    // Garante que o listener está ativo após o primeiro save
                    garantirListenerAtivo();
                }
            });
        }
    }

    /**
     * Solicita que o backend gere uma análise de IA para a ideia atual.
     */
    public void solicitarAnaliseIA() {
        // <<< CORREÇÃO 1 (Continuação): Usa o campo `this.ideiaId`
        if (this.ideiaId == null) {
            _toastEvent.setValue(new Event<>("Erro: ID da ideia não encontrado."));
            return;
        }

        // Garante que os dados mais recentes foram salvos antes de analisar
        // <<< CORREÇÃO 3 (Continuação): Agora este método existe
        salvarCanvasAtual();

        _isIaLoading.setValue(true); // <<< CORREÇÃO 2 (Continuação): Usa o LiveData correto

        ideiaRepository.solicitarAnaliseIA(this.ideiaId, result -> {
            _isIaLoading.setValue(false); // <<< CORREÇÃO 2 (Continuação): Usa o LiveData correto

            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Análise da IA solicitada! Os resultados aparecerão em breve."));
                // O listener 'listenToIdeia' (que já existe no seu loadIdeia)
                // irá automaticamente capturar a atualização no campo 'avaliacaoIA'
                // e atualizar o LiveData '_ideia'.
            } else {
                String errorMsg = "Erro ao solicitar análise.";
                if (result instanceof Result.Error) {
                    Exception e = ((Result.Error<String>) result).error;
                    if (e != null) {
                        // Tenta extrair a mensagem de erro da Firebase Function
                        String functionError = e.getMessage();
                        if(functionError != null && functionError.contains("PERMISSION_DENIED")) {
                            errorMsg = "Você não é o dono desta ideia.";
                        } else if (functionError != null && functionError.contains("NOT_FOUND")) {
                            errorMsg = "Ideia não encontrada no servidor.";
                        } else if(e.getMessage() != null) {
                            errorMsg = e.getMessage();
                        }
                    }
                }
                _toastEvent.setValue(new Event<>(errorMsg));
            }
        });
    }

    public void confirmarEscolhaDeMentor(@NonNull User userMentor, boolean fecharTela) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) {
            _toastEvent.postValue(new Event<>("Erro: Nenhuma ideia carregada."));
            return;
        }

        Log.d(TAG, "confirmarEscolhaDeMentor: Vinculando mentor " + userMentor.getId());
        _matchLoading.postValue(true);
        _matchProgressMessage.postValue("Vinculando mentor à ideia...");

        if (userMentor.getId().equals(ideiaAtual.getOwnerId())) {
            Log.w(TAG, "Mentor é o próprio dono da ideia. Abortando vínculo.");
            publicarIdeiaSemMentor(ideiaAtual);
            return;
        }

        ideiaAtual.setMentorId(userMentor.getId());
        ideiaAtual.setStatus(Ideia.Status.EM_AVALIACAO);

        ideiaRepository.publicarIdeia(ideiaAtual.getId(), userMentor.getId(), result -> {
            _matchLoading.postValue(false);
            if (result instanceof Result.Success) {
                Log.i(TAG, "Mentor vinculado com sucesso: " + userMentor.getNome());
                _toastEvent.postValue(new Event<>("Mentor encontrado: " + userMentor.getNome()));
                if (fecharTela) _closeScreenEvent.postValue(new Event<>(true));
            } else {
                Log.e(TAG, "Erro ao vincular mentor.", ((Result.Error<Void>) result).error);
                _toastEvent.postValue(new Event<>("Erro ao vincular o mentor. Tente novamente."));
            }
        });
    }

    /**
     * Inicia o processo de re-match de mentor sem fechar a tela.
     */
    public void iniciarReMatch(@NonNull Context context, @Nullable Location location) {
        Log.d(TAG, "Iniciando re-match...");
        _isRematching.postValue(true);
        _rematchMessage.postValue("Buscando novos mentores compatíveis...");

        // Simula progressão visual
        handler.postDelayed(() -> _rematchMessage.postValue("Analisando áreas de atuação..."), 2000);
        handler.postDelayed(() -> _rematchMessage.postValue("Avaliando proximidade geográfica..."), 4000);
        handler.postDelayed(() -> _rematchMessage.postValue("Finalizando correspondência..."), 6000);

        // Chama o mesmo método de busca, mas sem fechar a tela no final
        buscarMentoresCompativeis(location, true);
    }

    /**
     * Cancela o re-match se estiver em andamento.
     */
    public void cancelarReMatch() {
        Log.d(TAG, "Re-match cancelado pelo usuário.");
        _isRematching.postValue(false);
        _rematchMessage.postValue("Busca cancelada.");
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // Garante que o listener seja removido para evitar memory leaks
        if (ideiaListener != null) {
            ideiaListener.remove();
        }
    }
}