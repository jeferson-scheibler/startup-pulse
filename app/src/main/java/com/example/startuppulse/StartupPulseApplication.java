package com.example.startuppulse;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

@HiltAndroidApp
public class StartupPulseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializa o Firebase (normal)
        FirebaseApp.initializeApp(this);

        // Inicializa o App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );
    }
}