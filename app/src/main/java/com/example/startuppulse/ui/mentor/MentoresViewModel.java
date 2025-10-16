package com.example.startuppulse.ui.mentor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.Mentor;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.MentorRepository;
import com.example.startuppulse.data.ResultCallback;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MentoresViewModel extends ViewModel {

    private final MentorRepository mentorRepository;

    private final MutableLiveData<Result<List<Mentor>>> _mentores = new MutableLiveData<>();
    public LiveData<Result<List<Mentor>>> mentores = _mentores;

    @Inject
    public MentoresViewModel(MentorRepository mentorRepository) {
        this.mentorRepository = mentorRepository;
    }

    public void carregarMentores() {
        _mentores.setValue(new Result.Loading<>());
        // Usando o método findAllMentores do repositório. O excludeUserId pode ser adicionado se necessário.
        mentorRepository.findAllMentores(null, new ResultCallback<List<Mentor>>() {
            @Override
            public void onResult(Result<List<Mentor>> result) {
                _mentores.postValue(result);
            }
        });
    }
}