package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.startuppulse.databinding.ActivitySignUpBinding; // Importe sua classe de binding
import com.example.startuppulse.ui.signup.SignUpState;
import com.example.startuppulse.ui.signup.SignUpViewModel;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private SignUpViewModel signUpViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Inicializar ViewModel
        signUpViewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        // 2. Configurar os listeners da UI
        setupClickListeners();

        // 3. Observar o estado para atualizar a UI
        signUpViewModel.signUpState.observe(this, this::handleSignUpState);
    }

    private void setupClickListeners() {
        binding.signUpButton.setOnClickListener(v -> {
            String name = binding.nameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            signUpViewModel.signUp(name, email, password);
        });

        // Listener para voltar para a tela de login
        binding.loginTextView.setOnClickListener(v -> {
            // Finaliza a tela de cadastro e volta para a de login que já está na pilha
            finish();
        });
    }

    private void handleSignUpState(SignUpState state) {
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
        binding.signUpButton.setEnabled(!isLoading);
        binding.loginTextView.setEnabled(!isLoading);
    }

    private void navigateToMainApp() {
        Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        // Limpa a pilha de activities para que o usuário não volte para as telas de login/cadastro
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}