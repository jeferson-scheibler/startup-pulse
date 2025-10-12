package com.example.startuppulse.ui.investor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.data.Investor;
import com.example.startuppulse.databinding.FragmentInvestorDetailBinding;
import com.google.android.material.chip.Chip;

import java.util.List;

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

        // Usando Safe Args para obter o ID do investidor de forma segura
        String investorId = InvestorDetailFragmentArgs.fromBundle(getArguments()).getInvestorId();
        if (TextUtils.isEmpty(investorId)) {
            Toast.makeText(requireContext(), "ID do Investidor inválido", Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        setupObservers();
        viewModel.loadInvestor(investorId);
    }

    private void setupObservers() {
        viewModel.investor.observe(getViewLifecycleOwner(), investor -> {
            if (investor != null && binding != null) {
                populateUi(investor);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), errorMsg -> {
            if (binding != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateUi(Investor investor) {
        // CORRIGIDO: Usando os getters da sua classe Investor
        binding.investorNameTextView.setText(investor.getNome());
        binding.investorBioTextView.setText(investor.getBio());
        binding.investorTeseTextView.setText(investor.getTese());

        // A classe Investor não tem um campo para "Faixa de Investimento (Cheque)".
        // Vamos esconder essa seção por enquanto.
        binding.labelFaixaInvestimento.setVisibility(View.GONE);
        binding.investorCheckTextView.setVisibility(View.GONE);

        Glide.with(requireContext())
                .load(investor.getFotoUrl())
                .placeholder(R.drawable.ic_person)
                .into(binding.investorPhotoImageView);

        // CORRIGIDO: Usando os getters da sua classe Investor
        populateChipGroup(binding.chipGroupAreas, investor.getAreas());
        populateChipGroup(binding.chipGroupEstagios, investor.getEstagios());

        // Configura o botão do LinkedIn
        String linkedinUrl = investor.getLinkedinUrl();
        if (linkedinUrl != null && !linkedinUrl.isEmpty()) {
            binding.linkedinButton.setVisibility(View.VISIBLE);
            binding.linkedinButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedinUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            binding.linkedinButton.setVisibility(View.GONE);
        }
    }

    private void populateChipGroup(com.google.android.material.chip.ChipGroup chipGroup, List<String> items) {
        chipGroup.removeAllViews();
        if (items != null && !items.isEmpty()) {
            chipGroup.setVisibility(View.VISIBLE);
            for (String item : items) {
                Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
                chip.setText(item);
                // Você pode aplicar seu estilo customizado aqui se necessário
                // ex: chip.setTextAppearance(R.style.Text_BodySmall_Muted);
                chip.setClickable(false);
                chip.setFocusable(false);
                chipGroup.addView(chip);
            }
        } else {
            chipGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}