package com.example.startuppulse.ui.investor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.repositories.IInvestorRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.util.Event;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InvestorVerificationViewModel extends ViewModel {

    public enum VerificationState { IDLE, LOADING, VERIFYING, SUCCESS, ERROR }

    private final IInvestorRepository investorRepository;
    private final IUserRepository userRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<VerificationState> _state = new MutableLiveData<>(VerificationState.IDLE);
    public final LiveData<VerificationState> state = _state;

    private final MutableLiveData<User> _user = new MutableLiveData<>();
    public final LiveData<User> user = _user;

    private final MutableLiveData<Event<String>> _errorEvent = new MutableLiveData<>();
    public final LiveData<Event<String>> errorEvent = _errorEvent;

    private String currentUserId;

    @Inject
    public InvestorVerificationViewModel(
            IInvestorRepository investorRepository,
            IUserRepository userRepository,
            AuthRepository authRepository) {
        this.investorRepository = investorRepository;
        this.userRepository = userRepository;
        this.authRepository = authRepository;

        loadCurrentUser();
    }

    /**
     * Carrega os dados do usuário logado (Nome, Email) do repositório.
     */
    public void loadCurrentUser() {
        _state.setValue(VerificationState.LOADING);
        currentUserId = authRepository.getCurrentUserId();
        if (currentUserId == null) {
            _errorEvent.setValue(new Event<>("Você não está logado."));
            _state.setValue(VerificationState.ERROR);
            return;
        }

        userRepository.getUserProfile(currentUserId, result -> {
            if (result instanceof Result.Success) {
                _user.setValue(((Result.Success<User>) result).data);
                _state.setValue(VerificationState.IDLE);
            } else {
                _errorEvent.setValue(new Event<>("Falha ao carregar dados do usuário."));
                _state.setValue(VerificationState.ERROR);
            }
        });
    }

    /**
     * Etapa 1: Cria o documento "PENDING" e começa a ouvir.
     */
    public void startVerification(String investorType, String documento) {
        User currentUser = _user.getValue();
        if (currentUser == null) {
            _errorEvent.setValue(new Event<>("Dados do usuário não carregados."));
            return;
        }

        _state.setValue(VerificationState.LOADING);

        Investor novoInvestor = new Investor();
        novoInvestor.setId(currentUserId);
        novoInvestor.setEmailContato(currentUser.getEmail());
        novoInvestor.setNome(currentUser.getNome());
        novoInvestor.setInvestorType(investorType);
        novoInvestor.setStatus("PENDING_APPROVAL");
        novoInvestor.setCreatedAt(new Date());

        if ("INDIVIDUAL".equals(investorType)) {
            novoInvestor.setCpf(documento);
        } else { // "FIRM"
            novoInvestor.setCnpj(documento);
        }

        // Cria o documento no Firestore
        investorRepository.createInvestorDocument(novoInvestor, result -> {
            if (result instanceof Result.Success) {
                // Documento criado, agora começa a ouvir
                _state.setValue(VerificationState.VERIFYING);
                listenForVerification();
            } else {
                _errorEvent.setValue(new Event<>("Erro ao criar registro de investidor."));
                _state.setValue(VerificationState.ERROR);
            }
        });
    }

    /**
     * Etapa 2: Ouve as mudanças da Cloud Function.
     */
    public void listenForVerification() {
        if(currentUserId == null) return;

        investorRepository.listenForInvestorVerification(currentUserId, (snapshot, exception) -> {
            if (exception != null) {
                _errorEvent.setValue(new Event<>("Erro ao verificar dados: " + exception.getMessage()));
                _state.setValue(VerificationState.ERROR);
                investorRepository.stopListening(currentUserId);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String status = snapshot.getString("status");
                if ("ACTIVE".equals(status)) {
                    _state.setValue(VerificationState.SUCCESS);
                    investorRepository.stopListening(currentUserId);
                } else if ("REJECTED".equals(status)) {
                    String razao = snapshot.getString("rejectionReason");
                    _errorEvent.setValue(new Event<>("Cadastro Rejeitado: " + razao));
                    _state.setValue(VerificationState.ERROR);
                    investorRepository.stopListening(currentUserId);
                }
                // Se for "PENDING_APPROVAL", continua ouvindo (estado VERIFYING)
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentUserId != null) {
            investorRepository.stopListening(currentUserId);
        }
    }
}