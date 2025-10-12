package com.example.startuppulse.ui.ideias;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.Ideia;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;
public class IdeiasViewModel extends ViewModel {

    private final AuthRepository repository;
    private ListenerRegistration publicIdeiasListener;
    private ListenerRegistration draftIdeiasListener;

    private final MutableLiveData<List<Ideia>> _publicIdeias = new MutableLiveData<>();
    public final LiveData<List<Ideia>> publicIdeias = _publicIdeias;

    private final MutableLiveData<List<Ideia>> _draftIdeias = new MutableLiveData<>();
    public final LiveData<List<Ideia>> draftIdeias = _draftIdeias;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    public IdeiasViewModel() {
        this.repository = AuthRepository.getInstance();
        startListeners();
    }

    public void refresh() {
        _isLoading.setValue(true);
        // Os listeners já recarregam os dados, então só precisamos garantir que o loading pare.
        // A lógica do listener já desativa o _isLoading.
    }

    private void startListeners() {
        _isLoading.setValue(true);
        publicIdeiasListener = repository.listenToPublicIdeias(ideias -> {
            _publicIdeias.setValue(ideias);
            _isLoading.setValue(false); // Desativa o loading quando os dados principais chegam
        });

        draftIdeiasListener = repository.listenToDraftIdeias(_draftIdeias::setValue);
    }

    public void deleteIdeia(String ideiaId) {
        repository.deleteIdeia(ideiaId, success -> {
            // Lógica para lidar com sucesso/falha na exclusão, se necessário
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Garante que os listeners sejam removidos para evitar memory leaks
        if (publicIdeiasListener != null) {
            publicIdeiasListener.remove();
        }
        if (draftIdeiasListener != null) {
            draftIdeiasListener.remove();
        }
    }
}