// Em: app/src/main/java/com/example/startuppulse/ui/perfil/PerfilViewModel.java

package com.example.startuppulse.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.repositories.MentorRepository;
import com.example.startuppulse.data.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Imports do Hilt
import java.util.Date;

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

    private final MutableLiveData<Boolean> _isPro = new MutableLiveData<>(false);
    public final LiveData<Boolean> isPro = _isPro;
    private final FirebaseFirestore firestore;

    @Inject
    public PerfilViewModel(AuthRepository authRepository, MentorRepository mentorRepository, FirebaseFirestore firestore) {
        this.authRepository = authRepository;
        this.mentorRepository = mentorRepository;
        this.firestore = firestore;
        checkSubscriptionStatus();
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
    public void checkSubscriptionStatus() {
        String userId = authRepository.getCurrentUserId();
        if (userId == null) {
            _isPro.postValue(false);
            return;
        }

        firestore.collection("premium").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Timestamp dataFim = document.getTimestamp("data_fim");
                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            _isPro.postValue(true); // Usuário é Pro
                        } else {
                            _isPro.postValue(false); // Assinatura expirou
                        }
                    } else {
                        _isPro.postValue(false); // Não tem assinatura
                    }
                })
                .addOnFailureListener(e -> {
                    _isPro.postValue(false); // Assume que não é Pro em caso de erro
                    // Opcional: Logar o erro
                });
    }

    public void logout() {
        authRepository.logout();
        _navigateToLogin.setValue(true);
    }
}