package com.example.startuppulse.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
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
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private static final String USUARIOS_COLLECTION = "usuarios"; // Padronizado

    @Inject
    public AuthRepository(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    // --- MÉTODOS DE SESSÃO ---

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        return (currentUser != null) ? currentUser.getUid() : null;
    }

    public boolean isCurrentUser(@Nullable String userId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && userId != null && currentUserId.equals(userId);
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    // --- MÉTODOS DE AUTENTICAÇÃO ---

    public void loginWithEmail(String email, String password, @NonNull ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onResult(new Result.Success<>(authResult.getUser())))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void loginWithGoogle(GoogleSignInAccount googleAccount, @NonNull ResultCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // Após o login, garante que o usuário exista no Firestore
                    saveUserToFirestoreOnLogin(authResult.getUser(), callback);
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    public void createUser(String name, String email, String password, @NonNull ResultCallback<FirebaseUser> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Atualiza o nome no Firebase Auth
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    // E então, salva no Firestore
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
     * (Método migrado e aprimorado)
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
     * (Método migrado)
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