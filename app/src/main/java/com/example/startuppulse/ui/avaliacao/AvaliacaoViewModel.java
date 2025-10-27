package com.example.startuppulse.ui.avaliacao;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

// --- Importações Adicionadas ---
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IIdeiaRepository;
// --- Fim das Importações Adicionadas ---

import com.example.startuppulse.util.Event;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.Avaliacao;
// import com.example.startuppulse.data.repositories.IdeiaRepository; // <-- Removido

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AvaliacaoViewModel extends ViewModel {

    // Adiciona uma constante para a nota de corte
    private static final float NOTA_MINIMA_APROVACAO = 7.0f;

    // --- CORREÇÃO: Injeta a Interface, não a classe concreta ---
    private final IIdeiaRepository ideiaRepository;
    private final String ideiaId;

    // --- LiveData para a UI ---
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _closeScreenEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closeScreenEvent = _closeScreenEvent;

    @Inject
    public AvaliacaoViewModel(IIdeiaRepository ideiaRepository, SavedStateHandle savedStateHandle) {
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

        // --- CORREÇÃO: Lógica para calcular a média e definir o status ---
        float notaTotal = 0;
        if (avaliacoes.isEmpty()) {
            _toastEvent.setValue(new Event<>("Erro: Lista de avaliação está vazia."));
            _isLoading.setValue(false);
            return;
        }

        for (Avaliacao aval : avaliacoes) {
            notaTotal += aval.getNota();
        }
        float notaMediaFinal = notaTotal / (float) avaliacoes.size();

        // Determina o status final com base na média
        Ideia.Status statusFinal = (notaMediaFinal >= NOTA_MINIMA_APROVACAO) ?
                Ideia.Status.AVALIADA_APROVADA :
                Ideia.Status.AVALIADA_REPROVADA;
        // --- FIM DA CORREÇÃO ---


        // Converte a lista de Avaliacao para o formato que o Firestore espera
        List<Map<String, Object>> avaliacoesParaSalvar = new ArrayList<>();
        for (Avaliacao aval : avaliacoes) {
            Map<String, Object> avalMap = new HashMap<>();
            avalMap.put("criterio", aval.getCriterio());
            avalMap.put("nota", aval.getNota());
            avalMap.put("feedback", aval.getFeedback());
            avaliacoesParaSalvar.add(avalMap);
        }

        // --- CORREÇÃO: Passa o statusFinal para o repositório ---
        ideiaRepository.salvarAvaliacao(ideiaId, avaliacoesParaSalvar, statusFinal, result -> {
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
            // Se a nota for baixa (ex: < 5), o feedback é obrigatório
            if (aval.getNota() < 5.0f && (aval.getFeedback() == null || aval.getFeedback().trim().isEmpty())) {
                _toastEvent.setValue(new Event<>("Para notas abaixo de 5.0 em \"" + aval.getCriterio() + "\", um feedback é obrigatório."));
                return false;
            }
        }
        return true;
    }
}