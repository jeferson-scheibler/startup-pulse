package com.example.startuppulse.ui.ideias;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IdeiaRepository;
import com.example.startuppulse.util.Event;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MeusRascunhosViewModel extends ViewModel {

    private final IdeiaRepository ideiaRepository;
    private ListenerRegistration draftIdeiasListener;

    // --- LiveData para a UI ---
    private final MutableLiveData<Result<List<Ideia>>> _draftIdeiasResult = new MutableLiveData<>();
    public LiveData<Result<List<Ideia>>> draftIdeiasResult = _draftIdeiasResult;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> toastEvent = _toastEvent;

    @Inject
    public MeusRascunhosViewModel(IdeiaRepository ideiaRepository) {
        this.ideiaRepository = ideiaRepository;
        listenToDraftIdeias();
    }

    private void listenToDraftIdeias() {
        _isLoading.setValue(true);
        if (draftIdeiasListener != null) {
            draftIdeiasListener.remove();
        }
        draftIdeiasListener = ideiaRepository.listenToDraftIdeias(result -> {
            _draftIdeiasResult.setValue(result);
            _isLoading.setValue(false);
        });
    }

    public void deleteDraft(String ideiaId) {
        ideiaRepository.deleteIdeia(ideiaId, result -> {
            if (result instanceof Result.Error) {
                _toastEvent.postValue(new Event<>("Erro ao excluir o rascunho."));
            }
            // Em caso de sucesso, o listener em tempo real já atualizará a UI.
        });
    }

    public void refresh() {
        // A lógica do listener já atualiza em tempo real.
        // Apenas garantimos que o ícone de refresh pare de girar.
        _isLoading.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (draftIdeiasListener != null) {
            draftIdeiasListener.remove();
        }
    }
}