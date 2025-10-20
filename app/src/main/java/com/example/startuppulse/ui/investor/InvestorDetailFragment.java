package com.example.startuppulse.ui.investor;

import android.content.Intent;
import android.net.Uri;
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
import com.example.startuppulse.R;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.databinding.FragmentInvestorDetailBinding;
import com.google.android.material.chip.Chip;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestorDetailFragment extends Fragment {

    private FragmentInvestorDetailBinding binding;
    private InvestorDetailViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInvestorDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(InvestorDetailViewModel.class);

        setupToolbar();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void setupObservers() {
        viewModel.investorResult.observe(getViewLifecycleOwner(), result -> {
            if (binding == null) return;

            // Esconde todos os estados por padrão
            binding.progressBar.setVisibility(View.GONE);
            binding.contentScrollView.setVisibility(View.GONE);
            binding.errorView.setVisibility(View.GONE);

            if (result instanceof Result.Loading) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else if (result instanceof Result.Success) {
                binding.contentScrollView.setVisibility(View.VISIBLE);
                Investor investor = ((Result.Success<Investor>) result).data;
                if (investor != null) {
                    populateUi(investor);
                }
            } else if (result instanceof Result.Error) {
                binding.errorView.setVisibility(View.VISIBLE);
                String errorMsg = ((Result.Error<Investor>) result).error.getMessage();
                binding.errorView.setText("Erro ao carregar: " + errorMsg);
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateUi(Investor investor) {
        binding.toolbar.setTitle(investor.getNome());
        binding.investorNameTextView.setText(investor.getNome());
        binding.investorBioTextView.setText(investor.getBio());
        binding.investorTeseTextView.setText(investor.getTese());

        Glide.with(this)
                .load(investor.getFotoUrl())
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(binding.investorPhotoImageView);

        populateChipGroup(binding.chipGroupAreas, investor.getAreas());
        populateChipGroup(binding.chipGroupEstagios, investor.getEstagios());

        String linkedinUrl = investor.getLinkedinUrl();
        if (linkedinUrl != null && !linkedinUrl.isEmpty()) {
            binding.linkedinButton.setVisibility(View.VISIBLE);
            binding.linkedinButton.setOnClickListener(v -> openLink(linkedinUrl));
        } else {
            binding.linkedinButton.setVisibility(View.GONE);
        }
    }

    private void populateChipGroup(com.google.android.material.chip.ChipGroup chipGroup, List<String> items) {
        chipGroup.removeAllViews();
        if (items != null && !items.isEmpty()) {
            chipGroup.setVisibility(View.VISIBLE);
            for (String item : items) {
                Chip chip = new Chip(requireContext());
                chip.setText(item);
                chip.setClickable(false);
                chip.setFocusable(false);
                chipGroup.addView(chip);
            }
        } else {
            chipGroup.setVisibility(View.GONE);
        }
    }

    private void openLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}