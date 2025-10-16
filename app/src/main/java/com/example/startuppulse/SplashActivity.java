package com.example.startuppulse;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

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
 * Splash com inicializações rápidas e verificação de autenticação.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_TIMEOUT_MS = 1500; // Um pouco mais de tempo para a rede
    private final AtomicBoolean isReadyToProceed = new AtomicBoolean(false);
    private int tasksToComplete = 2; // RemoteConfig + Firestore Warmup

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Mantém a splash na tela até que as tarefas de inicialização terminem
        splash.setKeepOnScreenCondition(() -> !isReadyToProceed.get());

        // Inicia as tarefas de inicialização
        initRemoteConfig();
        warmupFirestore();

        // Um temporizador de segurança para garantir que o app não fique preso na splash
        new Handler(Looper.getMainLooper()).postDelayed(this::proceed, SPLASH_TIMEOUT_MS);
    }

    private void initRemoteConfig() {
        FirebaseRemoteConfig rc = FirebaseRemoteConfig.getInstance();

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("feature_canvas_novo_flow", true);
        defaults.put("ui_home_show_banner", true);
        defaults.put("ui_theme_primary_color", "#0052FF");
        rc.setDefaultsAsync(defaults);

        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 60 : 3600) // 1 min em debug
                .build();
        rc.setConfigSettingsAsync(settings);

        rc.fetchAndActivate().addOnCompleteListener(task -> onTaskCompleted());
    }

    private void warmupFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Se o usuário está logado, tenta pré-carregar os dados dele para um início mais rápido
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnCompleteListener(task -> onTaskCompleted());
        } else {
            // Se não há usuário, a tarefa de "warmup" já está concluída
            onTaskCompleted();
        }
    }
    private synchronized void onTaskCompleted() {
        tasksToComplete--;
        if (tasksToComplete <= 0) {
            proceed();
        }
    }

    private void proceed() {
        if (isReadyToProceed.getAndSet(true)) {
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Finaliza a SplashActivity para que o usuário não possa voltar para ela
        finish();
    }
}