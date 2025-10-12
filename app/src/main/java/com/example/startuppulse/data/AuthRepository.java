package com.example.startuppulse.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    // Usa um Callback para lidar com o resultado assíncrono
    public void login(String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }
}
