package com.example.startuppulse.ui.investor;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.data.repositories.AuthRepository;
import com.example.startuppulse.data.repositories.IInvestorRepository;
import com.example.startuppulse.data.repositories.IStorageRepository;
import com.example.startuppulse.util.Event;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InvestorProfileSetupViewModel extends ViewModel {

    private final IInvestorRepository investorRepository;
    private final IStorageRepository storageRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<Investor> _investor = new MutableLiveData<>();
    public LiveData<Investor> investor = _investor;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _navigationEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> navigationEvent = _navigationEvent;

    private String currentUserId;
    private Uri profilePicUri = null; // URI local da foto

    @Inject
    public InvestorProfileSetupViewModel(IInvestorRepository investorRepository, IStorageRepository storageRepository, AuthRepository authRepository) {
        this.investorRepository = investorRepository;
        this.storageRepository = storageRepository;
        this.authRepository = authRepository;
        loadInvestorData();
    }

    private void loadInvestorData() {
        _isLoading.setValue(true);
        currentUserId = authRepository.getCurrentUserId();
        if (currentUserId == null) {
            _toastEvent.setValue(new Event<>("Erro fatal: Usuário não encontrado."));
            _isLoading.setValue(false);
            return;
        }

        investorRepository.getInvestorDetails(currentUserId, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                _investor.setValue(((Result.Success<Investor>) result).data);
            } else {
                _toastEvent.setValue(new Event<>("Falha ao carregar dados do perfil."));
            }
        });
    }

    public void setProfilePicUri(Uri uri) {
        this.profilePicUri = uri;
    }

    public void saveProfile(String bio, String linkedin, String site, String tese, String ticket, List<String> areas, List<String> estagios) {
        _isLoading.setValue(true);

        if (profilePicUri != null) {
            // 1. Se tem foto, faz upload primeiro
            storageRepository.uploadInvestorProfileImage(currentUserId, profilePicUri, result -> {
                if (result instanceof Result.Success) {
                    String photoUrl = ((Result.Success<String>) result).data;
                    // 2. Com a URL da foto, atualiza o documento
                    updateInvestorDocument(bio, linkedin, site, tese, ticket, areas, estagios, photoUrl);
                } else {
                    _isLoading.setValue(false);
                    _toastEvent.setValue(new Event<>("Falha no upload da foto."));
                }
            });
        } else {
            // 1. Se não tem foto, apenas atualiza o documento
            updateInvestorDocument(bio, linkedin, site, tese, ticket, areas, estagios, null);
        }
    }

    private void updateInvestorDocument(String bio, String linkedin, String site, String tese, String ticket, List<String> areas, List<String> estagios, String photoUrl) {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("bio", bio);
        profileData.put("linkedinUrl", linkedin);
        profileData.put("siteUrl", site);
        profileData.put("tese", tese);
        profileData.put("ticketMedio", ticket);
        profileData.put("areas", areas);
        profileData.put("estagios", estagios);
        if (photoUrl != null) {
            profileData.put("fotoUrl", photoUrl);
        }
        // O status "ACTIVE" já foi setado pela Cloud Function

        investorRepository.updateProfileDetails(currentUserId, profileData, result -> {
            _isLoading.setValue(false);
            if (result instanceof Result.Success) {
                _toastEvent.setValue(new Event<>("Perfil salvo com sucesso!"));
                _navigationEvent.setValue(new Event<>(true));
            } else {
                _toastEvent.setValue(new Event<>("Erro ao salvar o perfil."));
            }
        });
    }
}