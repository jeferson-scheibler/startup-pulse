package com.example.startuppulse.data.repositories;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório central para autenticação e gerenciamento de dados de perfil do usuário.
 * Gerenciado pelo Hilt como um Singleton para toda a aplicação.
 */
@Singleton
public class AuthRepository implements IAuthRepository{
    private static final String USUARIOS_COLLECTION = "usuarios";
    private static final String PREMIUM_COLLECTION = "premium";
    private static final String TAG = "AuthRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    @Inject
    public AuthRepository(FirebaseFirestore firestore, FirebaseAuth firebaseAuth) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
    }

    // --- MÉTODOS DE SESSÃO ---

    @Override
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    @Override
    public boolean isCurrentUser(@Nullable String userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(getCurrentUserId());
    }

    @Override
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    @Override
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public void logout() {
        firebaseAuth.signOut();
    }

    // --- MÉTODOS DE AUTENTICAÇÃO ---

    @Override
    public void loginWithEmail(@NonNull String email, @NonNull String password, @NonNull ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password) // CORRIGIDO: Usa firebaseAuth
                .addOnSuccessListener(authResult -> callback.onResult(new Result.Success<>(authResult.getUser())))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void loginWithGoogle(@NonNull GoogleSignInAccount googleAccount, @NonNull ResultCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential) // CORRIGIDO: Usa firebaseAuth
                .addOnSuccessListener(authResult -> {
                    saveUserToFirestoreOnLogin(authResult.getUser(), callback);
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void createUser(@NonNull String name, @NonNull String email, @NonNull String password, @NonNull ResultCallback<FirebaseUser> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password) // CORRIGIDO: Usa firebaseAuth
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    saveUserToFirestoreOnLogin(user, callback);
                                });
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Falha ao obter o usuário após a criação.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    // --- MÉTODOS DE PERFIL DE USUÁRIO (FIRESTORE) ---

    /**
     * Busca os dados de um perfil de usuário a partir do seu ID.
     */
    @Override
    public void getUserProfile(@NonNull String userId, @NonNull ResultCallback<User> callback) {
        Log.d(TAG, "getUserProfile: Buscando perfil combinado para userId: " + userId);

        // Referência para o documento do usuário
        DocumentReference userDocRef = firestore.collection(USUARIOS_COLLECTION).document(userId);
        // Referência para o documento premium
        DocumentReference premiumDocRef = firestore.collection(PREMIUM_COLLECTION).document(userId);

        // 1. Busca os dados básicos do usuário
        userDocRef.get().addOnSuccessListener(userSnapshot -> {
            if (userSnapshot != null && userSnapshot.exists()) {
                // Converte para o objeto User (pode ter dados premium nulos/padrão)
                User user = userSnapshot.toObject(User.class);
                if (user == null) {
                    // Erro na conversão
                    Log.e(TAG, "getUserProfile: Falha ao converter userSnapshot para User object.");
                    callback.onResult(new Result.Error<>(new Exception("Erro ao processar dados do usuário.")));
                    return;
                }

                // 2. Busca os dados da assinatura premium
                premiumDocRef.get().addOnSuccessListener(premiumSnapshot -> {
                    if (premiumSnapshot != null && premiumSnapshot.exists()) {
                        Log.d(TAG, "getUserProfile: Documento premium encontrado.");
                        // Extrai os dados premium
                        Timestamp dataFim = premiumSnapshot.getTimestamp("data_fim");
                        Boolean ativo = premiumSnapshot.getBoolean("ativo"); // Ou use o campo isPremium se existir

                        // 3. Combina os dados premium no objeto User
                        if (ativo != null) {
                            user.setPremium(ativo); // Define o status premium
                        }
                        if (dataFim != null) {
                            user.setDataExpiracaoPlano(dataFim.toDate()); // Define a data de expiração
                            Log.d(TAG, "getUserProfile: Data Fim Premium: " + dataFim.toDate());
                        } else {
                            user.setDataExpiracaoPlano(null); // Garante que esteja nulo se não houver data
                            user.setPremium(false); // Considera não premium se não houver data_fim válida? (Decida a regra)
                            Log.d(TAG, "getUserProfile: Documento premium existe, mas sem data_fim.");
                        }
                    } else {
                        // Documento premium não existe, garante que o usuário não seja premium
                        Log.d(TAG, "getUserProfile: Documento premium NÃO encontrado.");
                        user.setPremium(false);
                        user.setDataExpiracaoPlano(null);
                    }

                    // Retorna o objeto User combinado
                    Log.d(TAG, "getUserProfile COMBINED SUCCESS. isPremium=" + user.isPremium());
                    callback.onResult(new Result.Success<>(user));

                }).addOnFailureListener(ePremium -> {
                    // Falha ao buscar dados premium, retorna o usuário básico mas loga o erro
                    Log.e(TAG, "getUserProfile: Falha ao buscar dados premium. Retornando usuário básico.", ePremium);
                    user.setPremium(false); // Assume não premium em caso de erro
                    user.setDataExpiracaoPlano(null);
                    callback.onResult(new Result.Success<>(user)); // Ou pode retornar Erro se preferir
                });

            } else {
                // Documento principal do usuário não encontrado
                Log.w(TAG, "getUserProfile: Documento principal do usuário não encontrado para userId: " + userId);
                callback.onResult(new Result.Error<>(new Exception("Perfil de usuário principal não encontrado.")));
            }
        }).addOnFailureListener(eUser -> {
            // Falha ao buscar dados básicos do usuário
            Log.e(TAG, "getUserProfile: Falha ao buscar dados básicos do usuário.", eUser);
            callback.onResult(new Result.Error<>(eUser));
        });
    }

    /**
     * Atualiza o status de premium de um usuário.
     */
    public void updatePremiumStatus(@NonNull String userId, boolean isPremium, @NonNull ResultCallback<Void> callback) {
        firestore.collection(USUARIOS_COLLECTION).document(userId)
                .update("isPremium", isPremium)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Garante que um usuário exista no Firestore após o login ou criação.
     * Usa SetOptions.merge() para criar o documento apenas se ele não existir,
     * ou para atualizar dados básicos (nome, foto) sem apagar outros campos como 'isPremium'.
     */
    private void saveUserToFirestoreOnLogin(FirebaseUser user, @NonNull ResultCallback<FirebaseUser> finalCallback) {
        if (user == null) {
            finalCallback.onResult(new Result.Error<>(new Exception("Usuário do Firebase é nulo.")));
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("nome", user.getDisplayName());
        userData.put("email", user.getEmail());
        if (user.getPhotoUrl() != null) {
            userData.put("foto_perfil", user.getPhotoUrl().toString());
        }

        firestore.collection(USUARIOS_COLLECTION).document(user.getUid())
                .set(userData, SetOptions.merge()) // A chave para a operação segura
                .addOnSuccessListener(aVoid -> finalCallback.onResult(new Result.Success<>(user)))
                .addOnFailureListener(e -> finalCallback.onResult(new Result.Error<>(e)));
    }

    @Override
    public void getDiasAcessados(String userId, ResultCallback<Integer> callback) {
        Log.d(TAG, "getDiasAcessados: Buscando diasAcessoTotal para userId: " + userId);
        firestore.collection(USUARIOS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Lê o campo 'diasAcessoTotal' como Long
                        Long diasTotais = documentSnapshot.getLong("diasAcessoTotal");
                        int count = (diasTotais != null) ? diasTotais.intValue() : 0; // Converte para int, tratando nulo
                        Log.d(TAG, "getDiasAcessados SUCCESS: Valor lido: " + count);
                        callback.onResult(new Result.Success<>(count));
                    } else {
                        Log.w(TAG, "getDiasAcessados: Documento não encontrado para userId: " + userId);
                        callback.onResult(new Result.Success<>(0)); // Retorna 0 se o documento não existe
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getDiasAcessados ERROR: ", e);
                    callback.onResult(new Result.Error<>(e)); // Retorna erro em caso de falha na leitura
                });
    }
}