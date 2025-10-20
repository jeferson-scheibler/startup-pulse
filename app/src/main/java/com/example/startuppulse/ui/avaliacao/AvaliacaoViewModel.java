package com.example.startuppulse.ui.avaliacao;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.util.Event;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.Avaliacao;
import com.example.startuppulse.data.repositories.IdeiaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AvaliacaoViewModel extends ViewModel {

    private final IdeiaRepository ideiaRepository;
    private final String ideiaId;

    // --- LiveData para a UI ---
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _closeScreenEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closeScreenEvent = _closeScreenEvent;

    @Inject
    public AvaliacaoViewModel(IdeiaRepository ideiaRepository, SavedStateHandle savedStateHandle) {
        this.ideiaRepository = ideiaRepository;
        this.ideiaId = savedStateHandle.get("ideiaId");
    }

    public void enviarAvaliacao(List<Avaliacao> avaliacoes) {
        if (ideiaId == null) {
            _toastEvent.setValue(new Event<>("Erro: ID da ideia não encontrado."));
            return;
        }
        if (!isAvaliacaoValida(avaliacoes)) {
            return; // A validação já emite o Toast
        }

        _isLoading.setValue(true);

        // Converte a lista de Avaliacao para o formato que o Firestore espera
        List<Map<String, Object>> avaliacoesParaSalvar = new ArrayList<>();
        for (Avaliacao aval : avaliacoes) {
            Map<String, Object> avalMap = new HashMap<>();
            avalMap.put("criterio", aval.getCriterio()); // Corrigido para 'criterio'
            avalMap.put("nota", aval.getNota());
            avalMap.put("feedback", aval.getFeedback());
            avaliacoesParaSalvar.add(avalMap);
        }

        ideiaRepository.salvarAvaliacao(ideiaId, avaliacoesParaSalvar, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Avaliação enviada com sucesso!"));
                _closeScreenEvent.setValue(new Event<>(true));
            } else {
                _toastEvent.setValue(new Event<>("Erro ao enviar avaliação."));
            }
        });
    }

    private boolean isAvaliacaoValida(List<Avaliacao> avaliacoes) {
        for (Avaliacao aval : avaliacoes) {
            if (aval.getNota() < 5.0f && (aval.getFeedback() == null || aval.getFeedback().trim().isEmpty())) {
                _toastEvent.setValue(new Event<>("Para notas baixas em \"" + aval.getCriterio() + "\", um feedback é obrigatório."));
                return false;
            }
        }
        return true;
    }
}