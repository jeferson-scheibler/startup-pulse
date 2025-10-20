package com.example.startuppulse.data.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório central para autenticação e gerenciamento de dados de perfil do usuário.
 * Gerenciado pelo Hilt como um Singleton para toda a aplicação.
 */
@Singleton
public class AuthRepository implements IAuthRepository{
    private static final String USUARIOS_COLLECTION = "usuarios";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    @Inject
    public AuthRepository(FirebaseFirestore firestore, FirebaseAuth firebaseAuth) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
    }

    // --- MÉTODOS DE SESSÃO ---

    @Override
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    @Override
    public boolean isCurrentUser(@Nullable String userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(getCurrentUserId());
    }

    @Override
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    @Override
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public void logout() {
        firebaseAuth.signOut();
    }

    // --- MÉTODOS DE AUTENTICAÇÃO ---

    @Override
    public void loginWithEmail(@NonNull String email, @NonNull String password, @NonNull ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password) // CORRIGIDO: Usa firebaseAuth
                .addOnSuccessListener(authResult -> callback.onResult(new Result.Success<>(authResult.getUser())))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void loginWithGoogle(@NonNull GoogleSignInAccount googleAccount, @NonNull ResultCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential) // CORRIGIDO: Usa firebaseAuth
                .addOnSuccessListener(authResult -> {
                    saveUserToFirestoreOnLogin(authResult.getUser(), callback);
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void createUser(@NonNull String name, @NonNull String email, @NonNull String password, @NonNull ResultCallback<FirebaseUser> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password) // CORRIGIDO: Usa firebaseAuth
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    saveUserToFirestoreOnLogin(user, callback);
                                });
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Falha ao obter o usuário após a criação.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // --- MÉTODOS DE PERFIL DE USUÁRIO (FIRESTORE) ---

    /**
     * Busca os dados de um perfil de usuário a partir do seu ID.
     */
    public void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback) {
        firestore.collection(USUARIOS_COLLECTION).document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        callback.onResult(new Result.Success<>(user));
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Perfil de usuário não encontrado.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Atualiza o status de premium de um usuário.
     */
    public void updatePremiumStatus(@NonNull String userId, boolean isPremium, @NonNull ResultCallback<Void> callback) {
        firestore.collection(USUARIOS_COLLECTION).document(userId)
                .update("isPremium", isPremium)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Garante que um usuário exista no Firestore após o login ou criação.
     * Usa SetOptions.merge() para criar o documento apenas se ele não existir,
     * ou para atualizar dados básicos (nome, foto) sem apagar outros campos como 'isPremium'.
     */
    private void saveUserToFirestoreOnLogin(FirebaseUser user, @NonNull ResultCallback<FirebaseUser> finalCallback) {
        if (user == null) {
            finalCallback.onResult(new Result.Error<>(new Exception("Usuário do Firebase é nulo.")));
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("nome", user.getDisplayName());
        userData.put("email", user.getEmail());
        if (user.getPhotoUrl() != null) {
            userData.put("foto_perfil", user.getPhotoUrl().toString());
        }

        firestore.collection(USUARIOS_COLLECTION).document(user.getUid())
                .set(userData, SetOptions.merge()) // A chave para a operação segura
                .addOnSuccessListener(aVoid -> finalCallback.onResult(new Result.Success<>(user)))
                .addOnFailureListener(e -> finalCallback.onResult(new Result.Error<>(e)));
    }
}