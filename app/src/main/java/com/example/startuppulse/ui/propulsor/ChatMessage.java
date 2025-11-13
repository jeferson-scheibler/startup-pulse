package com.example.startuppulse.ui.propulsor;

/**
 * Modelo de dados simples para representar uma mensagem no chat do Propulsor.
 */
public class ChatMessage {
    private final String text;
    private final boolean isUser;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }
}