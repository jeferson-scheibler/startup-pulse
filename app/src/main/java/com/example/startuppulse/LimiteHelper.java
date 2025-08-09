
package com.example.startuppulse;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LimiteHelper {

    public interface LimiteCallback {
        void onPermitido();
        void onNegado(String mensagem);
    }

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLECAO = "limites";

    public static void verificarAcessoIdeia(Context context, LimiteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onNegado("Usuário não autenticado.");
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("limites").document(uid);
        Timestamp agora = Timestamp.now();

        isPlanoPremiumValido(isPremium -> {
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    docRef.set(Collections.singletonMap("data_ultimo_acesso", agora));
                    callback.onPermitido();
                    return;
                }

                Timestamp ultimo = documentSnapshot.getTimestamp("data_ultimo_acesso");

                if (isPremium || ultimo == null || !isMesmoDia(ultimo.toDate(), agora.toDate())) {
                    docRef.update("data_ultimo_acesso", agora);
                    callback.onPermitido();
                } else {
                    callback.onNegado("Limite diário atingido.");
                }

            }).addOnFailureListener(e -> {
                Log.e("LimiteHelper", "Erro no acesso Firestore", e);
                callback.onNegado("Erro ao verificar acesso.");
            });
        });
    }



    private static void isPlanoPremiumValido(PlanoCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(false);
            return;
        }

        FirebaseFirestore.getInstance().collection("premium")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp dataFim = documentSnapshot.getTimestamp("data_fim");
                        boolean valido = dataFim != null && dataFim.toDate().after(new Date());
                        callback.onResult(valido);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LimiteHelper", "Erro ao verificar plano: ", e);
                    callback.onResult(false);
                });
    }

    // Callback para verificar plano
    private interface PlanoCallback {
        void onResult(boolean isPremiumValido);
    }

    private static boolean isMesmoDia(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean passouUmaSemana(Date ultima, Date agora) {
        long diferencaMillis = agora.getTime() - ultima.getTime();
        long dias = TimeUnit.MILLISECONDS.toDays(diferencaMillis);
        return dias >= 7;
    }


    public static void verificarPublicacaoIdeia(Context context, LimiteCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onNegado("Usuário não autenticado.");
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("limites").document(uid);
        Timestamp agora = Timestamp.now();

        isPlanoPremiumValido(isPremium -> {
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Map<String, Object> dados = new HashMap<>();
                    dados.put("data_ultima_publicacao", agora);
                    docRef.set(dados);
                    callback.onPermitido();
                    return;
                }

                Timestamp ultima = documentSnapshot.getTimestamp("data_ultima_publicacao");

                if (isPremium || ultima == null || passouUmaSemana(ultima.toDate(), agora.toDate())) {
                    docRef.update("data_ultima_publicacao", agora);
                    callback.onPermitido();
                } else {
                    callback.onNegado("Limite semanal de publicação atingido.");
                }

            }).addOnFailureListener(e -> {
                Log.e("LimiteHelper", "Erro ao verificar limite de publicação", e);
                callback.onNegado("Erro ao verificar publicação.");
            });
        });
    }

    public interface LimiteDataCallback {
        void onResult(String data);
    }

    public static void getProximaDataAcessoFormatada(String uid, LimiteDataCallback callback) {
        FirebaseFirestore.getInstance().collection("limites")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Timestamp ultimoAcesso = documentSnapshot.getTimestamp("data_ultimo_acesso");

                    if (ultimoAcesso != null) {
                        Calendar calendario = Calendar.getInstance();
                        calendario.setTime(ultimoAcesso.toDate());
                        calendario.set(Calendar.HOUR_OF_DAY, 0);
                        calendario.set(Calendar.MINUTE, 0);
                        calendario.set(Calendar.SECOND, 0);
                        calendario.set(Calendar.MILLISECOND, 0);
                        calendario.add(Calendar.DATE, 1);

                        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String proximaData = formato.format(calendario.getTime());
                        callback.onResult(proximaData);
                    } else {
                        callback.onResult("Data desconhecida");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LimiteHelper", "Erro ao obter próxima data de acesso", e);
                    callback.onResult("Erro ao consultar");
                });
    }


}
