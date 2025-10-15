package com.example.startuppulse.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainViewModel extends ViewModel {

    public enum AuthenticationState {
        AUTHENTICATED,
        UNAUTHENTICATED
    }

    private final MutableLiveData<AuthenticationState> _authenticationState = new MutableLiveData<>();
    public final LiveData<AuthenticationState> authenticationState = _authenticationState;

    private final FirebaseAuth.AuthStateListener authStateListener;

    public MainViewModel() {
        // Cria um listener que será chamado toda vez que o usuário logar ou deslogar.
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                _authenticationState.setValue(AuthenticationState.AUTHENTICATED);
            } else {
                _authenticationState.setValue(AuthenticationState.UNAUTHENTICATED);
            }
        };

        // Começa a ouvir as mudanças
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Para de ouvir para evitar memory leaks
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
    }
}