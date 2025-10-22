package com.example.startuppulse.util;

public interface ConfirmationDialogListener {
    /**
     * Chamado quando o botão positivo do diálogo de confirmação é clicado.
     * @param dialogTag Uma tag opcional para identificar qual diálogo foi confirmado.
     */
    void onConfirm(String dialogTag);

    // Opcional: Adicionar onCancel se precisar tratar o botão negativo
    // void onCancel(String dialogTag);
}