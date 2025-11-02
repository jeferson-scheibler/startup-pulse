package com.example.startuppulse.ui.investor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.repositories.InvestorRepository;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.util.Event;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InvestorRegistrationViewModel extends ViewModel {

    // Enum para controlar o estado da UI
    public enum RegistrationState {
        IDLE,       // Ocioso
        LOADING,    // Carregando (criação do usuário)
        VERIFYING,  // Usuário criado, aguardando a Cloud Function
        SUCCESS,    // Verificado com sucesso (status: ACTIVE)
        ERROR       // Erro em qualquer etapa
    }

    private final AuthRepository authRepository;
    private final InvestorRepository investorRepository;

    private final MutableLiveData<RegistrationState> _state = new MutableLiveData<>(RegistrationState.IDLE);
    public final LiveData<RegistrationState> state = _state;

    private final MutableLiveData<Event<String>> _errorEvent = new MutableLiveData<>();
    public final LiveData<Event<String>> errorEvent = _errorEvent;

    // Guarda o ID do usuário para o listener
    private String currentUserId = null;

    @Inject
    public InvestorRegistrationViewModel(AuthRepository authRepository, InvestorRepository investorRepository) {
        this.authRepository = authRepository;
        this.investorRepository = investorRepository;
    }

    /**
     * Etapa 1: Inicia o processo de registro.
     */
    public void startRegistration(String email, String password, String nome, String tipo, String documento) {
        _state.setValue(RegistrationState.LOADING);

        // 1. Cria o usuário no Firebase Auth
        authRepository.createUser(email, password, nome, result -> {
            if (result instanceof Result.Success) {
                FirebaseUser user = ((Result.Success<FirebaseUser>) result).data;
                this.currentUserId = user.getUid();
                // 2. Cria o documento "PENDING" no Firestore
                createInvestorDocument(user.getUid(), email, nome, tipo, documento);
            } else {
                String error = ((Result.Error<FirebaseUser>) result).error.getMessage();
                _errorEvent.setValue(new Event<>(error));
                _state.setValue(RegistrationState.ERROR);
            }
        });
    }

    /**
     * Etapa 2: Cria o documento inicial no Firestore para a Cloud Function pegar.
     */
    private void createInvestorDocument(String uid, String email, String nome, String tipo, String documento) {
        Investor novoInvestor = new Investor();
        novoInvestor.setId(uid); // Mesmo ID do Auth
        novoInvestor.setEmailContato(email);
        novoInvestor.setNome(nome); // Nome do responsável ou anjo
        novoInvestor.setInvestorType(tipo); // "INDIVIDUAL" ou "FIRM"
        novoInvestor.setStatus("PENDING_APPROVAL"); // Status inicial obrigatório
        novoInvestor.setCreatedAt(new Date());

        if (tipo.equals("INDIVIDUAL")) {
            novoInvestor.setCpf(documento);
        } else {
            novoInvestor.setCnpj(documento);
        }

        // 3. Chama o repositório para salvar o documento
        investorRepository.createInvestorDocument(novoInvestor, result -> {
            if (result instanceof Result.Success) {
                // 4. Documento criado, agora começa a ouvir
                _state.setValue(RegistrationState.VERIFYING);
            } else {
                _errorEvent.setValue(new Event<>("Erro ao salvar dados do investidor."));
                _state.setValue(RegistrationState.ERROR);
            }
        });
    }

    public void beginListening() {
        if (currentUserId == null) {
            currentUserId = authRepository.getCurrentUserId();
        }

        if (currentUserId == null) {
            _errorEvent.setValue(new Event<>("Usuário não autenticado. Impossível verificar."));
            _state.setValue(RegistrationState.ERROR);
            return;
        }

        // Garante que o estado inicial seja 'VERIFYING' enquanto escuta
        _state.setValue(RegistrationState.VERIFYING);
        listenForVerification(currentUserId);
    }

    /**
     * Etapa 3: Ouve o documento no Firestore em tempo real.
     */
    private void listenForVerification(String uid) {
        investorRepository.listenForInvestorVerification(uid, (snapshot, exception) -> {
            if (exception != null) {
                _errorEvent.setValue(new Event<>("Erro ao verificar dados: " + exception.getMessage()));
                _state.setValue(RegistrationState.ERROR);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Investor investor = snapshot.toObject(Investor.class);
                if (investor == null) return;

                String status = investor.getStatus();
                if ("ACTIVE".equals(status)) {
                    _state.setValue(RegistrationState.SUCCESS);
                    // Para de ouvir após o sucesso
                    investorRepository.stopListening(uid);
                } else if ("REJECTED".equals(status)) {
                    String razao = investor.getRejectionReason(); // Precisamos adicionar este campo no Investor.java
                    _errorEvent.setValue(new Event<>("Cadastro Rejeitado: " + razao));
                    _state.setValue(RegistrationState.ERROR);
                    // Para de ouvir após a rejeição
                    investorRepository.stopListening(uid);
                }
                // Se for "PENDING_APPROVAL", ele continua ouvindo (estado VERIFYING)
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Garante que o listener seja removido se o ViewModel for destruído
        if (currentUserId != null) {
            investorRepository.stopListening(currentUserId);
        }
    }
}
