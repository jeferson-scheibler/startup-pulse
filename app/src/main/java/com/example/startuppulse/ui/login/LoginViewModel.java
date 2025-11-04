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
    private static final String TAG = "LoginViewModel";
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
    }

    public void loginWithGoogle(GoogleSignInAccount googleAccount) {
        _loginState.setValue(new LoginState(LoginState.AuthState.LOADING));
        authRepository.loginWithGoogle(googleAccount, createLoginCallback());
    }

    private ResultCallback<FirebaseUser> createLoginCallback() {
        return result -> {
            if (result instanceof Result.Success) {
                // MODIFICAÇÃO: Não notificar sucesso ainda, primeiro registrar o token
                FirebaseUser user = ((Result.Success<FirebaseUser>) result).data;
                if (user != null) {
                    registerFcmToken(user);
                } else {
                    _loginState.setValue(new LoginState("Falha ao obter usuário após login."));
                }
            }
            else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<FirebaseUser>) result).error;
                _loginState.setValue(new LoginState("Falha na autenticação: " + e.getMessage()));
            }
        };
    }
    private void registerFcmToken(FirebaseUser user) {
        String userId = user.getUid();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        // O login foi bem-sucedido, mas o token falhou.
                        // Decidimos continuar e notificar o sucesso do login mesmo assim.
                        _loginState.postValue(new LoginState(LoginState.AuthState.SUCCESS));
                        return;
                    }

                    // Obter o token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token obtained: " + token);

                    // Salvar o token no Firestore usando o UserRepository
                    userRepository.updateFcmToken(userId, token, tokenResult -> {
                        if (tokenResult instanceof Result.Success) {
                            Log.i(TAG, "FCM Token updated successfully for user: " + userId);
                        } else {
                            Log.w(TAG, "Failed to update FCM Token in Firestore for user: " + userId);
                        }

                        // Agora sim, notificar a UI que o login foi concluído.
                        // Fazemos isso independentemente de salvar o token ter falhado,
                        // pois o login em si funcionou.
                        _loginState.postValue(new LoginState(LoginState.AuthState.SUCCESS));
                    });
                });
    }
}