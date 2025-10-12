package com.example.startuppulse.ui.login;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class LoginViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final MutableLiveData<AuthenticationState> _authState = new MutableLiveData<>();
    public final LiveData<AuthenticationState> authState = _authState;

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void login(String email, String password) {
        _authState.setValue(AuthenticationState.LOADING);
        authRepository.login(email, password, new ResultCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser data) {
                _authState.setValue(AuthenticationState.AUTHENTICATED);
            }

            @Override
            public void onError(Exception error) {
                _authState.setValue(AuthenticationState.ERROR);
            }
        });
    }
}
