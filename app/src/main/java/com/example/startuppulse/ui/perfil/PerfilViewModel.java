// Em: app/src/main/java/com/example/startuppulse/ui/perfil/PerfilViewModel.java

package com.example.startuppulse.ui.perfil;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.repositories.IIdeiaRepository;
import com.example.startuppulse.data.repositories.IInvestorRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.data.repositories.MentorRepository;
import com.example.startuppulse.data.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Imports do Hilt
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

// MODIFICAÇÃO 1: Adicionar a anotação @HiltViewModel
@HiltViewModel
public class PerfilViewModel extends ViewModel {

    private final IIdeiaRepository ideiaRepository;
    private final IInvestorRepository investorRepository;
    private final IUserRepository userRepository;

    private final AuthRepository authRepository;
    private final MentorRepository mentorRepository;
    private final FirebaseFirestore firestore;

    private final MutableLiveData<Result<User>> _userProfileResult = new MutableLiveData<>();
    public LiveData<Result<User>> userProfileResult = _userProfileResult;

    private final MutableLiveData<Boolean> _isMentor = new MutableLiveData<>(false);
    public LiveData<Boolean> isMentor = _isMentor;
    private final MutableLiveData<Boolean> _navigateToLogin = new MutableLiveData<>(false);
    public final LiveData<Boolean> navigateToLogin = _navigateToLogin;

    private final MutableLiveData<Boolean> _isPro = new MutableLiveData<>(false);
    public final LiveData<Boolean> isPro = _isPro;
    private final MutableLiveData<Integer> ideiasPublicas = new MutableLiveData<>(-1);
    private final MutableLiveData<Integer> diasAcessados = new MutableLiveData<>(-1);
    private final MutableLiveData<Integer> nivelEngajamento = new MutableLiveData<>(0);
    private Integer tempIdeiasCount = null;
    private Integer tempDiasCount = null;
    private final MutableLiveData<Integer> avaliacoesRecebidas = new MutableLiveData<>(-1);
    private Integer tempAvaliacoesCount = null;
    private User currentUserProfile = null; // Para guardar os dados do perfil
    private Timestamp tempUltimoAcesso = null; // Para guardar o último acesso
    private Boolean tempIsMentor = null;
    private final Observer<Result<User>> userProfileObserver;
    private final Observer<Boolean> isMentorObserver;
    private final MutableLiveData<String> _validadePlanoDisplay = new MutableLiveData<>("");
    public final LiveData<String> validadePlanoDisplay = _validadePlanoDisplay;
    private final MutableLiveData<Boolean> _isInvestorActive = new MutableLiveData<>(false);
    public LiveData<Boolean> isInvestorActive = _isInvestorActive;

