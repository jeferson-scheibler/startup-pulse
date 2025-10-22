// Em: app/src/main/java/com/example/startuppulse/data/repositories/IAuthRepository.java

package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;

/**
 * Interface que define o contrato para o repositório de autenticação.
 * Abstrai as fontes de dados e fornece um ponto de entrada único para
 * operações de usuário e sessão.
 */
public interface IAuthRepository {

    // --- Métodos de Sessão ---
    @Nullable
    FirebaseUser getCurrentUser();

    @Nullable
    String getCurrentUserId();

    boolean isLoggedIn();

    boolean isCurrentUser(@Nullable String userId);

    void logout();

    // --- Métodos de Autenticação ---
    void loginWithEmail(@NonNull String email, @NonNull String password, @NonNull ResultCallback<FirebaseUser> callback);

    void loginWithGoogle(@NonNull GoogleSignInAccount googleAccount, @NonNull ResultCallback<FirebaseUser> callback);

    void createUser(@NonNull String name, @NonNull String email, @NonNull String password, @NonNull ResultCallback<FirebaseUser> callback);

    // --- Métodos de Perfil (Firestore) ---
    void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback);
    void getDiasAcessados(String userId, ResultCallback<Integer> callback);

    void updatePremiumStatus(@NonNull String userId, boolean isPremium, @NonNull ResultCallback<Void> callback);
}