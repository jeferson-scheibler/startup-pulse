package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.startuppulse.databinding.ActivityLoginBinding; // Importe sua classe de binding
import com.example.startuppulse.ui.login.LoginState;
import com.example.startuppulse.ui.login.LoginViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding; // Usando View Binding
    private LoginViewModel loginViewModel;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_StartupPulse); // Retorna o tema após o Splash
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Inicializar ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 2. Configurar a UI e os listeners
        setupClickListeners();
        configureGoogleSignIn();

        // 3. Observar o estado do ViewModel para atualizar a UI
        loginViewModel.loginState.observe(this, this::handleLoginState);
    }

    private void setupClickListeners() {
        // Login com E-mail
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            loginViewModel.loginWithEmail(email, password);
        });

        // Login com Google
        binding.googleButtonLayout.setOnClickListener(v -> {
            // Limpa qualquer sessão anterior para sempre mostrar o seletor de contas
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });

        // Navegar para a tela de Cadastro
        binding.signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    private void configureGoogleSignIn() {
        // Configura o Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Prepara o launcher que receberá o resultado da tela de login do Google
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                // SUCESSO: Envia a conta para o ViewModel processar
                                loginViewModel.loginWithGoogle(account);
                            } else {
                                // ERRO: Conta nula
                                handleLoginState(new LoginState("Não foi possível obter a conta Google."));
                            }
                        } catch (ApiException e) {
                            // ERRO: Falha na API do Google
                            Log.w(TAG, "Google sign in failed", e);
                            handleLoginState(new LoginState("Falha no login com Google. Código: " + e.getStatusCode()));
                        }
                    } else {
                        // A tela do Google foi cancelada pelo usuário
                        handleLoginState(new LoginState(LoginState.AuthState.IDLE));
                    }
                }
        );
    }

    private void handleLoginState(LoginState state) {
        // Centraliza toda a lógica de atualização da UI aqui
        switch (state.getState()) {
            case LOADING:
                setLoadingState(true);
                break;
            case SUCCESS:
                setLoadingState(false);
                navigateToMainApp();
                break;
            case ERROR:
                setLoadingState(false);
                Toast.makeText(this, state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
            case IDLE:
            default:
                setLoadingState(false);
                break;
        }
    }

    private void setLoadingState(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!isLoading);
        binding.googleButtonLayout.setEnabled(!isLoading);
        binding.signUpTextView.setEnabled(!isLoading);
    }

    private void navigateToMainApp() {
        Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finaliza a LoginActivity para que o usuário não possa voltar a ela
    }
}