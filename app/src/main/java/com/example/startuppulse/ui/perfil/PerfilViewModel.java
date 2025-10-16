// Em: app/src/main/java/com/example/startuppulse/ui/perfil/PerfilViewModel.java

package com.example.startuppulse.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.MentorRepository;
import com.example.startuppulse.data.User;
import com.google.firebase.auth.FirebaseUser;

// Imports do Hilt
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

// MODIFICAÇÃO 1: Adicionar a anotação @HiltViewModel
@HiltViewModel
public class PerfilViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MentorRepository mentorRepository;

    private final MutableLiveData<Result<User>> _userProfileResult = new MutableLiveData<>();
    public LiveData<Result<User>> userProfileResult = _userProfileResult;

    private final MutableLiveData<Boolean> _isMentor = new MutableLiveData<>(false);
    public LiveData<Boolean> isMentor = _isMentor;
    private final MutableLiveData<Boolean> _navigateToLogin = new MutableLiveData<>(false);
    public final LiveData<Boolean> navigateToLogin = _navigateToLogin;

    @Inject
    public PerfilViewModel(AuthRepository authRepository, MentorRepository mentorRepository) {
        this.authRepository = authRepository;
        this.mentorRepository = mentorRepository;
        loadUserProfile();
        checkIfUserIsMentor();
    }

    private void loadUserProfile() {
        String currentUserId = authRepository.getCurrentUserId();
        if (currentUserId != null) {
            _userProfileResult.setValue(new Result.Loading<>());
            authRepository.getUserProfile(currentUserId, result -> {
                _userProfileResult.setValue(result);
            });
        } else {
            _userProfileResult.setValue(new Result.Error<>(new Exception("Usuário não autenticado.")));
        }
    }
    private void checkIfUserIsMentor() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            // Usamos o getMentorById, que é otimizado para buscar um único documento
            mentorRepository.getMentorById(currentUser.getUid(), result -> {
                // Se o resultado for Success, significa que o documento existe e o usuário é um mentor.
                // Se for Error (com a mensagem "Mentor não encontrado"), ele não é.
                _isMentor.postValue(result instanceof Result.Success);
            });
        }
    }

    public void logout() {
        authRepository.logout();
        _navigateToLogin.setValue(true);
    }
}