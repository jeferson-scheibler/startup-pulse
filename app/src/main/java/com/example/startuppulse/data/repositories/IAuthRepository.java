package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.Result;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public interface IAuthRepository {
    Task<AuthResult> signIn(String email, String password);
    Task<AuthResult> register(String email, String password);
    void signOut();
    boolean isLoggedIn();
    String getCurrentUserId();
}
