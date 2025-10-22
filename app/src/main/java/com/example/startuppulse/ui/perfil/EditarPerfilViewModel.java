package com.example.startuppulse.ui.perfil;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IStorageRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditarPerfilViewModel extends ViewModel {

    private final IAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final IStorageRepository storageRepository;

    private final MutableLiveData<Result<User>> _userProfile = new MutableLiveData<>();
    public final LiveData<Result<User>> userProfile = _userProfile;

    private final MutableLiveData<Result<Void>> _updateResult = new MutableLiveData<>();
    public final LiveData<Result<Void>> updateResult = _updateResult;

    @Inject
    public EditarPerfilViewModel(IAuthRepository authRepository, IUserRepository userRepository, IStorageRepository storageRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.storageRepository = storageRepository;
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
                    updateUserDocument(userId, newName, newBio, photoUrl, newProfession, newLinkedinUrl, newAreas);
                } else {
                    _updateResult.setValue(new Result.Error<>(((Result.Error<String>) result).error));
                }
            });
        } else {
            // Se não há nova foto, apenas atualiza os dados de texto
            updateUserDocument(userId, newName, newBio, null, newProfession, newLinkedinUrl, newAreas);
        }
    }

    private void updateUserDocument(String userId, String name, String bio, String photoUrl, String profession, String linkedinUrl, List<String> areas) {
        userRepository.updateUserProfile(userId, name, bio, photoUrl, profession, linkedinUrl, areas, result -> _updateResult.postValue(result));
    }
}