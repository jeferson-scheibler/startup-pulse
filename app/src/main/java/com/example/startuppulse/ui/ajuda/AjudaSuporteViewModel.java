package com.example.startuppulse.ui.ajuda;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.util.Event;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AjudaSuporteViewModel extends ViewModel {

    // Evento para sinalizar ao Fragment que ele deve abrir o e-mail
    private final MutableLiveData<Event<String>> _openEmailClient = new MutableLiveData<>();
    public final LiveData<Event<String>> openEmailClient = _openEmailClient;

    private final String supportEmail = "jeferson.scheibler@universo.univates.br";

    @Inject
    public AjudaSuporteViewModel() {
        // Construtor vazio por enquanto
    }

    public void requestOpenEmailClient() {
        _openEmailClient.setValue(new Event<>(supportEmail));
    }
}