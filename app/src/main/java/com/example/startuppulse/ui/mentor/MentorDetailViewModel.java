package com.example.startuppulse.ui.mentor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.IAuthRepository; // MUDADO: Usar IAuthRepository
import com.example.startuppulse.data.repositories.IMentorRepository; // MUDADO: Usar Interface
import com.example.startuppulse.data.repositories.IUserRepository; // ADICIONADO
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User; // ADICIONADO

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MentorDetailViewModel extends ViewModel {

    private final IMentorRepository mentorRepository;
    private final IUserRepository userRepository;
    private final IAuthRepository authRepository;
    private final String mentorId;

    // LiveData para os detalhes do MENTOR (bio, estado, cidade)
    private final MutableLiveData<Result<Mentor>> _mentorDetails = new MutableLiveData<>();
    public LiveData<Result<Mentor>> mentorDetails = _mentorDetails;

    // LiveData para os detalhes do USER (nome, foto, linkedin, areas)
    private final MutableLiveData<Result<User>> _userDetails = new MutableLiveData<>();
    public LiveData<Result<User>> userDetails = _userDetails;

    // LiveData para verificar se o usuário logado é o dono do perfil
    private final MutableLiveData<Boolean> _isProfileOwner = new MutableLiveData<>(false);
    public LiveData<Boolean> isProfileOwner = _isProfileOwner;

    @Inject
    public MentorDetailViewModel(IMentorRepository mentorRepository, IUserRepository userRepository, IAuthRepository authRepository, SavedStateHandle savedStateHandle) {
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
        this.authRepository = authRepository;

        this.mentorId = savedStateHandle.get("mentorId");

        if (mentorId != null && !mentorId.isEmpty()) {
            fetchUserDetails(mentorId);
            fetchMentorDetails(mentorId);
            checkIfProfileOwner(mentorId);
        } else {
            _userDetails.setValue(new Result.Error<>(new Exception("Mentor ID is missing.")));
            _mentorDetails.setValue(new Result.Error<>(new Exception("Mentor ID is missing.")));
        }
    }
    private void fetchUserDetails(String userId) {
        _userDetails.setValue(new Result.Loading<>());
        userRepository.getUserProfile(userId, _userDetails::postValue);
    }

    // AJUSTADO: Busca dados do /mentores/{uid}
    private void fetchMentorDetails(String mentorId) {
        _mentorDetails.setValue(new Result.Loading<>());
        // Usando getMentor (que assume que ID == UID)
        mentorRepository.getMentorById(mentorId, _mentorDetails::postValue);
    }

    /**
     * Compara o mentorId (que é o UID) com o UID do usuário logado.
     * MUDADO: Não depende mais do 'ownerId' do objeto mentor.
     */
    private void checkIfProfileOwner(String mentorId) {
        String currentUid = authRepository.getCurrentUserId();
        if (currentUid != null && mentorId != null && mentorId.equals(currentUid)) {
            _isProfileOwner.postValue(true);
        } else {
            _isProfileOwner.postValue(false);
        }
    }

    public String getMentorId() {
        return mentorId;
    }
}