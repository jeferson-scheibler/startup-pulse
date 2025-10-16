// app/src/main/java/com/example/startuppulse/ui/mentor/CanvasMentorViewModel.java

package com.example.startuppulse.ui.mentor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.Mentor;
import com.example.startuppulse.util.Event;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.MentorRepository;
import com.example.startuppulse.data.ResultCallback;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CanvasMentorViewModel extends ViewModel {

    private final MentorRepository mentorRepository;

    private final MutableLiveData<Result<String>> _saveResult = new MutableLiveData<>();
    public LiveData<Result<String>> saveResult = _saveResult;

    // Usamos Event para navegação, garantindo que ela ocorra apenas uma vez
    private final MutableLiveData<Event<Boolean>> _navigateToProfile = new MutableLiveData<>();
    public LiveData<Event<Boolean>> navigateToProfile = _navigateToProfile;

    @Inject
    public CanvasMentorViewModel(MentorRepository mentorRepository) {
        this.mentorRepository = mentorRepository;
    }

    public void saveMentorProfile(Mentor mentor) {
        _saveResult.setValue(new Result.Loading<>());
        mentorRepository.saveMentorProfile(mentor, new ResultCallback<String>() {
            @Override
            public void onResult(Result<String> result) {
                _saveResult.setValue(result);
                if (result instanceof Result.Success) {
                    _navigateToProfile.setValue(new Event<>(true)); // Sinaliza para navegar de volta
                }
            }
        });
    }
}