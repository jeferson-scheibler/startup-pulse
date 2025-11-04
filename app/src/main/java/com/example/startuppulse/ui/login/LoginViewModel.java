package com.example.startuppulse.ui.login;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final IAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final MutableLiveData<LoginState> _loginState = new MutableLiveData<>(new LoginState(LoginState.AuthState.IDLE));
    public final LiveData<LoginState> loginState = _loginState;

    @Inject
    public LoginViewModel(AuthRepository authRepository, IUserRepository userRepository) { // 3. INJETE O REPOSITÓRIO
        this.authRepository = authRepository;
        this.userRepository = userRepository;
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
        updateFcmToken();
    }

    public void loginWithGoogle(GoogleSignInAccount googleAccount) {
        _loginState.setValue(new LoginState(LoginState.AuthState.LOADING));
        authRepository.loginWithGoogle(googleAccount, createLoginCallback());
        updateFcmToken();
    }

    private void updateFcmToken() {
        // Pega o ID do usuário que acabou de logar
        String userId = authRepository.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w("LoginViewModel", "User ID nulo após o login, não é possível salvar o token FCM.");
            return;
        }

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("LoginViewModel", "Fetching FCM registration token failed", task.getException());
                return;
            }

            String token = task.getResult();

            // Salva o token no Firestore usando o método correto
            if (token != null) {
                // Chama o método com userId, token e um callback
                userRepository.updateFcmToken(userId, token, result -> {
                    if (result instanceof Result.Success) {
                        Log.d("LoginViewModel", "Token FCM salvo para o usuário: " + userId);
                    } else {
                        Log.e("LoginViewModel", "Falha ao salvar token FCM: ", ((Result.Error<?>) result).error);
                    }
                });
            }
        });
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