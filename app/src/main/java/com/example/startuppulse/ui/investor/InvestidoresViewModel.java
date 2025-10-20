package com.example.startuppulse.ui.investor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.ReadinessCalculator;
import com.example.startuppulse.ReadinessData;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.data.repositories.IdeiaRepository;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.data.repositories.InvestorRepository;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InvestidoresViewModel extends ViewModel {

    private final InvestorRepository investorRepository;
    private final IdeiaRepository ideiaRepository;
    private final AuthRepository authRepository;

    // Controla qual tela mostrar: "prontidão" ou a lista de investidores
    public enum ViewState { LOADING, SHOW_READINESS, SHOW_INVESTORS, ERROR }

    private final MutableLiveData<ViewState> _viewState = new MutableLiveData<>(ViewState.LOADING);
    public LiveData<ViewState> viewState = _viewState;

    private final MutableLiveData<ReadinessData> _readinessData = new MutableLiveData<>();
    public LiveData<ReadinessData> readinessData = _readinessData;

    private final MutableLiveData<List<Investor>> _investors = new MutableLiveData<>();
    public LiveData<List<Investor>> investors = _investors;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    @Inject
    public InvestidoresViewModel(InvestorRepository investorRepository, IdeiaRepository ideiaRepository, AuthRepository authRepository) {
        this.investorRepository = investorRepository;
        this.ideiaRepository = ideiaRepository;
        this.authRepository = authRepository;

        loadInitialData();
    }

    public void loadInitialData() {
        _viewState.setValue(ViewState.LOADING);
        String currentUserId = authRepository.getCurrentUserId();

        if (currentUserId == null) {
            _readinessData.setValue(ReadinessCalculator.calculate(null));
            _viewState.setValue(ViewState.SHOW_READINESS);
            return;
        }
        ideiaRepository.getIdeiasForOwner(currentUserId, result -> {
            if (result instanceof Result.Success) {
                List<Ideia> ideias = ((Result.Success<List<Ideia>>) result).data;
                Ideia ideiaPrincipal = (ideias != null && !ideias.isEmpty()) ? ideias.get(0) : null;

                if (ideiaPrincipal != null && ideiaPrincipal.isProntaParaInvestidores()) {
                    _viewState.setValue(ViewState.SHOW_INVESTORS);
                    loadInvestors();
                } else {
                    _readinessData.setValue(ReadinessCalculator.calculate(ideiaPrincipal));
                    _viewState.setValue(ViewState.SHOW_READINESS);
                }
            } else {
                _error.setValue("Falha ao buscar dados da sua ideia.");
                _viewState.setValue(ViewState.ERROR);
            }
        });
    }

    private void loadInvestors() {
        investorRepository.getInvestidores(result -> {
            if (result instanceof Result.Success) {
                _investors.setValue(((Result.Success<List<Investor>>) result).data);
            } else {
                _error.setValue("Falha ao carregar lista de investidores.");
                _viewState.setValue(ViewState.ERROR);
            }
        });
    }
}