package com.example.startuppulse.util;

public class DialogEvent {
    public enum Type {
        LOADING,
        HIDE,
        NO_MENTOR_FOUND
    }
    public final Type type;
    public final String message;

    public DialogEvent(Type type, String message) {
        this.type = type;
        this.message = message;
    }
}