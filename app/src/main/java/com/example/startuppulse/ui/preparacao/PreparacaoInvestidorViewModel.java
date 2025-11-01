package com.example.startuppulse.ui.preparacao;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.domain.usercase.ReadinessCalculator;
import com.example.startuppulse.ReadinessData;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IdeiaRepository;
import com.example.startuppulse.data.models.MembroEquipe;
import com.example.startuppulse.data.models.Metrica;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.util.Event;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PreparacaoInvestidorViewModel extends ViewModel {

    private final IdeiaRepository ideiaRepository;
    private final AuthRepository authRepository;
    private final String ideiaId;

    // LiveData para o estado da UI
    private final MutableLiveData<Ideia> _ideia = new MutableLiveData<>();
    public final LiveData<Ideia> ideia = _ideia;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public final LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _navigationEvent = new MutableLiveData<>();
    public final LiveData<Event<Boolean>> navigationEvent = _navigationEvent;

    // NOVO: LiveData para os dados de prontidão
    private final MutableLiveData<ReadinessData> _readinessData = new MutableLiveData<>();
    public final LiveData<ReadinessData> readinessData = _readinessData;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable saveRunnable;

    private User cachedUserProfile = null;
    private Ideia cachedIdeia = null;


    @Inject
    public PreparacaoInvestidorViewModel(IdeiaRepository ideiaRepository, AuthRepository authRepository, SavedStateHandle savedStateHandle) {
        this.ideiaRepository = ideiaRepository;
        this.authRepository = authRepository;
        this.ideiaId = savedStateHandle.get("ideiaId");
        loadIdeia();
    }

    private void loadIdeia() {
        _isLoading.setValue(true);
        ideiaRepository.getIdeiaById(ideiaId, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                cachedIdeia = ((Result.Success<Ideia>) result).data;
                tryMergeOwnerAndIdeia();
            } else {
                _toastEvent.setValue(new Event<>("Falha ao carregar dados da ideia."));
            }
        });

        authRepository.fetchCurrentUserProfile(result -> {
            if (result instanceof Result.Success) {
                cachedUserProfile = ((Result.Success<User>) result).data;
                tryMergeOwnerAndIdeia();
            }
        });
    }

    private void tryMergeOwnerAndIdeia() {
        if (cachedIdeia != null && cachedUserProfile != null) {
            ensureOwnerInEquipe(cachedIdeia, cachedUserProfile);
            _ideia.setValue(cachedIdeia);
            calculateReadiness(cachedIdeia);
        }
    }

    private void ensureOwnerInEquipe(Ideia ideia, @Nullable User currentUser) {
        if (ideia == null) return;

        String currentUserId = authRepository.getCurrentUserId();
        if (currentUserId == null) return;

        List<MembroEquipe> equipe = ideia.getEquipe() != null ? ideia.getEquipe() : new ArrayList<>();

        boolean exists = false;
        for (MembroEquipe membro : equipe) {
            if (currentUserId.equals(membro.getUserId())) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            String nome = currentUser != null && currentUser.getNome() != null ? currentUser.getNome() : "Fundador";
            String linkedin = currentUser != null && currentUser.getLinkedinUrl() != null ? currentUser.getLinkedinUrl() : "";

            MembroEquipe dono = new MembroEquipe(nome, "Fundador(a) / Criador(a)", linkedin, currentUserId);

            equipe.add(0, dono);
            ideia.setEquipe(equipe);

            // Salva uma única vez
            ideiaRepository.updateIdeia(ideia, r -> {
                if (r instanceof Result.Error) {
                    _toastEvent.postValue(new Event<>("Erro ao incluir o fundador na equipe."));
                }
            });
        }
    }



    private void updateIdeiaAndRecalculate(Ideia ideia) {
        _ideia.setValue(ideia);
        calculateReadiness(ideia);
    }

    public void onMetricaEditada() {
        if (saveRunnable != null) handler.removeCallbacks(saveRunnable);
        saveRunnable = () -> salvarDados(false); // salvar silencioso
        handler.postDelayed(saveRunnable, 1500);
    }

    // NOVO: Método que chama a classe de cálculo e atualiza o LiveData
    private void calculateReadiness(Ideia ideia) {
        if (ideia == null) return;
        ReadinessData data = ReadinessCalculator.calculate(ideia);
        _readinessData.setValue(data);
    }

    public void adicionarMembro(String nome, String funcao, String linkedin) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null) return;

        MembroEquipe novoMembro = new MembroEquipe(nome, funcao, linkedin);
        List<MembroEquipe> equipe = new ArrayList<>(currentIdeia.getEquipe() != null ? currentIdeia.getEquipe() : new ArrayList<>());
        equipe.add(novoMembro);
        currentIdeia.setEquipe(equipe);
        updateIdeiaAndRecalculate(currentIdeia); // ATUALIZADO
    }

    public void removerMembro(MembroEquipe membro) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null || currentIdeia.getEquipe() == null) return;

        List<MembroEquipe> equipe = new ArrayList<>(currentIdeia.getEquipe());
        equipe.remove(membro);
        currentIdeia.setEquipe(equipe);
        updateIdeiaAndRecalculate(currentIdeia); // ATUALIZADO
    }

    public void adicionarMetrica() {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null) return;

        Metrica novaMetrica = new Metrica("Nova Métrica", "0");
        List<Metrica> metricas = new ArrayList<>(currentIdeia.getMetricas() != null ? currentIdeia.getMetricas() : new ArrayList<>());
        metricas.add(novaMetrica);
        currentIdeia.setMetricas(metricas);
        updateIdeiaAndRecalculate(currentIdeia); // ATUALIZADO
    }

    public void removerMetrica(Metrica metrica) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null || currentIdeia.getMetricas() == null) return;

        List<Metrica> metricas = new ArrayList<>(currentIdeia.getMetricas());
        metricas.remove(metrica);
        currentIdeia.setMetricas(metricas);
        updateIdeiaAndRecalculate(currentIdeia); // ATUALIZADO
    }

    public void uploadPitchDeck(Uri fileUri) {
        _isLoading.setValue(true);
        String userId = ideiaRepository.getCurrentUserId();
        ideiaRepository.uploadPitchDeck(ideiaId, userId, fileUri, result -> {
            if (result instanceof Result.Success) {
                String downloadUrl = ((Result.Success<String>) result).data;
                Ideia currentIdeia = _ideia.getValue();
                if (currentIdeia != null) {
                    currentIdeia.setPitchDeckUrl(downloadUrl);
                    updateIdeiaAndRecalculate(currentIdeia);
                }
                _toastEvent.setValue(new Event<>("Upload do Pitch Deck concluído!"));
                salvarDados(false);
            } else {
                _isLoading.setValue(false);
                _toastEvent.setValue(new Event<>("Falha no upload: " +
                        ((Result.Error<?>) result).error.getMessage()));
            }
        });
    }


    private void salvarDados(boolean finalizando) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null) return;

        // Se não estiver finalizando, apenas salva o estado atual sem mostrar loading ou navegar
        if (!finalizando) {
            ideiaRepository.updateIdeia(currentIdeia, result -> {
                _isLoading.setValue(false); // Garante que o loading do upload termine
                if (result instanceof Result.Error) {
                    _toastEvent.setValue(new Event<>("Sincronização automática falhou."));
                }
            });
            return;
        }

        // Lógica original para salvar e finalizar
        currentIdeia.setProntaParaInvestidores(true);
        _isLoading.setValue(true);
        ideiaRepository.updateIdeia(currentIdeia, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Sua ideia agora está visível para investidores!"));
                _navigationEvent.setValue(new Event<>(true));
            } else {
                currentIdeia.setProntaParaInvestidores(false); // Reverte em caso de erro
                _toastEvent.setValue(new Event<>("Erro ao salvar os dados."));
            }
        });
    }


    public void salvarEFinalizar() {
        salvarDados(true);
    }
}