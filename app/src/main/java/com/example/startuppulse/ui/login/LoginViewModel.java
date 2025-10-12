package com.example.startuppulse.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.ResultCallback;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;

public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<LoginState> _loginState = new MutableLiveData<>(new LoginState(LoginState.AuthState.IDLE));
    public final LiveData<LoginState> loginState = _loginState;

    public LoginViewModel() {
        this.authRepository = AuthRepository.getInstance();
        checkIfUserIsAuthenticated();
    }

    private void checkIfUserIsAuthenticated() {
        if (authRepository.getCurrentUser() != null) {
            _loginState.setValue(new LoginState(LoginState.AuthState.SUCCESS));
        }
    }

    public void loginWithEmail(String email, String password) {
        if (email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.setValue(new LoginState("E-mail inválido"));
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            _loginState.setValue(new LoginState("Senha não pode estar em branco"));
            return;
        }

        _loginState.setValue(new LoginState(LoginState.AuthState.LOADING));
        authRepository.loginWithEmail(email, password, createLoginCallback());
    }

    public void loginWithGoogle(GoogleSignInAccount googleAccount) {
        _loginState.setValue(new LoginState(LoginState.AuthState.LOADING));
        authRepository.loginWithGoogle(googleAccount, createLoginCallback());
    }

    private ResultCallback<FirebaseUser> createLoginCallback() {
        return new ResultCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser data) {
                _loginState.setValue(new LoginState(LoginState.AuthState.SUCCESS));
            }

            @Override
            public void onError(Exception e) {
                _loginState.setValue(new LoginState("Falha na autenticação: " + e.getMessage()));
            }
        };
    }
}