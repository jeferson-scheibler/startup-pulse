package com.example.startuppulse.ui.propulsor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.repositories.ISparkRepository;
import com.example.startuppulse.util.Event;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IIdeiaRepository;
import com.example.startuppulse.data.repositories.IPropulsorRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PropulsorViewModel extends ViewModel {

    private final IPropulsorRepository propulsorRepository;
    private final IIdeiaRepository ideiaRepository;
    private final ISparkRepository sparkRepository;

    // Histórico do Chat
    private final MutableLiveData<List<ChatMessage>> _chatHistory = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ChatMessage>> getChatHistory() { return _chatHistory; }

    // Estados da UI
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() { return _isLoading; }

    private final MutableLiveData<Boolean> _showActionButtons = new MutableLiveData<>(false);
    public LiveData<Boolean> getShowActionButtons() { return _showActionButtons; }

    // Eventos (para Toasts/Snackbars)
    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> getToastEvent() { return _toastEvent; }

    private String lastSparkText = ""; // Armazena a última ideia do usuário

    @Inject
    public PropulsorViewModel(IPropulsorRepository propulsorRepository, IIdeiaRepository ideiaRepository,
                              ISparkRepository sparkRepository) {
        this.propulsorRepository = propulsorRepository;
        this.ideiaRepository = ideiaRepository;
        this.sparkRepository = sparkRepository;
        addInitialMessage();
    }

    private void addInitialMessage() {
        addMessage("Eu sou o Propulsor. Descreva sua faísca de ideia em poucas palavras. Em segundos, vou analisar o mercado, encontrar concorrentes e identificar suas oportunidades.", false);
    }

    public void sendSparkToIA(String text) {
        if (text == null || text.trim().isEmpty()) {
            _toastEvent.setValue(new Event<>("Por favor, insira uma ideia."));
            return;
        }
        lastSparkText = text.trim(); // Salva a ideia para os botões de ação
        addMessage(lastSparkText, true); // Adiciona a mensagem do usuário
        _isLoading.setValue(true);
        _showActionButtons.setValue(false);

        propulsorRepository.getAnalysis(lastSparkText, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                String analysis = ((Result.Success<String>) result).data;
                addMessage(analysis, false); // Adiciona a resposta da IA
                _showActionButtons.setValue(true); // Mostra os botões "Salvar" / "Lançar"

            } else if (result instanceof Result.Error) { // Boa prática: checar o tipo
                Exception e = ((Result.Error<String>) result).error;
                addMessage("Desculpe, não consegui analisar sua ideia. Tente novamente. Erro: " + e.getMessage(), false);
            }
        });
    }

    public void saveAsDraft() {
        if (lastSparkText.isEmpty()) {
            _toastEvent.setValue(new Event<>("Não há faísca para salvar."));
            return;
        }

        String newIdeiaId = ideiaRepository.getNewIdeiaId(); //
        Ideia novaIdeia = new Ideia(); //
        novaIdeia.setId(newIdeiaId);
        novaIdeia.setNome("Faísca: " + lastSparkText.substring(0, Math.min(lastSparkText.length(), 40)) + "...");
        novaIdeia.setDescricao(lastSparkText);
        novaIdeia.setStatus(Ideia.Status.RASCUNHO); //

        // Usando o saveIdeia que já existe no seu repositório
        ideiaRepository.saveIdeia(novaIdeia, result -> {
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Faísca salva em 'Meus Rascunhos'!"));
            } else if (result instanceof Result.Error) { // Boa prática: checar o tipo
                Exception e = ((Result.Error<Void>) result).error;
                _toastEvent.setValue(new Event<>("Erro ao salvar: " + e.getMessage()));
            }
        });
    }

    // Adiciona uma nova mensagem à lista e notifica o LiveData
    private void addMessage(String text, boolean isUser) {
        List<ChatMessage> currentHistory = _chatHistory.getValue();
        if (currentHistory == null) {
            currentHistory = new ArrayList<>();
        }
        currentHistory.add(new ChatMessage(text, isUser));
        _chatHistory.setValue(currentHistory); // Notifica os observadores
    }

    /**
     * Pega a última faísca analisada e a publica no Vórtex.
     */
    public void launchToVortex() {
        if (lastSparkText.isEmpty()) {
            _toastEvent.setValue(new Event<>("Não há faísca para lançar."));
            return;
        }

        // TODO: Obter a localização REAL do usuário.
        // Por agora, usaremos uma localização de placeholder.
        // A implementação de permissão e FusedLocationProvider é complexa
        // e deve ser feita separadamente.
        double placeholderLat = -23.5505; // São Paulo
        double placeholderLng = -46.6333;

        _isLoading.setValue(true); // Reutiliza o loading
        sparkRepository.createSpark(lastSparkText, placeholderLat, placeholderLng, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Faísca lançada no Vórtex!"));
                _showActionButtons.setValue(false); // Esconde os botões após a ação
            } else {
                Exception e = ((Result.Error<String>) result).error;
                _toastEvent.setValue(new Event<>("Erro ao lançar: " + e.getMessage()));
            }
        });
    }
}