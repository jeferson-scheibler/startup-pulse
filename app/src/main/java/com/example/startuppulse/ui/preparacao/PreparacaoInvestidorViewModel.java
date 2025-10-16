package com.example.startuppulse.ui.preparacao;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.IdeiaRepository;
import com.example.startuppulse.data.MembroEquipe;
import com.example.startuppulse.data.Metrica;
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
                _ideia.setValue(((Result.Success<Ideia>) result).data);
            } else {
                _toastEvent.setValue(new Event<>("Falha ao carregar dados da ideia."));
            }
        });
    }

    public void adicionarMembro(String nome, String funcao, String linkedin) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null) return;

        MembroEquipe novoMembro = new MembroEquipe(nome, funcao, linkedin);
        List<MembroEquipe> equipe = new ArrayList<>(currentIdeia.getEquipe() != null ? currentIdeia.getEquipe() : new ArrayList<>());
        equipe.add(novoMembro);
        currentIdeia.setEquipe(equipe);
        _ideia.setValue(currentIdeia);
    }

    public void removerMembro(MembroEquipe membro) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null || currentIdeia.getEquipe() == null) return;

        List<MembroEquipe> equipe = new ArrayList<>(currentIdeia.getEquipe());
        equipe.remove(membro);
        currentIdeia.setEquipe(equipe);
        _ideia.setValue(currentIdeia);
    }

    public void adicionarMetrica() {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null) return;

        Metrica novaMetrica = new Metrica("Nova Métrica", "0");
        List<Metrica> metricas = new ArrayList<>(currentIdeia.getMetricas() != null ? currentIdeia.getMetricas() : new ArrayList<>());
        metricas.add(novaMetrica);
        currentIdeia.setMetricas(metricas);
        _ideia.setValue(currentIdeia);
    }

    public void removerMetrica(Metrica metrica) {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null || currentIdeia.getMetricas() == null) return;

        List<Metrica> metricas = new ArrayList<>(currentIdeia.getMetricas());
        metricas.remove(metrica);
        currentIdeia.setMetricas(metricas);
        _ideia.setValue(currentIdeia);
    }

    public void uploadPitchDeck(Uri fileUri) {
        _isLoading.setValue(true);
        ideiaRepository.uploadPitchDeck(ideiaId, fileUri, result -> {
            if (result instanceof Result.Success) {
                String downloadUrl = ((Result.Success<String>) result).data;
                Ideia currentIdeia = _ideia.getValue();
                if (currentIdeia != null) {
                    currentIdeia.setPitchDeckUrl(downloadUrl);
                    _ideia.setValue(currentIdeia);
                }
                _toastEvent.setValue(new Event<>("Upload do Pitch Deck concluído!"));
                salvarDados(false); // Salva a URL no documento da ideia
            } else {
                _isLoading.setValue(false);
                _toastEvent.setValue(new Event<>("Falha no upload: " + ((Result.Error)result).error.getMessage()));
            }
        });
    }

    public void salvarEFinalizar() {
        Ideia currentIdeia = _ideia.getValue();
        if (currentIdeia == null) return;

        currentIdeia.setProntaParaInvestidores(true);

        _isLoading.setValue(true);
        ideiaRepository.updateIdeia(currentIdeia, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Sua ideia agora está visível para investidores!"));
                _navigationEvent.setValue(new Event<>(true)); // Sinaliza para o Fragment navegar
            } else {
                _toastEvent.setValue(new Event<>("Erro ao salvar os dados."));
            }
        });
    }
}