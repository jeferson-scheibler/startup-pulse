package com.example.startuppulse.ui.ideias;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.LimiteHelper;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.AuthRepository;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.IdeiaRepository;
import com.example.startuppulse.util.Event;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class IdeiasViewModel extends ViewModel {

    private static final String TAG = "IdeiasViewModel_DEBUG";

    // --- Repositórios ---
    private final IdeiaRepository ideiaRepository;
    private final AuthRepository authRepository;
    private ListenerRegistration publicIdeiasListener;

    // --- LiveData para a UI ---
    private final MutableLiveData<Result<List<Ideia>>> _publicIdeias = new MutableLiveData<>();
    public LiveData<Result<List<Ideia>>> publicIdeias = _publicIdeias;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    // --- Eventos para Ações Únicas ---

    // Evento para navegação, contendo os dados necessários
    public static class NavigationEvent {
        public final String ideiaId;
        public final boolean isReadOnly;
        public NavigationEvent(String ideiaId, boolean isReadOnly) {
            this.ideiaId = ideiaId;
            this.isReadOnly = isReadOnly;
        }
    }
    private final MutableLiveData<Event<NavigationEvent>> _navigateToCanvas = new MutableLiveData<>();
    public LiveData<Event<NavigationEvent>> navigateToCanvas = _navigateToCanvas;

    private final MutableLiveData<Event<String>> _showLimitDialog = new MutableLiveData<>();
    public LiveData<Event<String>> showLimitDialog = _showLimitDialog;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> toastEvent = _toastEvent;


    @Inject
    public IdeiasViewModel(IdeiaRepository ideiaRepository, AuthRepository authRepository) {
        this.ideiaRepository = ideiaRepository;
        this.authRepository = authRepository;
        listenToPublicIdeias();
    }

    private void listenToPublicIdeias() {
        _isLoading.setValue(true);
        // Garante que não haja listeners duplicados
        if (publicIdeiasListener != null) {
            publicIdeiasListener.remove();
        }

        publicIdeiasListener = ideiaRepository.listenToPublicIdeias(result -> {
            _publicIdeias.setValue(result);
            _isLoading.setValue(false);
        });
    }

    public void refresh() {
        _isLoading.setValue(false);
    }

    /**
     * Lógica de negócio para decidir o que fazer quando uma ideia é clicada.
     * O ViewModel recebe o contexto apenas para esta operação, não o armazena.
     */
    public void onIdeiaClicked(Ideia ideia, Context context) {

        Log.d(TAG, "onIdeiaClicked: Ideia clicada com ID = " + ideia.getId() + " e Nome = " + ideia.getNome());

        String currentUserId = authRepository.getCurrentUserId();
        boolean isOwner = authRepository.isCurrentUser(ideia.getOwnerId());
        boolean isMentor = currentUserId != null && currentUserId.equals(ideia.getMentorId());

        if (isOwner || isMentor) {
            Log.d(TAG, "Usuário é dono ou mentor. Navegando para modo de edição.");
            _navigateToCanvas.setValue(new Event<>(new NavigationEvent(ideia.getId(), false)));
        } else {
            Log.d(TAG, "Usuário é visitante. Verificando limite de acesso.");
            LimiteHelper.verificarAcessoIdeia(context, new LimiteHelper.LimiteCallback() {
                @Override
                public void onPermitido() {
                    Log.d(TAG, "Acesso permitido. Navegando para modo de leitura.");
                    _navigateToCanvas.setValue(new Event<>(new NavigationEvent(ideia.getId(), true)));
                }

                @Override
                public void onNegado(String mensagem) {
                    Log.w(TAG, "Acesso negado. Exibindo diálogo de limite.");
                    LimiteHelper.getProximaDataAcessoFormatada(currentUserId, dataFormatada ->
                            _showLimitDialog.postValue(new Event<>(dataFormatada))
                    );
                }
            });
        }
    }

    public void deleteIdeia(String ideiaId) {
        ideiaRepository.deleteIdeia(ideiaId, result -> {
            if (result instanceof Result.Error) {
                _toastEvent.postValue(new Event<>("Erro ao excluir ideia."));
            }
            // Em caso de sucesso, o listener em tempo real (listenToPublicIdeias)
            // receberá a lista atualizada do Firestore e a UI será reconstruída automaticamente.
        });
    }

    public boolean canDeleteIdeia(Ideia ideia) {
        return authRepository.isCurrentUser(ideia.getOwnerId());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Essencial para evitar memory leaks: remover o listener do Firestore quando o ViewModel é destruído.
        if (publicIdeiasListener != null) {
            publicIdeiasListener.remove();
        }
    }
}