package com.example.startuppulse.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.User;
import com.google.firebase.auth.FirebaseUser;

public class PerfilViewModel extends ViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<User> _userProfile = new MutableLiveData<>();
    public final LiveData<User> userProfile = _userProfile;

    private final MutableLiveData<Boolean> _navigateToLogin = new MutableLiveData<>(false);
    public final LiveData<Boolean> navigateToLogin = _navigateToLogin;

    public PerfilViewModel() {
        this.repository = AuthRepository.getInstance();
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = repository.getCurrentUser();
        if (firebaseUser == null) {
            _navigateToLogin.setValue(true);
            return;
        }
        // O ideal é ter um método no repositório que busca todos os dados do usuário de uma vez
        repository.getUserProfile(firebaseUser.getUid(), user -> {
            _userProfile.setValue(user);
        });
    }

    public void logout() {
        repository.logout();
        _navigateToLogin.setValue(true);
    }
}