package com.example.startuppulse.ui.investor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.data.repositories.InvestorRepository;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InvestorDetailViewModel extends ViewModel {

    private final InvestorRepository repository;

    // Unifica os estados de sucesso, erro e carregamento em um único LiveData
    private final MutableLiveData<Result<Investor>> _investorResult = new MutableLiveData<>();
    public final LiveData<Result<Investor>> investorResult = _investorResult;

    @Inject
    public InvestorDetailViewModel(InvestorRepository repository, SavedStateHandle savedStateHandle) {
        this.repository = repository;
        // Pega o investorId passado pelo gráfico de navegação de forma segura
        String investorId = savedStateHandle.get("investorId");
        if (investorId != null && !investorId.isEmpty()) {
            loadInvestor(investorId);
        } else {
            _investorResult.setValue(new Result.Error<>(new IllegalArgumentException("ID do investidor não fornecido.")));
        }
    }

    private void loadInvestor(String investorId) {
        // Informa a UI que o carregamento começou
        _investorResult.setValue(new Result.Loading<>());

        repository.getInvestorDetails(investorId, result -> {
            // Posta o resultado final (Success ou Error) vindo do repositório
            _investorResult.postValue(result);
        });
    }
}