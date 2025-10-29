package com.example.startuppulse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.startuppulse.databinding.ActivityMainBinding;
import com.example.startuppulse.ui.main.MainViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel mainViewModel;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Pega o NavController principal
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Inicializa o ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Observa o estado de autenticação
        mainViewModel.authenticationState.observe(this, authState -> {
            if (authState == MainViewModel.AuthenticationState.UNAUTHENTICATED) {
                // Se o usuário for deslogado, e não estiver já na tela de login,
                // navega de volta para o login, limpando tudo.
                if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.loginFragment) {
                    navController.navigate(R.id.action_global_return_to_login);
                }
            }
        });

        askNotificationPermission();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent); // Processa intents recebidos enquanto a activity está aberta
    }

    private void handleIntent(Intent intent) {
        // ... (código já fornecido para verificar "NAVIGATE_TO" e "ideiaId") ...
        if (intent != null && intent.getExtras() != null) {
            String navigateTo = intent.getStringExtra("NAVIGATE_TO");
            String ideiaId = intent.getStringExtra("ideiaId");

            Log.d("MainActivity", "handleIntent: navigateTo=" + navigateTo + ", ideiaId=" + ideiaId);

            if ("IDEIA_DETAIL".equals(navigateTo) && ideiaId != null) {
                NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment); // Use o ID do seu NavHostFragment
                if (navHostFragment != null) {
                    NavController navController = navHostFragment.getNavController();
                    Bundle args = new Bundle();
                    args.putString("ideiaId", ideiaId);
                    try {
                        // Tenta navegar para o CanvasIdeiaFragment
                        navController.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
                        // Limpa os extras para não navegar de novo
                        intent.removeExtra("NAVIGATE_TO");
                        intent.removeExtra("ideiaId");
                    } catch (Exception e) { // Captura Exception genérica por segurança
                        Log.e("MainActivity", "Erro ao navegar via notificação.", e);
                        // Opcional: Tenta navegar para a tela inicial como fallback
                        // navController.navigate(R.id.navigation_ideias); // Exemplo
                    }
                }
            }
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("Permission", "Notification permission granted.");
                    // Permissão concedida
                } else {
                    Log.w("Permission", "Notification permission denied.");
                    // Permissão negada, informe o usuário que ele não receberá notificações
                }
            });

    private void askNotificationPermission() {
        // Apenas para Tiramisu (API 33) e superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permissão já concedida
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Mostrar UI explicando por que a permissão é necessária (opcional)
                // e então pedir a permissão: requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                // Por simplicidade, pedimos diretamente:
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Pedir a permissão diretamente
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}