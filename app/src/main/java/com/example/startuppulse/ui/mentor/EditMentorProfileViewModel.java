package com.example.startuppulse.ui.mentor;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IMentorRepository;
import com.example.startuppulse.data.repositories.IStorageRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.data.repositories.MentorRepository;
import com.example.startuppulse.util.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditMentorProfileViewModel extends ViewModel {

    private final IMentorRepository mentorRepository;
    private final IUserRepository userRepository;
    private final IStorageRepository storageRepository;
    private final String currentUserId;

    private final MutableLiveData<Result<Mentor>> _mentorDetails = new MutableLiveData<>();
    public LiveData<Result<Mentor>> mentorDetails = _mentorDetails;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastEvent = new MutableLiveData<>();
    public LiveData<Event<String>> toastEvent = _toastEvent;

    private final MutableLiveData<Event<Boolean>> _saveSuccessEvent = new MutableLiveData<>();
    public LiveData<Event<Boolean>> saveSuccessEvent = _saveSuccessEvent;

    @Inject
    public EditMentorProfileViewModel(IMentorRepository mentorRepository,
                                      IUserRepository userRepository,
                                      IStorageRepository storageRepository,
                                      IAuthRepository authRepository) {
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
        this.storageRepository = storageRepository;

        this.currentUserId = authRepository.getCurrentUserId();

        if (currentUserId != null) {
            fetchMentorData();
        } else {
            _mentorDetails.setValue(new Result.Error<>(new Exception("Nenhum usuário logado.")));
        }
    }

    /**
     * Busca os dados do mentor logado no repositório.
     */
    public void fetchMentorData() {
        if (currentUserId == null) return;
        _isLoading.setValue(true);
        _mentorDetails.setValue(new Result.Loading<>());
        mentorRepository.getMentorById(currentUserId, result -> {
            _mentorDetails.postValue(result);
            _isLoading.postValue(false);
        });
    }

    /**
     * Salva o perfil do mentor.
     * O MentorRepository deve ser responsável por decidir se faz upload de imagens
     * (baseado nas URIs) antes de salvar no Firestore.
     */
    public void saveProfile(String nome, String headline, String bio, List<String> areas,
                            Uri newAvatarUri, Uri newBannerUri, String linkedinUrl) {

        if (_mentorDetails.getValue() == null || !(_mentorDetails.getValue() instanceof Result.Success)) {
            _toastEvent.setValue(new Event<>("Erro: Não foi possível carregar os dados originais."));
            return;
        }
        _isLoading.setValue(true);

        // Pega os dados de texto para passar adiante
        Map<String, Object> textData = new HashMap<>();
        textData.put("nome", nome);
        textData.put("headline", headline);
        textData.put("bio", bio);
        textData.put("areas", areas);
        textData.put("linkedinUrl", linkedinUrl);

        // Inicia a cadeia de uploads, começando pelo Avatar
        uploadAvatar(newAvatarUri, newBannerUri, textData);
    }
    private void uploadAvatar(Uri newAvatarUri, Uri newBannerUri, Map<String, Object> textData) {
        if (newAvatarUri != null) {
            // Se há um novo avatar, faz o upload primeiro
            String fileName = currentUserId + "_avatar.jpg";
            String folderPath = "mentor_images/" + currentUserId;

            storageRepository.uploadImage(newAvatarUri, folderPath, fileName, result -> {
                if (result instanceof Result.Success) {
                    String avatarUrl = ((Result.Success<String>) result).data;
                    // Passa a nova URL do avatar para o próximo passo
                    uploadBanner(newBannerUri, avatarUrl, textData);
                } else {
                    // Falha no upload do avatar
                    String errorMsg = ((Result.Error<String>) result).error.getMessage();
                    _toastEvent.setValue(new Event<>("Falha no upload do avatar: " + errorMsg));
                }
            });
        } else {
            // Se não há novo avatar, pula para o upload do banner, passando 'null' como URL do avatar
            uploadBanner(newBannerUri, null, textData);
        }
    }

    private void uploadBanner(Uri newBannerUri, @Nullable String avatarUrl, Map<String, Object> textData) {
        if (newBannerUri != null) {
            // Se há um novo banner, faz o upload
            String fileName = currentUserId + "_banner.jpg";
            String folderPath = "mentor_images/" + currentUserId;

            storageRepository.uploadImage(newBannerUri, folderPath, fileName, new ResultCallback<String>() {
                @Override
                public void onResult(Result<String> result) {
                    if (result instanceof Result.Success) {
                        String bannerUrl = ((Result.Success<String>) result).data;
                        performFullMentorUpdate(avatarUrl, bannerUrl, textData);
                    } else {
                        _isLoading.setValue(false);
                        // <-- CORREÇÃO: Usa _toastEvent
                        String errorMsg = ((Result.Error<String>) result).error.getMessage();
                        _toastEvent.setValue(new Event<>("Falha no upload do banner: " + errorMsg));
                    }
                }
            });
        } else {
            // Se não há novo banner, chama o passo final com 'null' como URL do banner
            performFullMentorUpdate(avatarUrl, null, textData);
        }
    }

    private void performFullMentorUpdate(@Nullable String avatarUrl, @Nullable String bannerUrl, Map<String, Object> textData) {

        // Pega os dados de texto do Map
        String nome = (String) textData.get("nome");
        String headline = (String) textData.get("headline");
        String bio = (String) textData.get("bio");
        List<String> areas = (List<String>) textData.get("areas");
        String linkedinUrl = (String) textData.get("linkedinUrl");

        // Monta o Map final para o Firestore
        Map<String, Object> mentorData = new HashMap<>();
        mentorData.put("name", nome);
        mentorData.put("headline", headline);
        mentorData.put("bio", bio);
        mentorData.put("areas", areas);
        mentorData.put("linkedinUrl", linkedinUrl);

        if (avatarUrl != null) {
            mentorData.put("fotoUrl", avatarUrl);
        }
        if (bannerUrl != null) {
            mentorData.put("bannerUrl", bannerUrl);
        }

        // Atualiza o Mentor no Firestore
        mentorRepository.updateMentorFieldsByOwnerId(currentUserId, mentorData, new ResultCallback<Void>() {
            @Override
            public void onResult(Result<Void> result) {
                if (result instanceof Result.Success) {
                    syncUserData(nome, headline, areas, linkedinUrl, avatarUrl);
                } else {
                    _isLoading.setValue(false);
                    // <-- CORREÇÃO: Usa _toastEvent
                    String errorMsg = ((Result.Error<Void>) result).error.getMessage();
                    _toastEvent.setValue(new Event<>("Falha ao salvar perfil de mentor: " + errorMsg));
                }
            }
        });
    }

    private void syncUserData(String nome, String headline, List<String> areas, String linkedinUrl, @Nullable String avatarUrl) {

            _isLoading.setValue(true);

            userRepository.updateUserProfile(
                    currentUserId,
                    nome,
                    null, // Bio do usuário não é sincronizada
                    avatarUrl, // A nova URL da foto (ou null)
                    headline, // Headline do mentor -> Profissão do usuário
                    linkedinUrl,
                    areas, // Areas do mentor -> Areas de Interesse do usuário
                    new ResultCallback<Void>() {
                        @Override
                        public void onResult(Result<Void> result) {
                            _isLoading.setValue(false); // Loading finalizado
                            if (result instanceof Result.Success) {
                                _toastEvent.setValue(new Event<>("Perfil atualizado e sincronizado!"));
                                _saveSuccessEvent.setValue(new Event<>(true));
                            } else {
                                _toastEvent.setValue(new Event<>("Perfil de mentor salvo (falha na sincronia com usuário)."));
                                _saveSuccessEvent.setValue(new Event<>(true)); // Ainda fecha a tela
                            }
                        }
                    }
            );
    }
}