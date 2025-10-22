package com.example.startuppulse.ui.privacidade;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.common.Callback;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IIdeiaRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.data.repositories.MentorRepository;
import com.example.startuppulse.util.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo; // Importar UserInfo
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.lifecycle.MediatorLiveData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PrivacidadeSegurancaViewModel extends ViewModel {

    private static final String TAG = "PrivacidadeVM";
    private final IAuthRepository authRepository;
    private final IUserRepository userRepository; // Para visibilidade
    private final FirebaseAuth firebaseAuth; // Para reautenticação/exclusão

    private final MutableLiveData<Boolean> _isGoogleSignIn = new MutableLiveData<>(false);
    public final LiveData<Boolean> isGoogleSignIn = _isGoogleSignIn;

    private final MutableLiveData<Boolean> _isProfilePublic = new MutableLiveData<>(false);
    public final LiveData<Boolean> isProfilePublic = _isProfilePublic;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    // --- LiveData para os critérios de desativação ---
    private final MutableLiveData<Boolean> _isMentorCheckResult = new MutableLiveData<>();
    public final LiveData<Boolean> isMentorCheckResult = _isMentorCheckResult;

    private final MutableLiveData<Boolean> _hasRatedIdeasCheckResult = new MutableLiveData<>();
    public final LiveData<Boolean> hasRatedIdeasCheckResult = _hasRatedIdeasCheckResult;

    // MediatorLiveData para saber quando AMBOS os checks foram concluídos
    private final MediatorLiveData<Pair<Boolean, Boolean>> _deactivationCriteriaReady = new MediatorLiveData<>();
    public LiveData<Pair<Boolean, Boolean>> getDeactivationCriteriaReady() {
        return _deactivationCriteriaReady;
    }

    @Inject
    IIdeiaRepository ideiaRepository;
    @Inject
    MentorRepository mentorRepository;

    // Eventos para feedback na UI
    private final MutableLiveData<Event<String>> _toastMessage = new MutableLiveData<>();
    public final LiveData<Event<String>> toastMessage = _toastMessage;

    private final MutableLiveData<Event<Void>> _navigateToLogin = new MutableLiveData<>();
    public final LiveData<Event<Void>> navigateToLogin = _navigateToLogin;

    @Inject
    FirebaseFirestore firestore;
    @Inject
    FirebaseStorage storage;

    @Inject
    public PrivacidadeSegurancaViewModel(IAuthRepository authRepository, IUserRepository userRepository, FirebaseAuth firebaseAuth) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.firebaseAuth = firebaseAuth;
        checkSignInProvider();
        loadInitialProfileVisibility();
        setupCriteriaMediator();
    }

    private void setupCriteriaMediator() {
        _deactivationCriteriaReady.addSource(_isMentorCheckResult, isMentor -> {
            Boolean hasRatedIdeas = _hasRatedIdeasCheckResult.getValue();
            if (hasRatedIdeas != null) { // Só emite se o outro valor já chegou
                _deactivationCriteriaReady.setValue(new Pair<>(isMentor, hasRatedIdeas));
            }
        });
        _deactivationCriteriaReady.addSource(_hasRatedIdeasCheckResult, hasRatedIdeas -> {
            Boolean isMentor = _isMentorCheckResult.getValue();
            if (isMentor != null) { // Só emite se o outro valor já chegou
                _deactivationCriteriaReady.setValue(new Pair<>(isMentor, hasRatedIdeas));
            }
        });
    }

    private void checkSignInProvider() {
        FirebaseUser user = authRepository.getCurrentUser();
        boolean googleUser = false;
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) {
                    googleUser = true;
                    break;
                }
            }
        }
        _isGoogleSignIn.setValue(googleUser);
        Log.d(TAG, "Is Google Sign In: " + googleUser);
    }

    private void loadInitialProfileVisibility() {
        String userId = authRepository.getCurrentUserId();
        if (userId != null) {
            userRepository.getUserProfile(userId, result -> {
                if (result instanceof Result.Success) {
                    User user = ((Result.Success<User>) result).data;
                    // Assumindo que User.java tem o campo 'profilePublic' e getter/setter
                    _isProfilePublic.postValue(user != null && user.isProfilePublic());
                } else {
                    // Tratar erro, talvez assumir privado por padrão
                    _isProfilePublic.postValue(false);
                    Log.e(TAG, "Erro ao carregar visibilidade inicial do perfil", ((Result.Error<User>)result).error );
                }
            });
        }
    }

    public void updateProfileVisibility(boolean isPublic) {
        String userId = authRepository.getCurrentUserId();
        if (userId != null) {
            _isLoading.setValue(true);
            userRepository.updateProfileVisibility(userId, isPublic, result -> { // Precisa criar este método no IUserRepository/UserRepository
                _isLoading.postValue(false);
                if (result instanceof Result.Success) {
                    _isProfilePublic.postValue(isPublic); // Atualiza o LiveData local
                    _toastMessage.postValue(new Event<>("Visibilidade atualizada"));
                } else {
                    _isProfilePublic.postValue(!isPublic); // Reverte na UI em caso de erro
                    _toastMessage.postValue(new Event<>("Erro ao atualizar visibilidade"));
                    Log.e(TAG, "Erro ao atualizar visibilidade do perfil", ((Result.Error<Void>)result).error );
                }
            });
        }
    }


    public void sendPasswordResetEmail() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            _isLoading.setValue(true);
            firebaseAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        _isLoading.postValue(false);
                        if (task.isSuccessful()) {
                            _toastMessage.postValue(new Event<>("E-mail de redefinição enviado para " + user.getEmail()));
                            Log.d(TAG, "Password reset email sent.");
                        } else {
                            _toastMessage.postValue(new Event<>("Erro ao enviar e-mail."));
                            Log.e(TAG, "Error sending password reset email", task.getException());
                        }
                    });
        } else {
            _toastMessage.postValue(new Event<>("E-mail do usuário não encontrado."));
            Log.w(TAG, "Cannot send password reset email, user or email is null");
        }
    }

    public void deleteAccount() {
        requestAccountDeletionOrDeactivation();
    }

    public void prepareDeletionDialogData() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            _toastMessage.postValue(new Event<>("Usuário não encontrado."));
            return;
        }
        String userId = user.getUid();
        Log.d(TAG, "prepareDeletionDialogData: Verificando critérios para userId: " + userId);
        _isLoading.setValue(true); // Mostra loading enquanto verifica

        // Resetar LiveData para garantir que o mediator funcione corretamente em re-cliques
        _isMentorCheckResult.setValue(null);
        _hasRatedIdeasCheckResult.setValue(null);
        _deactivationCriteriaReady.setValue(null); // Limpa o valor do mediator


        // Tarefa 1: Verificar se é mentor
        Task<DocumentSnapshot> isMentorTask = firestore.collection("mentores").document(userId).get()
                .addOnCompleteListener(task -> {
                    boolean isMentor = task.isSuccessful() && task.getResult() != null && task.getResult().exists();
                    _isMentorCheckResult.postValue(isMentor); // Atualiza o LiveData do mentor
                    Log.d(TAG, "Check Mentor Concluído: " + isMentor);
                });


        // Tarefa 2: Verificar se tem ideias com avaliações
        Task<QuerySnapshot> ideasWithRatingsTask = firestore.collection("ideias")
                .whereEqualTo("ownerId", userId)
                .limit(100) // Pega algumas ideias para verificar
                .get()
                .addOnCompleteListener(task -> {
                    boolean hasRatedIdeas = false;
                    if(task.isSuccessful() && task.getResult() != null){
                        // Itera sobre as ideias encontradas para ver se alguma tem avaliação
                        for (DocumentSnapshot doc : task.getResult()){
                            // *** CORRECTION START ***
                            // Check if the 'avaliacoes' field exists and is a List
                            Object avaliacoesField = doc.get("avaliacoes");
                            if (avaliacoesField instanceof List) {
                                // Check if the list is not empty
                                if (!((List<?>) avaliacoesField).isEmpty()) {
                                    hasRatedIdeas = true;
                                    Log.d(TAG, "Found idea (" + doc.getId() + ") with non-empty 'avaliacoes' list.");
                                    break; // Found one, no need to check further
                                }
                            }
                            // *** CORRECTION END ***
                        }
                    } else {
                        Log.e(TAG,"Erro ao buscar ideias para checar avaliações", task.getException());
                    }
                    _hasRatedIdeasCheckResult.setValue(hasRatedIdeas);
                    Log.d(TAG, "Check Ideias Avaliadas Concluído: " + hasRatedIdeas);
                });


        // Esconde o loading quando AMBAS as tarefas terminarem (não importa o resultado)
        Tasks.whenAllComplete(isMentorTask, ideasWithRatingsTask).addOnCompleteListener(task -> {
            _isLoading.postValue(false);
            Log.d(TAG, "Verificação de critérios finalizada.");
        });
    }
    public void requestAccountDeletionOrDeactivation() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            _toastMessage.postValue(new Event<>("Usuário não encontrado."));
            return;
        }
        String userId = user.getUid();
        _isLoading.setValue(true);

        // 1. Verifica os critérios para desativação
        checkDeactivationCriteria(userId, shouldDeactivate -> {
            if (shouldDeactivate) {
                // 2a. Se deve desativar, chama a função de desativação
                deactivateAccount(userId);
            } else {
                // 2b. Se não precisa desativar, prossegue com a exclusão completa
                deleteUserDataFirestore(userId, () -> {
                    // Callback chamado APÓS limpar dados do Firestore/Storage
                    deleteFirebaseAuthUser(user); // Função separada para deletar Auth
                });
            }
        });
    }

    /**
     * Verifica se o usuário é mentor ou tem ideias avaliadas.
     * @param userId ID do usuário.
     * @param callback Retorna true se a conta deve ser desativada, false se pode ser excluída.
     */
    private void checkDeactivationCriteria(String userId, Callback<Boolean> callback) {
        Log.d(TAG, "Verificando critérios de desativação para userId: " + userId);

        // Tarefa 1: Verificar se é mentor
        Task<DocumentSnapshot> isMentorTask = firestore.collection("mentores").document(userId).get();

        // Tarefa 2: Verificar se tem ideias com avaliações
        Task<QuerySnapshot> ideasWithRatingsTask = firestore.collection("ideias")
                .whereEqualTo("ownerId", userId)
                // Assumindo que 'avaliacoes' é um array/lista e não vazio significa avaliado
                .whereGreaterThan("avaliacoes.size", 0) // Query Firestore para array não vazio (ajuste se a estrutura for diferente)
                // Alternativa (se 'avaliacoes' for um campo numérico de contagem): .whereGreaterThan("contagemAvaliacoes", 0)
                .limit(1) // Só precisamos saber se *existe* pelo menos uma
                .get();

        // Combina as verificações
        Tasks.whenAllComplete(isMentorTask, ideasWithRatingsTask).addOnCompleteListener(task -> {
            boolean isMentor = isMentorTask.isSuccessful() && isMentorTask.getResult() != null && isMentorTask.getResult().exists();
            boolean hasRatedIdeas = ideasWithRatingsTask.isSuccessful() && ideasWithRatingsTask.getResult() != null && !ideasWithRatingsTask.getResult().isEmpty();

            Log.d(TAG, "Critérios: É Mentor = " + isMentor + ", Tem Ideias Avaliadas = " + hasRatedIdeas);

            // Se for mentor OU tiver ideias avaliadas, desativa. Caso contrário, exclui.
            callback.onComplete(isMentor || hasRatedIdeas);
        });
    }

    /**
     * Marca a conta do usuário e o perfil de mentor (se existir) como desativados.
     * Não exclui dados nem a conta de autenticação.
     * @param userId ID do usuário a ser desativado.
     */
    private void deactivateAccount(String userId) {
        Log.d(TAG, "Iniciando desativação da conta para userId: " + userId);
        _isLoading.setValue(true);

        WriteBatch batch = firestore.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "desativado");
        updates.put("desativadoEm", FieldValue.serverTimestamp());

        // Marca o documento do usuário como desativado
        DocumentReference userRef = firestore.collection("usuarios").document(userId);
        batch.set(userRef, updates, SetOptions.merge()); // Use merge para não apagar outros campos

        // Marca o documento do mentor como desativado (se existir)
        DocumentReference mentorRef = firestore.collection("mentores").document(userId);
        batch.set(mentorRef, updates, SetOptions.merge()); // Merge também funciona se o doc não existir (não faz nada)

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Conta desativada com sucesso para userId: " + userId);
                    _isLoading.postValue(false);
                    _toastMessage.postValue(new Event<>("Conta desativada. Você será desconectado."));
                    // Força o logout e navega para login
                    authRepository.logout();
                    _navigateToLogin.postValue(new Event<>(null));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao desativar a conta para userId: " + userId, e);
                    _isLoading.postValue(false);
                    _toastMessage.postValue(new Event<>("Erro ao desativar a conta. Tente novamente."));
                });
    }

    // Função separada para deletar o usuário do Firebase Auth
    private void deleteFirebaseAuthUser(FirebaseUser user) {
        if (user == null) return;
        user.delete()
                .addOnCompleteListener(task -> {
                    _isLoading.postValue(false); // Para o loading final aqui
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Conta do Firebase Auth deletada.");
                        _toastMessage.postValue(new Event<>("Conta excluída com sucesso."));
                        // Força o logout e navega para login (garante estado limpo)
                        authRepository.logout();
                        _navigateToLogin.postValue(new Event<>(null));
                    } else {
                        Log.e(TAG, "Erro ao deletar conta do Firebase Auth", task.getException());
                        _toastMessage.postValue(new Event<>("Erro ao excluir conta. Tente fazer login novamente."));
                    }
                });
    }


    // Função placeholder para deletar dados do Firestore associados ao usuário
    private void deleteUserDataFirestore(String userId, Runnable onComplete) {
        Log.d(TAG, "Iniciando exclusão COMPLETA de dados (Firestore & Storage) para userId: " + userId);
        _isLoading.postValue(true);

        // --- Tarefas de Exclusão ---
        List<Task<?>> allDeleteTasks = new ArrayList<>(); // Use Task<?> para Tasks.whenAll

        // 1. Deletar documento principal do usuário
        Task<Void> deleteUserDocTask = firestore.collection("usuarios").document(userId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Documento 'usuarios/" + userId + "' deletado."))
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao deletar documento 'usuarios/" + userId + "'", e));
        allDeleteTasks.add(deleteUserDocTask);

        // 2. Deletar documento premium (se existir)
        Task<Void> deletePremiumDocTask = firestore.collection("premium").document(userId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Documento 'premium/" + userId + "' deletado (se existia)."))
                .addOnFailureListener(e -> Log.w(TAG, "Erro ao deletar documento 'premium/" + userId + "' (pode não existir)", e));
        allDeleteTasks.add(deletePremiumDocTask);

        // 3. Deletar documento de mentor (se existir)
        Task<Void> deleteMentorDocTask = firestore.collection("mentores").document(userId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Documento 'mentores/" + userId + "' deletado (se existia)."))
                .addOnFailureListener(e -> Log.w(TAG, "Erro ao deletar documento 'mentores/" + userId + "' (pode não existir)", e));
        allDeleteTasks.add(deleteMentorDocTask);

        // 4. Deletar as ideias do usuário (em batch)
        Task<Void> deleteIdeiasTask = firestore.collection("ideias")
                .whereEqualTo("ownerId", userId)
                .get()
                .continueWithTask(task -> { // Use continueWithTask para encadear corretamente
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.e(TAG, "Erro ao buscar ideias para exclusão", task.getException());
                        // Lança a exceção para falhar o Tasks.whenAll
                        throw task.getException() != null ? task.getException() : new Exception("Falha ao buscar ideias");
                    }

                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "Nenhuma ideia encontrada para deletar para userId: " + userId);
                        return Tasks.forResult(null); // Nenhuma ideia, tarefa concluída
                    }

                    Log.d(TAG, "Encontradas " + querySnapshot.size() + " ideias para deletar.");
                    WriteBatch batch = firestore.batch();
                    querySnapshot.forEach(doc -> batch.delete(doc.getReference()));
                    return batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, querySnapshot.size() + " ideias deletadas em batch."))
                            .addOnFailureListener(e -> Log.e(TAG, "Erro ao executar batch de exclusão de ideias", e));
                });
        allDeleteTasks.add(deleteIdeiasTask);

        // 5. Deletar foto de perfil do Storage
        StorageReference profilePicRef = storage.getReference()
                .child("profile_images")
                .child(userId)
                .child("profile.jpg"); // Caminho exato da foto
        Task<Void> deleteStorageTask = profilePicRef.delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Foto de perfil deletada do Storage (se existia)."))
                .addOnFailureListener(e -> Log.w(TAG, "Erro ao deletar foto de perfil (pode não existir)", e)); // Warning, pois pode não existir
        allDeleteTasks.add(deleteStorageTask);

        // 6. (Opcional) Adicione Tasks para deletar outros dados (avaliações feitas, etc.)

        // 7. Aguarda TODAS as tarefas (sucesso ou falha não crítica)
        // Usamos whenAll para garantir que todas tentaram executar, mesmo que algumas falhem (como deletar arquivos/docs que não existem)
        Tasks.whenAll(allDeleteTasks)
                .addOnCompleteListener(task -> {
                    // Este listener é chamado quando todas as tarefas terminam, independentemente do sucesso individual
                    if (task.isSuccessful()) {
                        // Isso só acontece se NENHUMA task lançou exceção explicitamente
                        Log.i(TAG, "Limpeza de dados do Firestore/Storage concluída (ou não necessária) para userId " + userId);
                        onComplete.run(); // Prossegue para deletar Auth
                    } else {
                        // Alguma tarefa crítica (como buscar ideias ou deletar user doc) pode ter falhado
                        Log.e(TAG, "Falha em uma ou mais tarefas de limpeza de dados para userId: " + userId, task.getException());
                        _isLoading.postValue(false);
                        _toastMessage.postValue(new Event<>("Erro crítico ao limpar dados. Exclusão cancelada."));
                        // NÃO chama onComplete
                    }
                    // Note: Erros em deletePremiumDocTask, deleteMentorDocTask, deleteStorageTask são tratados como Warnings e não impedem onComplete
                    // se deleteUserDocTask e deleteIdeiasTask funcionarem. Ajuste se precisar de mais rigor.
                });
    }

}