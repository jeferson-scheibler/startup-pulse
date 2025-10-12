package com.example.startuppulse.ui.investor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.Investor;
import com.example.startuppulse.data.InvestorRepository;
import com.example.startuppulse.data.ResultCallback;

public class InvestorDetailViewModel extends ViewModel {
    private final InvestorRepository repository;
    private final MutableLiveData<Investor> _investor = new MutableLiveData<>();
    public final LiveData<Investor> investor = _investor;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    public InvestorDetailViewModel() {
        this.repository = InvestorRepository.getInstance();
    }

    public void loadInvestor(String investorId) {
        repository.getInvestorDetails(investorId, new ResultCallback<Investor>() {
            @Override
            public void onSuccess(Investor data) {
                _investor.setValue(data);
            }

            @Override
            public void onError(Exception e) {
                _error.setValue(e.getMessage());
            }
        });
    }
}