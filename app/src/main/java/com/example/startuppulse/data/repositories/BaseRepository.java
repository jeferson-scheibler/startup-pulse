package com.example.startuppulse.data.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * BaseRepository centraliza instâncias Firebase e utilitários comuns.
 */
public abstract class BaseRepository {

    protected final FirebaseAuth auth;
    protected final FirebaseFirestore db;
    protected final FirebaseStorage storage;

    protected BaseRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    protected String getCurrentUserId() {
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
}