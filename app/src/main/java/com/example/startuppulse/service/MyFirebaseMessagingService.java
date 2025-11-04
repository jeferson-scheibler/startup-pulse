package com.example.startuppulse.service; // Confirme o nome do seu pacote

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.startuppulse.MainActivity; // Importe sua Activity principal
import com.example.startuppulse.R;
import com.example.startuppulse.common.Result; // Importe sua classe Result
import com.example.startuppulse.data.models.AppNotification;
import com.example.startuppulse.data.repositories.NotificationRepository;
import com.example.startuppulse.data.repositories.UserRepository; // Importe seu repositório de usuário
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
import javax.inject.Inject; // Para Hilt
import dagger.hilt.android.AndroidEntryPoint; // Para Hilt

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    // MODIFICAÇÃO: IDs dos Canais de Notificação
    private static final String CHANNEL_ID_AVALIACOES = "STARTUP_PULSE_AVALIACOES_CHANNEL";
    private static final String CHANNEL_ID_INTERACOES = "STARTUP_PULSE_INTERACOES_CHANNEL";
    private static final String CHANNEL_ID_GERAL = "STARTUP_PULSE_GERAL_CHANNEL"; // (Antigo CHANNEL_ID)

    // Injeta o UserRepository para salvar o token FCM no perfil do usuário
    @Inject
    UserRepository userRepository; // Garanta que UserRepository seja fornecido pelo Hilt (@Singleton ou @Provides)

    @Inject
    NotificationRepository notificationRepository;

    /**
     * Chamado quando um novo token FCM é gerado para esta instalação do app.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendRegistrationToServer(token);
    }

    /**
     * Tenta salvar o token FCM no documento do usuário no Firestore.
     * @param token O novo token FCM.
     */
    private void sendRegistrationToServer(String token) {
        // (Este método permanece o mesmo da Etapa 1)
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null && userRepository != null) {
            userRepository.updateFcmToken(userId, token, result -> {
                if (result instanceof Result.Success) {
                    Log.i(TAG, "FCM Token updated successfully in Firestore for user: " + userId);
                } else {
                    Log.w(TAG, "Failed to update FCM Token in Firestore for user: " + userId,
                            result instanceof Result.Error ? ((Result.Error<Void>) result).error : null);
                }
            });
        } else if (userRepository == null) {
            Log.e(TAG, "UserRepository not injected! Cannot save FCM token.");
        } else {
            Log.w(TAG, "User not logged in. FCM token not saved to server yet.");
        }
    }

    /**
     * Chamado quando uma mensagem FCM (apenas 'data') é recebida.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "FCM Message Received From: " + remoteMessage.getFrom());

        Map<String, String> dataPayload = remoteMessage.getData();

        if (dataPayload.isEmpty()) {
            Log.w(TAG, "Received empty data payload. Ignoring message.");
            return;
        }

        Log.d(TAG, "Message Data Payload: " + dataPayload);

        // Define padrões que vêm do app (R.string)
        String messageTitle = dataPayload.getOrDefault("title", getString(R.string.app_name));
        String messageBody = dataPayload.getOrDefault("body", "Você tem uma nova atualização.");

        // MODIFICAÇÃO: Lê o channelId do payload. Padrão para o canal "Geral".
        String channelId = dataPayload.getOrDefault("channelId", CHANNEL_ID_GERAL);
        persistNotification(messageTitle, messageBody, channelId, dataPayload);
        // Cria e exibe a notificação na barra de status do Android
        sendNotification(messageTitle, messageBody, dataPayload, channelId); // MODIFICAÇÃO: Passa o channelId
    }

    /**
     * Salva a notificação recebida no Firestore (subcoleção do usuário).
     */
    private void persistNotification(String title, String body, String channelId, Map<String, String> data) {
        // 1. Verificar se o usuário está logado (mesma lógica do sendRegistrationToServer)
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "User not logged in. Notification will be displayed, but not persisted.");
            return; // Usuário não logado, não há onde salvar
        }

        // 2. Verificar se o Hilt injetou o repositório
        if (notificationRepository == null) {
            Log.e(TAG, "NotificationRepository not injected! Cannot persist notification.");
            return;
        }

        // 3. Obter dados relevantes e criar o objeto
        String ideiaId = data.get("ideiaId");
        AppNotification notification = new AppNotification(title, body, channelId, ideiaId);

        // 4. Salvar no Firestore (disparar e esquecer - "fire-and-forget")
        notificationRepository.saveNotification(userId, notification, result -> {
            if (result instanceof Result.Success) {
                Log.i(TAG, "Notification persisted to Firestore for user: " + userId);
            } else {
                // Apenas logamos o erro. A notificação *ainda* será exibida.
                Log.w(TAG, "Failed to persist notification to Firestore.",
                        result instanceof Result.Error ? ((Result.Error<Void>) result).error : null);
            }
        });
    }

    /**
     * Cria e mostra uma notificação local na barra de status.
     * @param messageTitle Título da notificação.
     * @param messageBody Corpo da notificação.
     * @param data Dados extras da mensagem FCM para usar no clique.
     * @param channelId O ID do canal que esta notificação deve usar.
     */
    // MODIFICAÇÃO: Assinatura do método atualizada
    private void sendNotification(String messageTitle, String messageBody, Map<String, String> data, String channelId) {
        Intent intent;
        String ideiaId = data.get("ideiaId"); // Verifica se a notificação contém um ID de ideia

        // (A lógica do Intent e PendingIntent permanece a mesma)
        if (ideiaId != null && !ideiaId.isEmpty()) {
            Log.d(TAG, "Creating intent to navigate to ideiaId: " + ideiaId);
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("NAVIGATE_TO", "IDEIA_DETAIL");
            intent.putExtra("ideiaId", ideiaId);
        } else {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        int requestCode = (ideiaId != null) ? ideiaId.hashCode() : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // MODIFICAÇÃO: Obter o manager e criar o canal primeiro
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // MODIFICAÇÃO: Garante que o canal correto exista (Obrigatório para Android 8+)
        createNotificationChannelIfNeeded(notificationManager, channelId);


        // MODIFICAÇÃO: Constrói a notificação usando o channelId dinâmico
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_stat_logo)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        // A prioridade é definida pelo canal no Android 8+
                        // mas ainda é útil para versões mais antigas.
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent);

        String notificationTag = data.get("ideiaId");

        // O 'id' é o nosso identificador de tipo dentro do tópico (ex: avaliação vs. interação)
        // Usamos o hashCode do channelId para um 'id' inteiro estável.
        int notificationId = channelId.hashCode();

        Log.d(TAG, "Exibindo notificação com Tag: " + notificationTag + " e ID: " + notificationId);

        // Usa a versão (tag, id) do notify() para empilhar/atualizar notificações
        notificationManager.notify(notificationTag, notificationId, notificationBuilder.build());
    }

    /**
     * MODIFICAÇÃO: NOVO MÉTODO
     * Cria o canal de notificação apropriado (se ainda não existir).
     * Obrigatório para Android 8.0 (Oreo) e superior.
     *
     * @param manager O NotificationManager do sistema.
     * @param channelId O ID do canal a ser criado.
     */
    private void createNotificationChannelIfNeeded(NotificationManager manager, String channelId) {
        // Canais só existem no Android 8 (API 26) e superior
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Se o canal já existe, não faz nada
        if (manager.getNotificationChannel(channelId) != null) {
            return;
        }

        // Configurações do canal (serão definidas no switch)
        CharSequence name;
        String description;
        int importance;

        switch (channelId) {
            case CHANNEL_ID_AVALIACOES:
                name = getString(R.string.notification_channel_name_avaliacoes);
                description = getString(R.string.notification_channel_description_avaliacoes);
                importance = NotificationManager.IMPORTANCE_HIGH; // Crítico
                break;
            case CHANNEL_ID_INTERACOES:
                name = getString(R.string.notification_channel_name_interacoes);
                description = getString(R.string.notification_channel_description_interacoes);
                importance = NotificationManager.IMPORTANCE_DEFAULT; // Padrão
                break;
            case CHANNEL_ID_GERAL:
            default:
                // Se o backend enviar um ID desconhecido, cai no canal Geral
                channelId = CHANNEL_ID_GERAL;
                name = getString(R.string.notification_channel_name_geral);
                description = getString(R.string.notification_channel_description_geral);
                importance = NotificationManager.IMPORTANCE_DEFAULT; // Padrão
                break;
        }

        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);

        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        // Registra o canal no sistema
        Log.i(TAG, "Criando novo canal de notificação: " + channelId);
        manager.createNotificationChannel(channel);
    }
}