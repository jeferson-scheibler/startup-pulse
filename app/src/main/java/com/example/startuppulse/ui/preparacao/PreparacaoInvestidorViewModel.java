package com.example.startuppulse.ui.preparacao;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.ReadinessCalculator;
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


    @Inject
    public PreparacaoInvestidorViewModel(IdeiaRepository ideiaRepository, SavedStateHandle savedStateHandle) {
        this.ideiaRepository = ideiaRepository;
        this.ideiaId = savedStateHandle.get("ideiaId");
        loadIdeia();
    }

    private void loadIdeia() {
        if (ideiaId == null) {
            _toastEvent.setValue(new Event<>("Erro: ID da Ideia não encontrado."));
            return;
        }
        _isLoading.setValue(true);
        ideiaRepository.getIdeiaById(ideiaId, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                Ideia loadedIdeia = ((Result.Success<Ideia>) result).data;
                _ideia.setValue(loadedIdeia);
                // Calcula o score assim que a ideia é carregada
                calculateReadiness(loadedIdeia);
            } else {
                _toastEvent.setValue(new Event<>("Falha ao carregar dados da ideia."));
            }
        });
    }

    // NOVO: Método para centralizar a atualização da ideia e o recálculo do score
    private void updateIdeiaAndRecalculate(Ideia ideia) {
        _ideia.setValue(ideia);
        calculateReadiness(ideia);
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
        ideiaRepository.uploadPitchDeck(ideiaId, fileUri, result -> {
            if (result instanceof Result.Success) {
                String downloadUrl = ((Result.Success<String>) result).data;
                Ideia currentIdeia = _ideia.getValue();
                if (currentIdeia != null) {
                    currentIdeia.setPitchDeckUrl(downloadUrl);
                    updateIdeiaAndRecalculate(currentIdeia); // ATUALIZADO
                }
                _toastEvent.setValue(new Event<>("Upload do Pitch Deck concluído!"));
                salvarDados(false); // Salva a URL no documento da ideia
            } else {
                _isLoading.setValue(false);
                _toastEvent.setValue(new Event<>("Falha no upload: " + ((Result.Error)result).error.getMessage()));
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