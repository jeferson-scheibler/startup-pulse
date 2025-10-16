package com.example.startuppulse.ui.mentor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.MentorRepository;
import com.example.startuppulse.data.Mentor;
import com.example.startuppulse.data.ResultCallback;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MentorDetailViewModel extends ViewModel {

    private final MentorRepository mentorRepository;
    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<Result<Mentor>> _mentorDetails = new MutableLiveData<>();
    public LiveData<Result<Mentor>> mentorDetails = _mentorDetails;

    @Inject
    public MentorDetailViewModel(MentorRepository mentorRepository, SavedStateHandle savedStateHandle) {
        this.mentorRepository = mentorRepository;
        this.savedStateHandle = savedStateHandle;

        // Recupera o ID do mentor passado através do gráfico de navegação
        String mentorId = savedStateHandle.get("mentorId");
        if (mentorId != null && !mentorId.isEmpty()) {
            fetchMentorDetails(mentorId);
        } else {
            _mentorDetails.setValue(new Result.Error<>(new Exception("Mentor ID is missing.")));
        }
    }

    private void fetchMentorDetails(String mentorId) {
        _mentorDetails.setValue(new Result.Loading<>());
        mentorRepository.getMentorById(mentorId, new ResultCallback<Mentor>() {
            @Override
            public void onResult(Result<Mentor> result) {
                _mentorDetails.postValue(result);
            }
        });
    }
}