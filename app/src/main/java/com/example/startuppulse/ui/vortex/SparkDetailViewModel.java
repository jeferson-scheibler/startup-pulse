package com.example.startuppulse.ui.vortex;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.util.Event;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.ISparkRepository;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SparkDetailViewModel extends ViewModel {

    private final ISparkRepository sparkRepository;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> getToastEvent() { return _toastEvent; }

    private final MutableLiveData<Boolean> _closeDialogEvent = new MutableLiveData<>(false);
    public LiveData<Boolean> getCloseDialogEvent() { return _closeDialogEvent; }

    @Inject
    public SparkDetailViewModel(ISparkRepository sparkRepository) {
        this.sparkRepository = sparkRepository;
    }

    public void voteOnSpark(String sparkId, int weight) {
        sparkRepository.voteSpark(sparkId, weight, result -> {
            if (result instanceof Result.Success) {
                String status = ((Result.Success<String>) result).data;
                if ("already_voted".equals(status)) {
                    _toastEvent.setValue(new Event<>("Você já deu o pulso nesta faísca!"));
                } else {
                    _toastEvent.setValue(new Event<>("Pulso (+" + weight + ") registado!"));
                }
                _closeDialogEvent.setValue(true); // Fecha o dialog em sucesso

            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<String>) result).error;
                _toastEvent.setValue(new Event<>("Erro ao votar: " + e.getMessage()));
            }
        });
    }
}