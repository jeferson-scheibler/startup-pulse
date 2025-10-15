package com.example.startuppulse.ui.signup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentSignUpBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SignUpFragment extends Fragment {

    private FragmentSignUpBinding binding;
    private SignUpViewModel signUpViewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        signUpViewModel = new ViewModelProvider(requireActivity()).get(SignUpViewModel.class);

        setupClickListeners();

        signUpViewModel.signUpState.observe(getViewLifecycleOwner(), this::handleSignUpState);
    }

    private void setupClickListeners() {
        binding.signUpButton.setOnClickListener(v -> {
            String name = binding.nameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            signUpViewModel.signUp(name, email, password);
        });

        binding.loginTextView.setOnClickListener(v -> {
            // Volta para o fragment anterior na pilha (o LoginFragment)
            navController.navigateUp();
        });
    }

    private void handleSignUpState(SignUpState state) {
        if (getView() == null) return;
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
        binding.signUpButton.setEnabled(!isLoading);
        binding.loginTextView.setEnabled(!isLoading);
    }

    private void navigateToMainApp() {
        Toast.makeText(requireContext(), "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
        // Usa a ação do nav_graph para ir para o fluxo principal, limpando a pilha de volta
        navController.navigate(R.id.action_signUpFragment_to_mainHostFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}