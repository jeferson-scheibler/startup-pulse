package com.example.startuppulse.ui.signup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SignUpViewModel extends ViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<SignUpState> _signUpState = new MutableLiveData<>(new SignUpState(SignUpState.AuthState.IDLE));
    public final LiveData<SignUpState> signUpState = _signUpState;

    @Inject
    public SignUpViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public void signUp(String name, String email, String password) {
        // Validações de entrada
        if (name == null || name.trim().isEmpty()) {
            _signUpState.setValue(new SignUpState("O nome é obrigatório."));
            return;
        }
        if (email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _signUpState.setValue(new SignUpState("E-mail inválido."));
            return;
        }
        if (password == null || password.trim().length() < 6) {
            _signUpState.setValue(new SignUpState("A senha deve ter pelo menos 6 caracteres."));
            return;
        }

        _signUpState.setValue(new SignUpState(SignUpState.AuthState.LOADING));

        repository.createUser(name, email, password, result -> {
            if (result instanceof Result.Success) {
                _signUpState.setValue(new SignUpState(SignUpState.AuthState.SUCCESS));
            }
            if (result instanceof Result.Error) {
                _signUpState.setValue(new SignUpState(SignUpState.AuthState.ERROR));
            }
        });
    }
}