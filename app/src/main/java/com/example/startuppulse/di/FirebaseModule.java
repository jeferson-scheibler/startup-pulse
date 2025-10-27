package com.example.startuppulse.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;

import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class) // Define que estas dependências viverão enquanto o app estiver vivo.
public class FirebaseModule {
    @Provides
    @Singleton
    public FirebaseFirestore provideFirestoreInstance() {
        return FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseAuth provideAuthInstance() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseStorage provideFirebaseStorage() {
        return FirebaseStorage.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseFunctions provideFirebaseFunctions() {
        return FirebaseFunctions.getInstance("southamerica-east1");
    }
}