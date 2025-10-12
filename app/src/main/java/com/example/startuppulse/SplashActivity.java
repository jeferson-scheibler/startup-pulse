package com.example.startuppulse;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Splash com inicializações rápidas (Remote Config + Firestore warmup) e timeout curto.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_TIMEOUT_MS = 1200; // limite de espera curto
    private final AtomicBoolean isReadyToProceed = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Android 12+: usa o tema Theme.SplashScreen; versões antigas ignoram
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Mantém a splash até que os inits acabem ou o timeout role
        final long start = System.currentTimeMillis();
        splash.setKeepOnScreenCondition(() -> !isReadyToProceed.get());

        // Dispara inicializações em paralelo (não bloqueiam a UI)
        initRemoteConfig();
        warmupFirestore();

        // Fallback por tempo máximo (garante que não “congela”)
        getWindow().getDecorView().postDelayed(this::proceedIfReadyOrForce, SPLASH_TIMEOUT_MS);
    }

    private void initRemoteConfig() {
        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();

        // Defaults locais (para não depender da rede)
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("feature_canvas_novo_flow", true);
        defaults.put("ui_home_show_banner", true);
        defaults.put("ui_theme_primary_color", "#0052FF");
        rc.setDefaultsAsync(defaults);

        // Intervalo mínimo de fetch (em ms). Para debug, pode usar 0.
        FirebaseRemoteConfigSettings settings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : 3600)
                        .build();
        rc.setConfigSettingsAsync(settings);

        // Tenta buscar e ativar; não precisa esperar terminar pra seguir,
        // mas vamos contar como “um dos inits”:
        rc.fetchAndActivate()
                .addOnCompleteListener(task -> markOneInitDone());
    }

    private void warmupFirestore() {
        // Tocar no singleton inicializa os componentes internos
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Se houver usuário logado, tenta puxar o doc dele rapidamente (best-effort).
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid())
                    .get()
                    .addOnCompleteListener(this::onUserDocPrefetched);
        } else {
            // Não há usuário — ainda assim marcamos este init como concluído
            markOneInitDone();
        }
    }

    private void onUserDocPrefetched(Task<?> task) {
        // Não importa sucesso/erro aqui — é apenas aquecimento
        markOneInitDone();
    }

    // ==== Coordenação simples dos “dois inits” (RemoteConfig + Firestore) ====
    private int initsDone = 0;
    private final Object lock = new Object();

    private void markOneInitDone() {
        synchronized (lock) {
            initsDone++;
            // Assim que os dois inits terminarem, seguimos (sem esperar o timeout)
            if (initsDone >= 2) {
                proceed();
            }
        }
    }

    private void proceedIfReadyOrForce() {
        // Se ainda não marcou ready, força seguir após o timeout
        if (!isReadyToProceed.get()) {
            proceed();
        }
    }

    private void proceed() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finaliza a SplashActivity
    }
}