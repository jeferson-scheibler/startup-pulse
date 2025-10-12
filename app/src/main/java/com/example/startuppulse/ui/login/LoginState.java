package com.example.startuppulse.ui.login;

public class LoginState {
    public enum AuthState { IDLE, LOADING, SUCCESS, ERROR }

    private final AuthState state;
    private final String errorMessage;

    // Construtor para estados sem erro
    public LoginState(AuthState state) {
        this.state = state;
        this.errorMessage = null;
    }

    // Construtor para estado de erro
    public LoginState(String errorMessage) {
        this.state = AuthState.ERROR;
        this.errorMessage = errorMessage;
    }

    public AuthState getState() { return state; }
    public String getErrorMessage() { return errorMessage; }
}