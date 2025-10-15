package com.example.startuppulse.ui.ideias;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.IdeiaRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
@HiltViewModel
public class IdeiasViewModel extends ViewModel {

    private final IdeiaRepository repository;
    private ListenerRegistration publicIdeiasListener;
    private ListenerRegistration draftIdeiasListener;

    private final MutableLiveData<Result<List<Ideia>>> _publicIdeias = new MutableLiveData<com.example.startuppulse.common.Result<List<Ideia>>>();
    public final LiveData<Result<List<Ideia>>> publicIdeias = _publicIdeias;

    private final MutableLiveData<List<Ideia>> _draftIdeias = new MutableLiveData<>();
    public final LiveData<List<Ideia>> draftIdeias = _draftIdeias;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    @Inject
    public IdeiasViewModel(IdeiaRepository repository) {
        this.repository = repository;
        startListeners();
    }

    public void refresh() {
        _isLoading.setValue(true);
    }

    private void startListeners() {
        _isLoading.setValue(true);
        publicIdeiasListener = repository.listenToPublicIdeias(ideias -> {
            _publicIdeias.setValue(ideias);
            _isLoading.setValue(false); // Desativa o loading quando os dados principais chegam
        });

        draftIdeiasListener = repository.listenToDraftIdeias(result -> {
            if (result.isOk() && result.data != null) {
                // Se o resultado for sucesso, extrai a lista de dados.
                _draftIdeias.setValue(result.data);
            } else {
                // Se houver um erro, loga o problema e define a lista como vazia
                // para evitar que a UI mostre dados antigos ou quebre.
                Log.e("IdeiasViewModel", "Erro ao carregar rascunhos.", result.error);
                _draftIdeias.setValue(new ArrayList<>());
            }
        });
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