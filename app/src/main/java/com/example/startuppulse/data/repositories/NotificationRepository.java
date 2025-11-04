package com.example.startuppulse.data.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.common.ResultCallback;
import com.example.startuppulse.data.models.AppNotification;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório para salvar e ler notificações do usuário
 * armazenadas no Firestore.
 */
@Singleton // Gerenciado pelo Hilt
public class NotificationRepository {

    private static final String TAG = "NotificationRepo";
    private static final String USERS_COLLECTION = "usuarios";
    private static final String NOTIFICATIONS_COLLECTION = "notificacoes";

    private final FirebaseFirestore firestore;

    @Inject
    public NotificationRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Salva uma nova notificação na subcoleção do usuário no Firestore.
     *
     * @param userId O ID do usuário que receberá a notificação.
     * @param notification O objeto AppNotification a ser salvo.
     * @param callback Retorno de sucesso ou falha.
     */
    public void saveNotification(@NonNull String userId,
                                 @NonNull AppNotification notification,
                                 @NonNull ResultCallback<Void> callback) {

        if (userId.isEmpty()) {
            callback.onResult(new Result.Error<>(new IllegalArgumentException("User ID não pode ser nulo")));
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .add(notification) // O Firestore gerará um ID automático
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Notificação salva com sucesso: " + documentReference.getId());
                    callback.onResult(new Result.Success<>(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar notificação no Firestore", e);
                    callback.onResult(new Result.Error<>(e));
                });
    }

    // (Você pode adicionar outros métodos aqui no futuro, como:)
    // - getNotifications(String userId, ResultCallback<List<AppNotification>> callback)
    // - markAsRead(String userId, String notificationId, ResultCallback<Void> callback)
    // - getUnreadCount(String userId, ResultCallback<Integer> callback)
}