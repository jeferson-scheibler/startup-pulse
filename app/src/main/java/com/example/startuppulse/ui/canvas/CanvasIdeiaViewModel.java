package com.example.startuppulse.ui.canvas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.R;
import com.example.startuppulse.data.CanvasEtapa;
import com.example.startuppulse.data.MembroEquipe;
import com.example.startuppulse.data.Mentor;
import com.example.startuppulse.MentorMatchService;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.IdeiaRepository;
import com.example.startuppulse.data.MentorRepository;
import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.data.User;
import com.example.startuppulse.util.Event;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CanvasIdeiaViewModel extends ViewModel {

    private static final String TAG = "CanvasViewModel_DEBUG";

    // --- Repositórios Injetados ---
    private final IdeiaRepository ideiaRepository;
    private final MentorRepository mentorRepository;
    private final AuthRepository authRepository;

    private ListenerRegistration ideiaListener;

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

    // --- Eventos para Ações Únicas ---
    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public final LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _closeScreenEvent = new MutableLiveData<>();
    public final LiveData<Event<Boolean>> closeScreenEvent = _closeScreenEvent;
    public final LiveData<Boolean> isPublishEnabled;

    private final List<String> etapasObrigatorias = Arrays.asList(
            CanvasEtapa.CHAVE_PROPOSTA_VALOR, CanvasEtapa.CHAVE_SEGMENTO_CLIENTES,
            CanvasEtapa.CHAVE_CANAIS, CanvasEtapa.CHAVE_RELACIONAMENTO_CLIENTES,
            CanvasEtapa.CHAVE_FONTES_RENDA, CanvasEtapa.CHAVE_RECURSOS_PRINCIPAIS,
            CanvasEtapa.CHAVE_ATIVIDADES_CHAVE, CanvasEtapa.CHAVE_PARCERIAS_PRINCIPAIS,
            CanvasEtapa.CHAVE_ESTRUTURA_CUSTOS
    );

    @Inject
    public CanvasIdeiaViewModel(IdeiaRepository ideiaRepository, MentorRepository mentorRepository, AuthRepository authRepository) {
        this.ideiaRepository = ideiaRepository;
        this.mentorRepository = mentorRepository;
        this.authRepository = authRepository;
        isPublishEnabled = Transformations.map(_ideia, this::isIdeiaValidaParaPublicar);
    }

    public void loadIdeia(@Nullable String ideiaId) {
        Log.d(TAG, "loadIdeia: Método chamado com ideiaId = " + ideiaId);

        if (ideiaListener != null) {
            ideiaListener.remove();
        }
        _isLoading.setValue(true);

        if (ideiaId == null) {
            Log.d(TAG, "loadIdeia: ID é nulo. Criando uma nova ideia em branco.");
            Ideia novaIdeia = new Ideia();
            novaIdeia.setId(ideiaRepository.getNewIdeiaId());
            novaIdeia.setOwnerId(authRepository.getCurrentUserId());
            _ideia.setValue(novaIdeia);
            setupEtapas(novaIdeia);
            _isLoading.setValue(false);
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
                    }
                } else if (result instanceof Result.Error) {
                    Log.e(TAG, "listenToIdeia Callback: Erro ao carregar ideia.", ((Result.Error<Ideia>) result).error);
                    _toastEvent.setValue(new Event<>("Erro ao carregar ideia: " + ((Result.Error<Ideia>) result).error.getMessage()));
                    _closeScreenEvent.setValue(new Event<>(true));
                }
            });
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

        PostIt novoPostIt = new PostIt(texto, cor, new Date());
        // O ID do PostIt pode ser gerado aqui ou no repositório, se necessário

        ideiaRepository.addPostitToIdeia(ideiaAtual.getId(), etapaChave, novoPostIt, result -> {
            if (result instanceof Result.Error) {
                _toastEvent.postValue(new Event<>("Erro ao adicionar post-it."));
            }
            // A UI será atualizada automaticamente pelo listener do Firestore
        });
    }

    public void updatePostIt(@NonNull String etapaChave, @NonNull PostIt postitAntigo, @NonNull String novoTexto, @NonNull String novaCor) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) {
            _toastEvent.setValue(new Event<>("Erro: Ideia não encontrada para atualizar post-it."));
            return;
        }

        // Cria o novo objeto PostIt com os dados atualizados, mantendo o timestamp original
        PostIt postitNovo = new PostIt(novoTexto, novaCor, postitAntigo.getTimestamp());
        postitNovo.setLastModified(new Date());

        ideiaRepository.updatePostitInIdeia(ideiaAtual.getId(), etapaChave, postitAntigo, postitNovo, result -> {
            if (result instanceof Result.Error) {
                _toastEvent.postValue(new Event<>("Erro ao atualizar post-it."));
            }
            // A UI será atualizada automaticamente pelo listener do Firestore
        });
    }

    public void updateIdeiaBasics(String nome, String descricao, List<String> areas) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) return;

        // Atualiza os campos do objeto de ideia atual
        ideiaAtual.setNome(nome);
        ideiaAtual.setDescricao(descricao); // Usando o campo 'bio' do novo modelo Mentor/Ideia
        ideiaAtual.setAreasNecessarias(areas);

        // Emite o objeto modificado. O LiveData notificará os observers.
        _ideia.setValue(ideiaAtual);
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
        mentorRepository.getMentorById(mentorId, result -> {
            if (result instanceof Result.Success) {
                _mentorNome.postValue(((Result.Success<Mentor>) result).data.getName());
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

    @SuppressLint("MissingPermission") // Permissão é checada no Fragment antes de chamar
    public void procurarNovoMentor(@NonNull Context context, @Nullable Location userLocation) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null) {
            _toastEvent.setValue(new Event<>("Ideia não está carregada."));
            return;
        }

        _isLoading.setValue(true); // Mostra loading para o usuário
        buscarMentoresCompativeis(context, ideiaAtual, userLocation);
    }


    private void buscarMentoresCompativeis(@NonNull Context context, @NonNull Ideia ideia, @Nullable Location location) {
        final List<String> areasDaIdeia = ideia.getAreasNecessarias() != null ? ideia.getAreasNecessarias() : new ArrayList<>();

        mentorRepository.findMentoresByAreas(areasDaIdeia, ideia.getOwnerId(), result -> {
            if (result instanceof Result.Success && !((Result.Success<List<Mentor>>) result).data.isEmpty()) {
                List<Mentor> mentores = ((Result.Success<List<Mentor>>) result).data;
                List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(mentores, areasDaIdeia, location);
                vincularMelhorMentor(ideia, ordenados.get(0), "Mentor encontrado por área.");
            } else {
                buscarMentoresPorProximidade(context, ideia, location, areasDaIdeia);
            }
        });
    }

    private void buscarMentoresPorProximidade(@NonNull Context context, @NonNull Ideia ideia, @Nullable Location location, @NonNull List<String> areasDaIdeia) {
        if (location == null) {
            finalizarBuscaComErro("Nenhum mentor encontrado com as áreas desejadas.");
            return;
        }

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            // O Geocoder pode retornar uma lista vazia mesmo sem lançar exceção
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String cidade = addresses.get(0).getLocality();
                if (cidade != null) {
                    mentorRepository.findMentoresByCity(cidade, ideia.getOwnerId(), result -> {
                        if (result instanceof Result.Success && !((Result.Success<List<Mentor>>) result).data.isEmpty()) {
                            List<Mentor> mentores = ((Result.Success<List<Mentor>>) result).data;
                            List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(mentores, areasDaIdeia, location);
                            vincularMelhorMentor(ideia, ordenados.get(0), "Mentor encontrado por proximidade.");
                        } else {
                            finalizarBuscaComErro("Nenhum mentor encontrado na sua região.");
                        }
                    });
                    return; // Retorna para evitar cair no erro abaixo
                }
            }
            // Se 'addresses' for nulo ou vazio, consideramos um erro de geocoding
            finalizarBuscaComErro("Não foi possível determinar sua cidade a partir da localização.");

        } catch (IOException e) {
            finalizarBuscaComErro("Não foi possível determinar sua cidade. Verifique sua conexão.");
        }
    }

    private void finalizarBuscaComErro(String mensagem) {
        _isLoading.postValue(false);
        _toastEvent.postValue(new Event<>(mensagem));
        _ideia.postValue(_ideia.getValue());
    }

    private void vincularMelhorMentor(Ideia ideia, Mentor mentor, String log) {
        // Atualiza o objeto localmente primeiro para uma UI mais reativa
        ideia.setMentorId(mentor.getId());
        ideia.setStatus(Ideia.Status.EM_AVALIACAO);
        _ideia.setValue(ideia); // Notifica a UI imediatamente

        ideiaRepository.publicarIdeia(ideia.getId(), mentor.getId(), result -> {
            _isLoading.postValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.postValue(new Event<>("Novo mentor encontrado: " + mentor.getName()));
            } else {
                _toastEvent.postValue(new Event<>("Erro ao vincular novo mentor."));
                // Opcional: Reverter o estado da UI se a escrita no banco falhar
            }
        });
    }

    /**
     * Verifica se o usuário atualmente logado é o mentor designado para a ideia carregada.
     * @return true se o usuário for o mentor, false caso contrário.
     */
    public boolean isCurrentUserTheMentor() {
        Ideia ideiaAtual = _ideia.getValue();
        // Retorna falso se a ideia não estiver carregada ou se não tiver mentor
        if (ideiaAtual == null || ideiaAtual.getMentorId() == null) {
            return false;
        }
        // Delega a verificação para o AuthRepository
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
            boolean hasContent = (ideiaAtual.getNome() != null && !ideiaAtual.getNome().trim().isEmpty()) ||
                    (ideiaAtual.getDescricao() != null && !ideiaAtual.getDescricao().trim().isEmpty()) ||
                    (ideiaAtual.getAreasNecessarias() != null && !ideiaAtual.getAreasNecessarias().isEmpty());

            if (!isExistingDraft && !hasContent) {
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

    public void deletePostIt(@NonNull String etapaChave, @NonNull PostIt postIt) {
        Ideia ideiaAtual = _ideia.getValue();
        if (ideiaAtual == null || ideiaAtual.getId() == null) {
            _toastEvent.setValue(new Event<>("Não é possível apagar o post-it. Salve a ideia primeiro."));
            return;
        }

        ideiaRepository.deletePostitFromIdeia(ideiaAtual.getId(), etapaChave, postIt, result -> {
            if (result instanceof Result.Error) {
                _toastEvent.postValue(new Event<>("Erro ao apagar post-it."));
            }
            // A UI será atualizada automaticamente pelo listener em tempo real, não precisamos fazer mais nada.
        });
    }

    private void garantirMembroIdealizador(Ideia ideia) {
        // Verifica se a ideia está em um status que exibe a equipe e se a equipe está vazia
        boolean deveTerEquipe = ideia.getStatus() == Ideia.Status.AVALIADA_APROVADA || ideia.getStatus() == Ideia.Status.AVALIADA_REPROVADA;
        boolean equipeVazia = ideia.getEquipe() == null || ideia.getEquipe().isEmpty();

        if (deveTerEquipe && equipeVazia) {
            // Busca o perfil do usuário dono da ideia
            authRepository.getUserProfile(ideia.getOwnerId(), result -> {
                if (result instanceof Result.Success) {
                    User owner = ((Result.Success<User>) result).data;
                    if (owner != null) {
                        // Cria o membro padrão e adiciona à lista
                        MembroEquipe idealizador = new MembroEquipe(owner.getNome(), "Idealizador", "");
                        ideia.setEquipe(new ArrayList<>());
                        ideia.getEquipe().add(idealizador);
                        // Re-emite a ideia atualizada para que a UI reaja
                        _ideia.postValue(ideia);
                    }
                }
            });
        }
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