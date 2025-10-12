package com.example.startuppulse.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private LoginViewModel loginViewModel;
    private NavController navController;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configura o Google Sign-In e o launcher que recebe o resultado
        configureGoogleSignIn();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Infla o layout usando View Binding
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializa o NavController e o ViewModel
        navController = Navigation.findNavController(view);
        // Usamos requireActivity() para que o ViewModel sobreviva à troca de fragments
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        setupClickListeners();

        // Observa o estado do ViewModel para atualizar a UI
        loginViewModel.loginState.observe(getViewLifecycleOwner(), this::handleLoginState);
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            loginViewModel.loginWithEmail(email, password);
        });

        binding.googleButtonLayout.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });

        binding.signUpTextView.setOnClickListener(v -> {
            // Usa a ação definida no nav_graph.xml para navegar
            navController.navigate(R.id.action_loginFragment_to_signUpFragment);
        });
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Usa requireContext() para obter o contexto do Fragment
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // O código aqui é o mesmo da Activity, pois não depende do contexto diretamente
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                loginViewModel.loginWithGoogle(account);
                            } else {
                                handleLoginState(new LoginState("Não foi possível obter a conta Google."));
                            }
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            handleLoginState(new LoginState("Falha no login com Google. Código: " + e.getStatusCode()));
                        }
                    } else {
                        handleLoginState(new LoginState(LoginState.AuthState.IDLE));
                    }
                }
        );
    }

    private void handleLoginState(LoginState state) {
        if (getView() == null) return; // Garante que o fragment ainda está na tela
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
                Toast.makeText(requireContext(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
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
        Toast.makeText(requireContext(), "Login bem-sucedido!", Toast.LENGTH_SHORT).show();
        // Usa a ação do nav_graph para ir para o fluxo principal, limpando a pilha de volta
        navController.navigate(R.id.action_loginFragment_to_mainHostFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpa a referência ao binding para evitar vazamentos de memória
        binding = null;
    }
}