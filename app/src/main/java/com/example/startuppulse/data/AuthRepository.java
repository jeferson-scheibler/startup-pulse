package com.example.startuppulse.data;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore; // Usar diretamente ou via helper

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private static volatile AuthRepository instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore; // Abstração do FirestoreHelper

    private AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository();
                }
            }
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // --- Login com Email e Senha ---
    public void loginWithEmail(String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Após o login, garantimos que os dados do usuário estão no Firestore
                    saveUserToFirestore(authResult.getUser(), callback);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- Login com Google ---
    public void loginWithGoogle(GoogleSignInAccount googleAccount, ResultCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // Após o login, salvamos ou atualizamos os dados do usuário no Firestore
                    saveUserToFirestore(authResult.getUser(), callback);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- Lógica de salvar usuário (antes no FirestoreHelper) ---
    private void saveUserToFirestore(FirebaseUser user, ResultCallback<FirebaseUser> finalCallback) {
        if (user == null) {
            finalCallback.onError(new Exception("Usuário do Firebase é nulo."));
            return;
        }

        // Caminho do documento do usuário
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().exists()) {
                        // O usuário não existe no Firestore, vamos criá-lo
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        if (user.getPhotoUrl() != null) {
                            userData.put("photoUrl", user.getPhotoUrl().toString());
                        }

                        firestore.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> finalCallback.onSuccess(user))
                                .addOnFailureListener(finalCallback::onError);
                    } else if (task.isSuccessful()) {
                        // Usuário já existe, login bem-sucedido
                        finalCallback.onSuccess(user);
                    } else {
                        // Erro ao verificar a existência do usuário
                        finalCallback.onError(task.getException());
                    }
                });
    }

    public void createUser(String name, String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Após criar, atualizamos o perfil com o nome fornecido
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Com o perfil atualizado, salvamos no Firestore
                                        saveUserToFirestore(user, callback);
                                    } else {
                                        // Se a atualização do perfil falhar, ainda consideramos um erro
                                        callback.onError(task.getException());
                                    }
                                });
                    } else {
                        callback.onError(new Exception("Falha ao obter o usuário após a criação."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void logout() {
        firebaseAuth.signOut();
    }
}