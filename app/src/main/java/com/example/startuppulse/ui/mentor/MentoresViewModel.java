package com.example.startuppulse.ui.mentor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.models.User; // MUDADO: de Mentor para User
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.repositories.IUserRepository; // MUDADO: de MentorRepository para IUserRepository
import com.example.startuppulse.data.ResultCallback;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MentoresViewModel extends ViewModel {
    private final IUserRepository userRepository;
    private final MutableLiveData<Result<List<User>>> _mentores = new MutableLiveData<>();
    public LiveData<Result<List<User>>> mentores = _mentores;

    @Inject
    public MentoresViewModel(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void carregarMentores() {
        _mentores.setValue(new Result.Loading<>());
        userRepository.getMentores(new ResultCallback<List<User>>() {
            @Override
            public void onResult(Result<List<User>> result) {
                _mentores.postValue(result);
            }
        });
    }
}