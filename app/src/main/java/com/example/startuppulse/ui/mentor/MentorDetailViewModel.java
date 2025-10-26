package com.example.startuppulse.ui.mentor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.MentorRepository;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.ResultCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MentorDetailViewModel extends ViewModel {

    private final MentorRepository mentorRepository;
    private final SavedStateHandle savedStateHandle;
    private final String mentorId;

    // LiveData principal para os detalhes do mentor (usando a classe Result)
    private final MutableLiveData<Result<Mentor>> _mentorDetails = new MutableLiveData<>();
    public LiveData<Result<Mentor>> mentorDetails = _mentorDetails;

    // LiveData para verificar se o usuário logado é o dono do perfil
    private final MutableLiveData<Boolean> _isProfileOwner = new MutableLiveData<>(false);
    public LiveData<Boolean> isProfileOwner = _isProfileOwner;

    @Inject
    public MentorDetailViewModel(MentorRepository mentorRepository, SavedStateHandle savedStateHandle) {
        this.mentorRepository = mentorRepository;
        this.savedStateHandle = savedStateHandle;

        // Recupera o ID do mentor dos argumentos de navegação
        this.mentorId = savedStateHandle.get("mentorId");

        if (mentorId != null && !mentorId.isEmpty()) {
            fetchMentorDetails(mentorId);
            // A verificação de "dono" (checkIfProfileOwner) foi movida para
            // dentro do fetchMentorDetails, pois precisamos do ownerId do mentor.
        } else {
            _mentorDetails.setValue(new Result.Error<>(new Exception("Mentor ID is missing.")));
        }
    }

    private void fetchMentorDetails(String mentorId) {
        _mentorDetails.setValue(new Result.Loading<>());

        mentorRepository.getMentorById(mentorId, new ResultCallback<Mentor>() {
            @Override
            public void onResult(Result<Mentor> result) {
                if (result instanceof Result.Success) {
                    Mentor mentor = ((Result.Success<Mentor>) result).data;
                    // Assim que o mentor for carregado com sucesso,
                    // verificamos se o usuário logado é o dono
                    checkIfProfileOwner(mentor.getOwnerId());
                } else if (result instanceof Result.Error) {
                    // Se der erro ao carregar, definimos que não é o dono
                    _isProfileOwner.postValue(false);
                }

                // Publica o resultado (Sucesso ou Erro) para a UI
                _mentorDetails.postValue(result);
            }
        });
    }

    /**
     * Compara o ownerId do mentor com o UID do usuário logado.
     * @param ownerId O ID do proprietário vindo do documento do mentor.
     */
    private void checkIfProfileOwner(String ownerId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && ownerId != null && ownerId.equals(currentUser.getUid())) {
            _isProfileOwner.postValue(true);
        } else {
            _isProfileOwner.postValue(false);
        }
    }

    // Getter para o ID, caso o Fragment precise (ex: para navegação)
    public String getMentorId() {
        return mentorId;
    }
}