package com.example.startuppulse;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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
    }
}