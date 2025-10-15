// Em: app/src/main/java/com/example/startuppulse/ui/perfil/PerfilViewModel.java

package com.example.startuppulse.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.User;
import com.google.firebase.auth.FirebaseUser;

// Imports do Hilt
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

// MODIFICAÇÃO 1: Adicionar a anotação @HiltViewModel
@HiltViewModel
public class PerfilViewModel extends ViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<User> _userProfile = new MutableLiveData<>();
    public final LiveData<User> userProfile = _userProfile;

    private final MutableLiveData<Boolean> _navigateToLogin = new MutableLiveData<>(false);
    public final LiveData<Boolean> navigateToLogin = _navigateToLogin;

    /**
     * MODIFICAÇÃO 2: Usar @Inject no construtor.
     * O Hilt agora fornecerá a instância do AuthRepository automaticamente.
     */
    @Inject
    public PerfilViewModel(AuthRepository repository) {
        this.repository = repository;
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = repository.getCurrentUser();
        if (firebaseUser == null) {
            _navigateToLogin.setValue(true);
            return;
        }
        // A busca de dados agora usa a instância do repositório injetada pelo Hilt.
        repository.getUserProfile(firebaseUser.getUid(), user -> {
            _userProfile.setValue(user);
        });
    }

    public void logout() {
        repository.logout();
        _navigateToLogin.setValue(true);
    }
}