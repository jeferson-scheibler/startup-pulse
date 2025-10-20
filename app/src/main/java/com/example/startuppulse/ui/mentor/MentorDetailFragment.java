package com.example.startuppulse.ui.mentor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.databinding.FragmentMentorDetailBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MentorDetailFragment extends Fragment {

    private FragmentMentorDetailBinding binding;
    private MentorDetailViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMentorDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MentorDetailViewModel.class);

        setupToolbar();
        observeViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void observeViewModel() {
        viewModel.mentorDetails.observe(getViewLifecycleOwner(), this::handleMentorResult);
    }

    private void handleMentorResult(Result<Mentor> result) {
        // Esconde todos os componentes antes de tratar o estado
        binding.progressBar.setVisibility(View.GONE);
        binding.errorView.setVisibility(View.GONE);

        if (result instanceof Result.Loading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else if (result instanceof Result.Success) {
            Mentor mentor = ((Result.Success<Mentor>) result).data;
            populateUI(mentor);
        } else if (result instanceof Result.Error) {
            binding.errorView.setVisibility(View.VISIBLE);
            String errorMessage = ((Result.Error<Mentor>) result).error.getMessage();
            Toast.makeText(getContext(), "Erro: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void populateUI(Mentor mentor) {
        if (mentor == null) return;

        binding.toolbar.setTitle(mentor.getName());
        binding.mentorName.setText(mentor.getName());
        binding.mentorHeadline.setText(mentor.getHeadline());
        binding.mentorBio.setText(mentor.getBio());

        Glide.with(this)
                .load(mentor.getImageUrl())
                .into(binding.mentorImage);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Essencial para evitar vazamentos de memória com View Binding
    }
}