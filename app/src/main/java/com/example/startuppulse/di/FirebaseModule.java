package com.example.startuppulse.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class) // Define que estas dependências viverão enquanto o app estiver vivo.
public class FirebaseModule {

    /**
     * Este método ensina ao Hilt como prover uma instância única (Singleton) do FirebaseFirestore.
     */
    @Provides
    @Singleton
    public FirebaseFirestore provideFirestoreInstance() {
        return FirebaseFirestore.getInstance();
    }

    /**
     * Este método ensina ao Hilt como prover uma instância única (Singleton) do FirebaseAuth.
     */
    @Provides
    @Singleton
    public FirebaseAuth provideAuthInstance() {
        return FirebaseAuth.getInstance();
    }
}