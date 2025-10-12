package com.example.startuppulse.ui.signup;

public class SignUpState {
    public enum AuthState { IDLE, LOADING, SUCCESS, ERROR }

    private final AuthState state;
    private final String errorMessage;

    public SignUpState(AuthState state) {
        this.state = state;
        this.errorMessage = null;
    }

    public SignUpState(String errorMessage) {
        this.state = AuthState.ERROR;
        this.errorMessage = errorMessage;
    }

    public AuthState getState() { return state; }
    public String getErrorMessage() { return errorMessage; }
}
