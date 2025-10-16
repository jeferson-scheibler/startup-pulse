package com.example.startuppulse.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.ResultCallback;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<LoginState> _loginState = new MutableLiveData<>(new LoginState(LoginState.AuthState.IDLE));
    public final LiveData<LoginState> loginState = _loginState;

    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
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
        return result -> {
            // Verifica se o resultado da operação foi um Sucesso
            if (result instanceof Result.Success) {
                _loginState.setValue(new LoginState(LoginState.AuthState.SUCCESS));
            }
            // Se não foi sucesso, foi um Erro
            else if (result instanceof Result.Error) {
                // Extrai a exceção do objeto de Erro
                Exception e = ((Result.Error<FirebaseUser>) result).error;
                _loginState.setValue(new LoginState("Falha na autenticação: " + e.getMessage()));
            }
        };
    }
}