    // Formatador de data (pode ser membro da classe para reutilização)
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Observer interno para perfil (exemplo de implementação completa)
    private static final String TAG = "PerfilViewModel";
    @Inject
    public PerfilViewModel(IIdeiaRepository ideiaRepository, AuthRepository authRepository, MentorRepository mentorRepository,IUserRepository userRepository,
                           IInvestorRepository investorRepository, FirebaseFirestore firestore) {
        this.ideiaRepository = ideiaRepository;
        this.authRepository = authRepository;
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
        this.investorRepository = investorRepository;
        this.firestore = firestore;
        userProfileObserver = result -> {
            boolean profileChanged = false;
            if (result instanceof Result.Success) {
                User newUser = ((Result.Success<User>) result).data;
                Log.d(TAG, "userProfileObserver triggered: Success");

                // --- LÓGICA DE ATUALIZAÇÃO DA VALIDADE ---
                String validadeStr = "";
                // Verifica se o usuário existe, é premium e tem data de expiração
                if (newUser != null && newUser.isPremium() && newUser.getDataExpiracaoPlano() != null) {
                    // Verifica se a data de expiração é no futuro
                    if (newUser.getDataExpiracaoPlano().after(new Date())) {
                        validadeStr = "Válido até " + sdf.format(newUser.getDataExpiracaoPlano());
                        Log.d(TAG, "Plano PRO válido até: " + validadeStr);
                    } else {
                        Log.d(TAG, "Plano PRO expirado em: " + sdf.format(newUser.getDataExpiracaoPlano()));
                    }
                } else {
                    Log.d(TAG, "Usuário não é premium ou não tem data de expiração.");
                }
                // Posta a string (vazia se não for premium/válido)
                // Verifica se o valor mudou antes de postar para evitar loops desnecessários
                if (!Objects.equals(_validadePlanoDisplay.getValue(), validadeStr)) {
                    _validadePlanoDisplay.postValue(validadeStr);
                }
                // ----------------------------------------


                // Atualiza currentUserProfile e tempUltimoAcesso (como antes)
                if (newUser != null && !newUser.equals(currentUserProfile)) {
                    currentUserProfile = newUser;
                    tempUltimoAcesso = currentUserProfile.getUltimoAcesso();
                    profileChanged = true;
                    Log.d(TAG, "Internal Observer: User profile object updated.");
                } else if (newUser == null && currentUserProfile != null) {
                    resetProfileData();
                    profileChanged = true;
                    Log.w(TAG, "Internal Observer: User profile data became null.");
                }
            } else if (result instanceof Result.Error || result == null) {
                if (currentUserProfile != null) {
                    resetProfileData();
                    profileChanged = true;
                    Log.e(TAG, "Internal Observer: Error loading user profile or result is null.");
                    _validadePlanoDisplay.postValue(""); // Limpa validade em caso de erro
                }
            }
            if (profileChanged) {
                tryUpdateNivelEngajamento();
            }
        };

        isMentorObserver = isMentorValue -> {
            tempIsMentor = isMentorValue;
            Log.d(TAG, "Internal Observer: Mentor status updated: " + tempIsMentor);
            // Check if stats data is also ready before calculating
            tryUpdateNivelEngajamento();
        };
        checkSubscriptionStatus();
        setupInternalObservers();
        checkIfUserIsMentor();
    }

    private void resetProfileData() {
        currentUserProfile = null;
        tempUltimoAcesso = null;
        // Do not reset tempIsMentor here, it's handled by its own observer
    }

    private void resetStatsData() {
        tempIdeiasCount = null;
        tempDiasCount = null;
        tempAvaliacoesCount = null;
    }

    private void setupInternalObservers() {
        userProfileResult.observeForever(userProfileObserver);
        isMentor.observeForever(isMentorObserver);
    }

