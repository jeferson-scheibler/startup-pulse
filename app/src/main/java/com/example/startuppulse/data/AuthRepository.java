package com.example.startuppulse.data;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore; // Usar diretamente ou via helper
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AuthRepository {

    private static volatile AuthRepository instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore; // Abstração do FirestoreHelper

    private AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository();
                }
            }
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public interface UserProfileCallback {
        void onResult(User user);
    }

    public void getUserProfile(String uid, UserProfileCallback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onResult(null); // Retorna nulo se não houver usuário
            return;
        }

        User userProfile = new User();

        // 1. Pega os dados básicos do Firebase Auth
        userProfile.setNome(firebaseUser.getDisplayName());
        userProfile.setEmail(firebaseUser.getEmail());
        if (firebaseUser.getPhotoUrl() != null) {
            userProfile.setFotoUrl(firebaseUser.getPhotoUrl().toString());
        }

        // 2. Calcula os dias de conta
        long created = firebaseUser.getMetadata().getCreationTimestamp();
        long dias = Math.max(1, TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - created));
        userProfile.setDiasDeConta(dias);

        // 3. Busca os dados do Firestore em paralelo (mais eficiente)
        // (Para simplificar, vamos fazer em sequência aqui)

        // Busca o status do plano premium
        firestore.collection("premium").document(uid).get().addOnCompleteListener(premiumTask -> {
            if (premiumTask.isSuccessful() && premiumTask.getResult().exists()) {
                Timestamp dataFim = premiumTask.getResult().getTimestamp("data_fim");
                if (dataFim != null && dataFim.toDate().after(new Date())) {
                    userProfile.setPremium(true);
                    String dataFormatada = android.text.format.DateFormat
                            .format("dd/MM/yyyy", dataFim.toDate()).toString();
                    userProfile.setValidadePlano(dataFormatada);
                }
            }

            // Busca a contagem de ideias publicadas
            firestore.collection("ideias").whereEqualTo("ownerId", uid).get().addOnCompleteListener(ideiasTask -> {
                if (ideiasTask.isSuccessful()) {
                    userProfile.setPublicadasCount(ideiasTask.getResult().size());
                }

                // Busca a contagem de mentores seguidos
                firestore.collection("users").document(uid).collection("following_mentors").get().addOnCompleteListener(followingTask -> {
                    if (followingTask.isSuccessful()) {
                        userProfile.setSeguindoCount(followingTask.getResult().size());
                    }

                    // 4. Retorna o objeto User completo através do callback
                    callback.onResult(userProfile);
                });
            });
        });
    }

    public interface IdeiasCallback {
        void onResult(List<Ideia> ideias);
    }

    /**
     * Busca as ideias que são consideradas públicas (em avaliação ou já avaliadas).
     * As ideias são ordenadas pela data de criação, da mais nova para a mais antiga.
     */
    public void getPublicIdeias(IdeiasCallback callback) {
        firestore.collection("ideias")
                // Filtra para incluir apenas os status que são considerados públicos
                .whereIn("status", Arrays.asList("EM_AVALIACAO", "AVALIADA_APROVADA", "AVALIADA_REPROVADA"))
                // Ordena para que as ideias mais recentes apareçam primeiro
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ideia> ideias = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Ideia ideia = document.toObject(Ideia.class);
                        ideia.setId(document.getId()); // Atribui o ID do documento ao objeto
                        ideias.add(ideia);
                    }
                    callback.onResult(ideias);
                })
                .addOnFailureListener(e -> {
                    // Em caso de falha, retorna uma lista vazia
                    Log.e("AuthRepository", "Erro ao buscar ideias públicas", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    /**
     * Busca os rascunhos de ideias pertencentes apenas ao usuário logado.
     * Os rascunhos são ordenados pela data de criação, do mais novo para o mais antigo.
     */
    public void getDraftIdeias(IdeiasCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onResult(new ArrayList<>()); // Retorna lista vazia se não houver usuário logado
            return;
        }

        firestore.collection("ideias")
                // Filtra para pegar apenas as ideias do usuário atual
                .whereEqualTo("ownerId", currentUser.getUid())
                // Filtra para pegar apenas as ideias com status de Rascunho
                .whereEqualTo("status", "RASCUNHO")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ideia> rascunhos = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Ideia ideia = document.toObject(Ideia.class);
                        ideia.setId(document.getId()); // Atribui o ID do documento
                        rascunhos.add(ideia);
                    }
                    callback.onResult(rascunhos);
                })
                .addOnFailureListener(e -> {
                    Log.e("AuthRepository", "Erro ao buscar rascunhos", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    public interface IdeiasListenerCallback {
        void onResult(List<Ideia> ideias);
    }

    // 2. Defina uma interface de callback para operações simples (como deletar).
    public interface CompletionCallback {
        void onComplete(boolean success);
    }


    /**
     * Cria um listener em tempo real para as ideias que são consideradas públicas.
     * As ideias são ordenadas pela data de criação, da mais nova para a mais antiga.
     * @param callback O callback que será chamado toda vez que a lista for atualizada.
     * @return Um ListenerRegistration que pode ser usado para remover o listener.
     */
    public ListenerRegistration listenToPublicIdeias(IdeiasListenerCallback callback) {
        return firestore.collection("ideias")
                .whereIn("status", Arrays.asList("EM_AVALIACAO", "AVALIADA_APROVADA", "AVALIADA_REPROVADA"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("AuthRepository", "Listen to public ideas failed.", e);
                        callback.onResult(new ArrayList<>());
                        return;
                    }

                    List<Ideia> ideias = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            ideia.setId(doc.getId()); // Atribui o ID do documento ao objeto
                            ideias.add(ideia);
                        }
                    }
                    callback.onResult(ideias);
                });
    }

    /**
     * Cria um listener em tempo real para os rascunhos de ideias do usuário logado.
     * Os rascunhos são ordenados pela data de criação, do mais novo para o mais antigo.
     * @param callback O callback que será chamado toda vez que a lista de rascunhos for atualizada.
     * @return Um ListenerRegistration que pode ser usado para remover o listener.
     */
    public ListenerRegistration listenToDraftIdeias(IdeiasListenerCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // Se não há usuário, não há rascunhos. Retornamos null.
            callback.onResult(new ArrayList<>());
            return null;
        }

        return firestore.collection("ideias")
                .whereEqualTo("ownerId", currentUser.getUid())
                .whereEqualTo("status", "RASCUNHO")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("AuthRepository", "Listen to draft ideas failed.", e);
                        callback.onResult(new ArrayList<>());
                        return;
                    }

                    List<Ideia> rascunhos = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            ideia.setId(doc.getId());
                            rascunhos.add(ideia);
                        }
                    }
                    callback.onResult(rascunhos);
                });
    }

    /**
     * Exclui uma ideia do Firestore pelo seu ID.
     * @param ideiaId O ID do documento da ideia a ser excluída.
     * @param callback O callback que será chamado indicando sucesso ou falha.
     */
    public void deleteIdeia(String ideiaId, CompletionCallback callback) {
        firestore.collection("ideias").document(ideiaId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e("AuthRepository", "Erro ao excluir ideia", e);
                    callback.onComplete(false);
                });
    }


    // --- Login com Email e Senha ---
    public void loginWithEmail(String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Após o login, garantimos que os dados do usuário estão no Firestore
                    saveUserToFirestore(authResult.getUser(), callback);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- Login com Google ---
    public void loginWithGoogle(GoogleSignInAccount googleAccount, ResultCallback<FirebaseUser> callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // Após o login, salvamos ou atualizamos os dados do usuário no Firestore
                    saveUserToFirestore(authResult.getUser(), callback);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- Lógica de salvar usuário (antes no FirestoreHelper) ---
    private void saveUserToFirestore(FirebaseUser user, ResultCallback<FirebaseUser> finalCallback) {
        if (user == null) {
            finalCallback.onError(new Exception("Usuário do Firebase é nulo."));
            return;
        }

        // Caminho do documento do usuário
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().exists()) {
                        // O usuário não existe no Firestore, vamos criá-lo
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        if (user.getPhotoUrl() != null) {
                            userData.put("photoUrl", user.getPhotoUrl().toString());
                        }

                        firestore.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> finalCallback.onSuccess(user))
                                .addOnFailureListener(finalCallback::onError);
                    } else if (task.isSuccessful()) {
                        // Usuário já existe, login bem-sucedido
                        finalCallback.onSuccess(user);
                    } else {
                        // Erro ao verificar a existência do usuário
                        finalCallback.onError(task.getException());
                    }
                });
    }

    public void createUser(String name, String email, String password, ResultCallback<FirebaseUser> callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Após criar, atualizamos o perfil com o nome fornecido
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Com o perfil atualizado, salvamos no Firestore
                                        saveUserToFirestore(user, callback);
                                    } else {
                                        // Se a atualização do perfil falhar, ainda consideramos um erro
                                        callback.onError(task.getException());
                                    }
                                });
                    } else {
                        callback.onError(new Exception("Falha ao obter o usuário após a criação."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void logout() {
        firebaseAuth.signOut();
    }
}