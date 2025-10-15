package com.example.startuppulse.data;

import static android.content.ContentValues.TAG;

import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private static final String USERS_COLLECTION = "users"; // Nome consistente com as regras do Firestore

    @Inject
    public AuthRepository(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    // --- MÉTODOS DE AUTENTICAÇÃO E SESSÃO ---

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        return (currentUser != null) ? currentUser.getUid() : null;
    }

    public boolean isCurrentUser(String userId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    public void loginWithEmail(String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> saveUserToFirestore(authResult.getUser(), callback))
                .addOnFailureListener(callback::onError);
    }

    public void loginWithGoogle(GoogleSignInAccount googleAccount, ResultCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> saveUserToFirestore(authResult.getUser(), callback))
                .addOnFailureListener(callback::onError);
    }

    public void createUser(String name, String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name).build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> saveUserToFirestore(user, callback));
                    } else {
                        callback.onError(new Exception("Falha ao obter o usuário após a criação."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }


    // --- MÉTODOS DE PERFIL DE USUÁRIO ---

    public interface UserProfileCallback {
        void onProfileLoaded(User user);
    }

    public void getUserProfile(String uid, final UserProfileCallback callback) {
        firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Lógica para preencher dados que não vêm diretamente do Firestore, se houver.
                            // Exemplo: user.setDiasDeConta(...);
                        }
                        // CORREÇÃO: Chama o callback com o usuário em caso de sucesso.
                        callback.onProfileLoaded(user);
                    } else {
                        // CORREÇÃO: Chama o callback com null se o documento não existir.
                        Log.w(TAG, "Documento do usuário não encontrado para o UID: " + uid);
                        callback.onProfileLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // CORREÇÃO: Chama o callback com null em caso de erro.
                    Log.e(TAG, "Erro ao buscar perfil do usuário", e);
                    callback.onProfileLoaded(null);
                });
    }

    private void saveUserToFirestore(FirebaseUser user, ResultCallback<FirebaseUser> finalCallback) {
        if (user == null) {
            finalCallback.onError(new Exception("Usuário do Firebase é nulo."));
            return;
        }

        firestore.collection(USERS_COLLECTION).document(user.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("nome", user.getDisplayName()); // "nome" em vez de "name"
                userData.put("email", user.getEmail());
                if (user.getPhotoUrl() != null) {
                    userData.put("foto_perfil", user.getPhotoUrl().toString());
                }
                firestore.collection(USERS_COLLECTION).document(user.getUid())
                        .set(userData)
                        .addOnSuccessListener(aVoid -> finalCallback.onSuccess(user))
                        .addOnFailureListener(finalCallback::onError);
            } else if (task.isSuccessful()) {
                finalCallback.onSuccess(user);
            } else {
                finalCallback.onError(task.getException());
            }
        });
    }
}