    public void loadUserProfile() {
        String currentUserId = authRepository.getCurrentUserId();
        investorRepository.getInvestorDetails(currentUserId, result -> {
            if (result instanceof Result.Success) {
                Investor investor = ((Result.Success<Investor>) result).data;
                // Se o perfil de investidor existe E está "ACTIVE"
                if (investor != null && "ACTIVE".equals(investor.getStatus())) {
                    _isInvestorActive.postValue(true);
                } else {
                    _isInvestorActive.postValue(false);
                }
            } else {
                // Se não encontrou o documento, ele não é um investidor
                _isInvestorActive.postValue(false);
            }
        });
        if (currentUserId != null) {
            _userProfileResult.setValue(new Result.Loading<>());
            authRepository.getUserProfile(currentUserId, result -> {
                _userProfileResult.setValue(result);
            });
        } else {
            _userProfileResult.setValue(new Result.Error<>(new Exception("Usuário não autenticado.")));
        }
    }
    private void checkIfUserIsMentor() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            // Usamos o getMentorById, que é otimizado para buscar um único documento
            mentorRepository.getMentorById(currentUser.getUid(), result -> {
                // Se o resultado for Success, significa que o documento existe e o usuário é um mentor.
                // Se for Error (com a mensagem "Mentor não encontrado"), ele não é.
                _isMentor.postValue(result instanceof Result.Success);
            });
        }
    }
    public void checkSubscriptionStatus() {
        String userId = authRepository.getCurrentUserId();
        if (userId == null) {
            _isPro.postValue(false);
            return;
        }

        firestore.collection("premium").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Timestamp dataFim = document.getTimestamp("data_fim");
                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            _isPro.postValue(true); // Usuário é Pro
                        } else {
                            _isPro.postValue(false); // Assinatura expirou
                        }
                    } else {
                        _isPro.postValue(false); // Não tem assinatura
                    }
                })
                .addOnFailureListener(e -> {
                    _isPro.postValue(false); // Assume que não é Pro em caso de erro
                    // Opcional: Logar o erro
                });
    }

    public void carregarDadosEstatisticas() {
        String userId = authRepository.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "carregarDadosEstatisticas: User ID is null.");
            // Set stats temps to 0 immediately if no user ID
            tempIdeiasCount = 0;
            tempDiasCount = 0;
            tempAvaliacoesCount = 0;
            ideiasPublicas.postValue(0);
            diasAcessados.postValue(0);
            avaliacoesRecebidas.postValue(0);
            return;
        }

        Log.d(TAG, "carregarDadosEstatisticas: Iniciando busca para userId: " + userId);
        resetStatsData(); // Reset temps before fetching new stats
        final AtomicInteger completionCounter = new AtomicInteger(0);
        final int TOTAL_TASKS = 3;

        Runnable checkCompletion = () -> {
            // Only trigger calculation when the LAST stat arrives
            if (completionCounter.incrementAndGet() == TOTAL_TASKS) {
                Log.d(TAG, "All statistics fetches completed.");
                tryUpdateNivelEngajamento(); // <<< TRIGGER CALCULATION HERE
            }
        };

        // Fetch Ideas
        ideiaRepository.getPublicIdeasCountByUser(userId, result -> {
            if (result instanceof Result.Success) {
                Integer count = ((Result.Success<Integer>) result).data;
                Log.d(TAG, "getPublicIdeasCountByUser SUCCESS: " + count);
                tempIdeiasCount = (count != null ? count : 0);
                ideiasPublicas.postValue(tempIdeiasCount);
            } else {
                Log.e(TAG, "getPublicIdeasCountByUser ERROR: ", ((Result.Error<Integer>) result).error);
                tempIdeiasCount = 0;
                ideiasPublicas.postValue(0);
            }
            checkCompletion.run(); // Increment counter and potentially trigger
        });

        // Fetch Days
        authRepository.getDiasAcessados(userId, result -> { // Ou UserRepository
            if (result instanceof Result.Success) {
                Integer count = ((Result.Success<Integer>) result).data;
                Log.d(TAG, "getDiasAcessados SUCCESS: " + count);
                tempDiasCount = (count != null ? count : 0);
                diasAcessados.postValue(tempDiasCount);
            } else {
                Log.e(TAG, "getDiasAcessados ERROR: ", ((Result.Error<Integer>) result).error);
                tempDiasCount = 0;
                diasAcessados.postValue(0);
            }
            checkCompletion.run(); // Increment counter and potentially trigger
        });

        // Fetch Ratings
        ideiaRepository.getAvaliacoesRecebidasCount(userId, result -> {
            if (result instanceof Result.Success) {
                Integer count = ((Result.Success<Integer>) result).data;
                Log.d(TAG, "getAvaliacoesRecebidasCount SUCCESS: " + count);
                tempAvaliacoesCount = (count != null ? count : 0);
                avaliacoesRecebidas.postValue(tempAvaliacoesCount);
            } else {
                Log.e(TAG, "getAvaliacoesRecebidasCount ERROR: ", ((Result.Error<Integer>) result).error);
                tempAvaliacoesCount = 0;
                avaliacoesRecebidas.postValue(0);
            }
            checkCompletion.run(); // Increment counter and potentially trigger
        });
    }


    private void tryUpdateNivelEngajamento() {
        // Verifica se TODOS os dados necessários chegaram
        if (tempIdeiasCount == null || tempDiasCount == null || tempAvaliacoesCount == null ||
                currentUserProfile == null || tempIsMentor == null) {

            Log.d(TAG, "tryUpdateNivelEngajamento: Calculation check - Not all data ready.");
            Log.d(TAG, String.format("Status: Ideias=%s, Dias=%s, Aval=%s, Profile=%s, Acesso=%s, Mentor=%s",
                    tempIdeiasCount, tempDiasCount, tempAvaliacoesCount,
                    (currentUserProfile != null),
                    (currentUserProfile != null && currentUserProfile.getUltimoAcesso() != null), // Check via profile
                    tempIsMentor));
            return;
        }

        Log.d(TAG, "tryUpdateNivelEngajamento: Calculando com:");
        Log.d(TAG, " Ideias=" + tempIdeiasCount);
        Log.d(TAG, " Dias=" + tempDiasCount);
        Log.d(TAG, " Avaliações Recebidas=" + tempAvaliacoesCount);
        Log.d(TAG, " Mentor=" + tempIsMentor);
        Log.d(TAG, " Ultimo Acesso=" + (tempUltimoAcesso != null ? tempUltimoAcesso.toDate() : "Nulo"));


        // Aplica a nova fórmula
        int pontos_ideias = Math.min(tempIdeiasCount * 5, 25);
        int pontos_dias = Math.min(tempDiasCount * 1, 30);
        int pontos_avaliacoes = Math.min(tempAvaliacoesCount * 3, 15);

        boolean temBio = currentUserProfile.getBio() != null && !currentUserProfile.getBio().isEmpty();
        boolean temProfissao = currentUserProfile.getProfissao() != null && !currentUserProfile.getProfissao().isEmpty();
        boolean temLinkedin = currentUserProfile.getLinkedinUrl() != null && !currentUserProfile.getLinkedinUrl().isEmpty();
        int bonus_perfil = (temBio ? 5 : 0) + (temProfissao ? 5 : 0) + (temLinkedin ? 5 : 0);

        boolean acessouUltimos7Dias = false;
        if (tempUltimoAcesso != null) {
            long diff = new Date().getTime() - tempUltimoAcesso.toDate().getTime();
            long diffDays = diff / (24 * 60 * 60 * 1000);
            acessouUltimos7Dias = diffDays < 7;
            Log.d(TAG, " Dias desde último acesso=" + diffDays);
        }
        int bonus_recencia = acessouUltimos7Dias ? 10 : 0;

        int bonus_mentor = tempIsMentor ? 15 : 0;

        int score_base = pontos_ideias + pontos_dias + pontos_avaliacoes;
        int score_bonus = bonus_perfil + bonus_recencia + bonus_mentor;
        int score_final = Math.min(score_base + score_bonus, 100);

        Log.d(TAG, String.format(" Score Detalhado: Base(%d) + Bonus(%d) = %d -> Final: %d",
                score_base, score_bonus, score_base + score_bonus, score_final));

        nivelEngajamento.postValue(score_final);

        // Resetar temps se for calcular novamente em outra ocasião
        tempIdeiasCount = null;
        tempDiasCount = null;
        tempAvaliacoesCount = null;

        resetStatsData();
    }

    public LiveData<Integer> getIdeiasPublicas() { return ideiasPublicas; }
    public LiveData<Integer> getDiasAcessados() { return diasAcessados; }
    public LiveData<Integer> getNivelEngajamento() { return nivelEngajamento; }
    public LiveData<Integer> getAvaliacoesRecebidas() { return avaliacoesRecebidas; }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove observers
        if (userProfileObserver != null) {
            userProfileResult.removeObserver(userProfileObserver);
        }
        if (isMentorObserver != null) {
            isMentor.removeObserver(isMentorObserver);
        }
        Log.d(TAG, "onCleared: ViewModel destroyed, observers removed.");
    }

    public void logout() {
        String currentUserId = authRepository.getCurrentUserId();

        if (currentUserId == null || currentUserId.isEmpty()) {
            // Se não há usuário, apenas faz o logout local e navega
            authRepository.logout();
            _navigateToLogin.setValue(true);
            return;
        }

        // Etapa 1: Remover o token FCM (passando null)
        userRepository.updateFcmToken(currentUserId, null, result -> {
            if (result instanceof Result.Success) {
                Log.i(TAG, "FCM Token removed successfully for user: " + currentUserId);
            } else {
                // Loga o erro mas continua o processo de logout mesmo assim.
                // É melhor deslogar o usuário do que mantê-lo preso por uma falha
                // na limpeza do token.
                Log.w(TAG, "Failed to remove FCM Token for user: " + currentUserId,
                        result instanceof Result.Error ? ((Result.Error<Void>) result).error : null);
            }

            // Etapa 2: Fazer o logout no Auth e navegar
            authRepository.logout();
            _navigateToLogin.setValue(true);
        });
    }
}