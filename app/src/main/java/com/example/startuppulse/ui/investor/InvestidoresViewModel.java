package com.example.startuppulse.ui.investor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.domain.usercase.ReadinessCalculator;
import com.example.startuppulse.ReadinessData;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor; // NOVO IMPORT
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IIdeiaRepository;   // NOVO: Usar interface
import com.example.startuppulse.data.repositories.IInvestorRepository; // NOVO: Usar interface
import com.example.startuppulse.data.repositories.InvestorPagingResult; // NOVO IMPORT
import com.google.firebase.firestore.DocumentSnapshot; // NOVO IMPORT

import java.util.ArrayList; // NOVO IMPORT
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InvestidoresViewModel extends ViewModel {
    private final IInvestorRepository investorRepository;
    private final IIdeiaRepository ideiaRepository;
    private final AuthRepository authRepository;
    // Controla qual tela mostrar
    public enum ViewState { LOADING, SHOW_READINESS, SHOW_INVESTORS, SHOW_NO_MATCHES, ERROR }
    private final MutableLiveData<ViewState> _viewState = new MutableLiveData<>(ViewState.LOADING);
    public LiveData<ViewState> viewState = _viewState;
    private final MutableLiveData<ReadinessData> _readinessData = new MutableLiveData<>();
    public LiveData<ReadinessData> readinessData = _readinessData;
    private final MutableLiveData<List<Investor>> _investors = new MutableLiveData<>();
    public LiveData<List<Investor>> investors = _investors;
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;
    private final MutableLiveData<List<Ideia>> _userReadyIdeias = new MutableLiveData<>();
    public LiveData<List<Ideia>> userReadyIdeias = _userReadyIdeias;
    private List<String> currentAreaFilter = null;
    private boolean isEntrepreneurReady = false;
    private boolean isInvestorProfileActive = false;
    private boolean hasCheckedIdeias = false;
    private boolean hasCheckedInvestorStatus = false;
    private String currentUserId;
    private DocumentSnapshot lastVisibleInvestor = null;
    private boolean isFetchingInvestors = false;
    private final int PAGE_SIZE = 20;
    @Inject
    public InvestidoresViewModel(IInvestorRepository investorRepository, IIdeiaRepository ideiaRepository, AuthRepository authRepository) {
        this.investorRepository = investorRepository;
        this.ideiaRepository = ideiaRepository;
        this.authRepository = authRepository;

        loadInitialData();
    }

    public void loadInitialData() {
        _viewState.setValue(ViewState.LOADING);
        currentUserId = authRepository.getCurrentUserId();
        isInvestorProfileActive = false;
        hasCheckedIdeias = false;
        hasCheckedInvestorStatus = false;
        currentAreaFilter = null;
        lastVisibleInvestor = null;
        _investors.setValue(new ArrayList<>());

        if (currentUserId == null) {
            _readinessData.setValue(ReadinessCalculator.calculate(null));
            _viewState.setValue(ViewState.SHOW_READINESS);
            return;
        }
        // Verificação 1: Status de Empreendedor
        ideiaRepository.getIdeiasForOwner(currentUserId, result -> {
            boolean isEntrepreneurReady = false;
            if (result instanceof Result.Success) {
                List<Ideia> ideias = ((Result.Success<List<Ideia>>) result).data;
                Ideia ideiaPrincipal = (ideias != null && !ideias.isEmpty()) ? ideias.get(0) : null;
                _readinessData.setValue(ReadinessCalculator.calculate(ideiaPrincipal));

                // Pega TODAS as ideias prontas para o filtro
                List<Ideia> readyIdeias = new ArrayList<>();
                if (ideias != null) {
                    for (Ideia ideia : ideias) {
                        if (ideia.isProntaParaInvestidores()) {
                            isEntrepreneurReady = true;
                            readyIdeias.add(ideia);
                        }
                    }
                }
                _userReadyIdeias.setValue(readyIdeias);

            } else {
                _error.setValue("Falha ao buscar dados da sua ideia.");
            }
            hasCheckedIdeias = true;
            tryShowInvestorsOrReadiness(isEntrepreneurReady);
        });

        // Verificação 2: Status de Investidor
        investorRepository.getInvestorDetails(currentUserId, result -> {
            if (result instanceof Result.Success) {
                Investor investor = ((Result.Success<Investor>) result).data;
                if (investor != null && "ACTIVE".equals(investor.getStatus())) {
                    isInvestorProfileActive = true;
                }
            }
            hasCheckedInvestorStatus = true;
            // Re-chama a verificação caso esta seja a última a terminar
            boolean isReady = _userReadyIdeias.getValue() != null && !_userReadyIdeias.getValue().isEmpty();
            tryShowInvestorsOrReadiness(isReady);
        });
    }

    private void tryShowInvestorsOrReadiness(boolean isEntrepreneurReady) {
        if (!hasCheckedIdeias || !hasCheckedInvestorStatus) {
            return; // Esperando as duas verificações
        }

        if (isEntrepreneurReady || isInvestorProfileActive) {
            _viewState.setValue(ViewState.SHOW_INVESTORS);
            loadInvestors(); // Carrega os investidores (inicialmente sem filtro)
        } else {
            _viewState.setValue(ViewState.SHOW_READINESS);
        }
    }

    /**
     * Carrega a lista de investidores (usando paginação).
     */
    private void loadInvestors() {
        if (isFetchingInvestors) return;
        isFetchingInvestors = true;

        // Salva se esta é a primeira página (antes de atualizar o 'lastVisibleInvestor')
        final boolean isFirstPage = (lastVisibleInvestor == null);

        // Mostra o loading SÓ se for a primeira página (nova busca ou filtro)
        if (isFirstPage) {
            _viewState.setValue(ViewState.LOADING);
        }

        investorRepository.getInvestidoresPaginados(PAGE_SIZE, lastVisibleInvestor, currentAreaFilter, result -> {
            isFetchingInvestors = false;

            if (result instanceof Result.Success) {
                InvestorPagingResult pagingResult = ((Result.Success<InvestorPagingResult>) result).data;
                List<Investor> newInvestors = pagingResult.getInvestors();
                lastVisibleInvestor = pagingResult.getLastVisible(); // Atualiza o cursor para a *próxima* chamada

                // Pega a lista atual (se for paginação) ou cria uma nova (se for 1ª página)
                List<Investor> currentList = (isFirstPage || _investors.getValue() == null) ?
                        new ArrayList<>() :
                        new ArrayList<>(_investors.getValue());

                currentList.addAll(newInvestors);
                _investors.setValue(currentList); // Atualiza o LiveData da lista

                // --- NOVA LÓGICA DE ESTADO ---
                if (currentList.isEmpty()) {
                    // Se a lista *total* (após a busca) está vazia, mostra "Sem Resultados"
                    _viewState.setValue(ViewState.SHOW_NO_MATCHES);
                } else {
                    // Se a lista tem itens, mostra a lista
                    _viewState.setValue(ViewState.SHOW_INVESTORS);
                }

            } else {
                _error.setValue("Falha ao carregar lista de investidores.");
                _viewState.setValue(ViewState.ERROR);
            }
        });
    }
    /**
     * Chamado pelo Fragment quando o usuário seleciona uma ideia no dropdown.
     */
    public void setFilter(Ideia ideia) {
        // Se 'ideia' for nula, o filtro é removido (mostra todos)
        this.currentAreaFilter = (ideia == null || ideia.getAreasNecessarias() == null || ideia.getAreasNecessarias().isEmpty()) ?
                null :
                ideia.getAreasNecessarias();

        // Reseta a lista e a paginação
        this.lastVisibleInvestor = null;
        _investors.setValue(new ArrayList<>()); // <-- Correção do bug de digitação

        // Recarrega a lista com o novo filtro
        loadInvestors();
    }


    public void loadNextPage() {
        // Só busca mais se não estiver buscando e se houver mais páginas (lastVisible não é nulo
        // ou a lista está vazia, indicando a primeira carga)
        if (!isFetchingInvestors && (lastVisibleInvestor != null || _investors.getValue() == null || _investors.getValue().isEmpty())) {
            loadInvestors();
        }
    }
}