package com.example.startuppulse.ui.perfil;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IMentorRepository;
import com.example.startuppulse.data.repositories.IStorageRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditarPerfilViewModel extends ViewModel {

    private final IAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final IStorageRepository storageRepository;
    private final IMentorRepository mentorRepository;

    private final MutableLiveData<Result<User>> _userProfile = new MutableLiveData<>();
    public final LiveData<Result<User>> userProfile = _userProfile;

    private final MutableLiveData<Result<Void>> _updateResult = new MutableLiveData<>();
    public final LiveData<Result<Void>> updateResult = _updateResult;

    @Inject
    public EditarPerfilViewModel(IAuthRepository authRepository, IUserRepository userRepository, IStorageRepository storageRepository, IMentorRepository mentorRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.storageRepository = storageRepository;
        this.mentorRepository = mentorRepository;
        loadUserProfile();
    }

    private void loadUserProfile() {
        String userId = authRepository.getCurrentUserId();
        if (userId != null) {
            _userProfile.setValue(new Result.Loading<>());
            userRepository.getUserProfile(userId, result -> _userProfile.postValue(result));
        } else {
            _userProfile.setValue(new Result.Error<>(new Exception("Usuário não autenticado.")));
        }
    }

    public void saveProfile(String newName, String newBio, Uri newPhotoUri, String newProfession, String newLinkedinUrl, List<String> newAreas) {
        String userId = authRepository.getCurrentUserId();
        if (userId == null) {
            _updateResult.setValue(new Result.Error<>(new Exception("Usuário não encontrado.")));
            return;
        }

        _updateResult.setValue(new Result.Loading<>());

        if (newPhotoUri != null) {
            // Se há uma nova foto, faz o upload primeiro
            String fileName = userId + ".jpg";
            String folderPath = "profile_images/" + userId;
            storageRepository.uploadImage(newPhotoUri, folderPath, fileName, result -> {
                if (result instanceof Result.Success) {
                    String photoUrl = ((Result.Success<String>) result).data;
                    performFullProfileUpdate(userId, newName, newBio, photoUrl, newProfession, newLinkedinUrl, newAreas);
                } else {
                    _updateResult.setValue(new Result.Error<>(((Result.Error<String>) result).error));
                }
            });
        } else {
            performFullProfileUpdate(userId, newName, newBio, null, newProfession, newLinkedinUrl, newAreas);
        }
    }

    private void performFullProfileUpdate(String userId, String name, String bio, String photoUrl, String profession, String linkedinUrl, List<String> areas) {

        userRepository.updateUserProfile(userId, name, bio, photoUrl, profession, linkedinUrl, areas,
                new ResultCallback<Void>() {
                    @Override
                    public void onResult(Result<Void> userResult) {
                        if (userResult instanceof Result.Success) {
                            // Passo 2: Sincroniza com o Mentor
                            syncMentorData(userId, name, profession, photoUrl, linkedinUrl, areas);

                        } else if (userResult instanceof Result.Error) {
                            // Se o Passo 1 falhar, relate o erro e pare.
                            _updateResult.postValue(userResult);
                        }
                    }
                }
        );
    }

    private void syncMentorData(String userIdAsOwnerId, String name, String profession, String photoUrl, String linkedinUrl, List<String> areas) {

        Map<String, Object> mentorData = new HashMap<>();
        mentorData.put("nome", name);
        mentorData.put("profissao", profession);
        mentorData.put("linkedinUrl", linkedinUrl);
        mentorData.put("areas", areas);

        if (photoUrl != null) {
            mentorData.put("fotoUrl", photoUrl);
        }
        mentorRepository.updateMentorFieldsByOwnerId(userIdAsOwnerId, mentorData,
                new ResultCallback<Void>() {
                    @Override
                    public void onResult(Result<Void> mentorResult) {
                        _updateResult.postValue(mentorResult);
                    }
                }
        );
    }

    private void updateUserDocument(String userId, String name, String bio, String photoUrl, String profession, String linkedinUrl, List<String> areas) {
        userRepository.updateUserProfile(userId, name, bio, photoUrl, profession, linkedinUrl, areas, result -> _updateResult.postValue(result));
    }
}