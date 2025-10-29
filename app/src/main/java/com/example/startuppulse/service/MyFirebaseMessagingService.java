package com.example.startuppulse.service; // Confirme o nome do seu pacote

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
import com.example.startuppulse.data.repositories.UserRepository; // Importe seu repositório de usuário
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
import javax.inject.Inject; // Para Hilt
import dagger.hilt.android.AndroidEntryPoint; // Para Hilt

@AndroidEntryPoint // Habilita injeção de dependência via Hilt nesta classe
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    // ID do Canal de Notificação (será criado para Android 8+)
    private static final String CHANNEL_ID = "STARTUP_PULSE_GENERAL_CHANNEL";

    // Injeta o UserRepository para salvar o token FCM no perfil do usuário
    @Inject
    UserRepository userRepository; // Garanta que UserRepository seja fornecido pelo Hilt (@Singleton ou @Provides)

    /**
     * Chamado quando um novo token FCM é gerado para esta instalação do app.
     * Isso acontece na primeira inicialização, quando o token expira, ou quando dados do app são limpos.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        // Envia o novo token para ser salvo no Firestore associado ao usuário logado (se houver)
        sendRegistrationToServer(token);
    }

    /**
     * Tenta salvar o token FCM no documento do usuário no Firestore.
     * @param token O novo token FCM.
     */
    private void sendRegistrationToServer(String token) {
        // Obtém o ID do usuário atualmente logado
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null && userRepository != null) {
            // Chama o método no UserRepository (que você precisará criar)
            userRepository.updateFcmToken(userId, token, result -> {
                if (result instanceof Result.Success) {
                    Log.i(TAG, "FCM Token updated successfully in Firestore for user: " + userId);
                } else {
                    Log.w(TAG, "Failed to update FCM Token in Firestore for user: " + userId,
                            result instanceof Result.Error ? ((Result.Error<Void>) result).error : null);
                    // Considere implementar uma lógica de retentativa aqui se falhar
                }
            });
        } else if (userRepository == null) {
            Log.e(TAG, "UserRepository not injected! Cannot save FCM token.");
            // Isso indica um problema na configuração do Hilt para este Service.
        } else {
            Log.w(TAG, "User not logged in. FCM token not saved to server yet.");
            // O token será salvo na próxima vez que o usuário logar e onNewToken for chamado,
            // ou você pode adicionar lógica para salvar o token no momento do login.
        }
    }

    /**
     * Chamado quando uma mensagem FCM (notificação ou dados) é recebida enquanto o app está
     * em primeiro plano ou segundo plano (dependendo do tipo de mensagem).
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "FCM Message Received From: " + remoteMessage.getFrom());

        String messageTitle = getString(R.string.app_name); // Título padrão
        String messageBody = "Você tem uma nova atualização."; // Corpo padrão
        Map<String, String> dataPayload = remoteMessage.getData(); // Dados extras enviados

        // Prioriza o payload de notificação (enviado via console Firebase ou `notification` no backend)
        if (remoteMessage.getNotification() != null) {
            messageTitle = remoteMessage.getNotification().getTitle() != null ?
                    remoteMessage.getNotification().getTitle() : messageTitle;
            messageBody = remoteMessage.getNotification().getBody() != null ?
                    remoteMessage.getNotification().getBody() : messageBody;
            Log.d(TAG, "Message Notification Payload: Title='" + messageTitle + "', Body='" + messageBody + "'");
        }
        // Se não houver payload de notificação, verifica se há dados (enviado via `data` no backend)
        else if (dataPayload.size() > 0) {
            Log.d(TAG, "Message Data Payload: " + dataPayload);
            // Tenta extrair título e corpo do payload de dados
            messageTitle = dataPayload.getOrDefault("title", messageTitle);
            messageBody = dataPayload.getOrDefault("body", messageBody);
        }

        // Cria e exibe a notificação na barra de status do Android
        sendNotification(messageTitle, messageBody, dataPayload);
    }

    /**
     * Cria e mostra uma notificação local na barra de status.
     * @param messageTitle Título da notificação.
     * @param messageBody Corpo da notificação.
     * @param data Dados extras da mensagem FCM para usar no clique.
     */
    private void sendNotification(String messageTitle, String messageBody, Map<String, String> data) {
        Intent intent;
        String ideiaId = data.get("ideiaId"); // Verifica se a notificação contém um ID de ideia

        // Cria o Intent que será disparado ao clicar na notificação
        if (ideiaId != null && !ideiaId.isEmpty()) {
            Log.d(TAG, "Creating intent to navigate to ideiaId: " + ideiaId);
            // Intent para abrir a MainActivity e passar dados para navegação
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Reutiliza a Activity se já aberta
            intent.putExtra("NAVIGATE_TO", "IDEIA_DETAIL"); // Sinaliza para MainActivity
            intent.putExtra("ideiaId", ideiaId); // Passa o ID da ideia
        } else {
            // Intent padrão para simplesmente abrir a MainActivity
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        // Cria um PendingIntent para encapsular o Intent
        // FLAG_IMMUTABLE é necessário para Android 12+
        // FLAG_ONE_SHOT garante que o PendingIntent só seja usado uma vez
        // Usamos requestCode 0 e ideiaId.hashCode() para tentar diferenciar intents se necessário
        int requestCode = (ideiaId != null) ? ideiaId.hashCode() : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Som padrão de notificação
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Constrói a notificação usando NotificationCompat
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_logo) // <<-- CRIE ESTE ÍCONE MONOCROMÁTICO!!
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true) // Remove a notificação ao clicar
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Prioridade padrão
                        .setContentIntent(pendingIntent); // Define a ação de clique

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Cria o Canal de Notificação (Obrigatório para Android 8.0 Oreo e superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Nome visível para o usuário nas configurações do app
            CharSequence name = getString(R.string.notification_channel_name);
            // Descrição visível para o usuário
            String description = getString(R.string.notification_channel_description);
            // Importância do canal (afeta como a notificação é exibida)
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Registra o canal no sistema
            notificationManager.createNotificationChannel(channel);
        }

        // Exibe a notificação. Usamos um ID fixo (0) ou um ID único por notificação.
        int notificationId = (int) System.currentTimeMillis(); // ID baseado no tempo para evitar sobrepor notificações idênticas rapidamente
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}