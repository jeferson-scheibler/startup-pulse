package com.example.startuppulse.ui.investor;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentInvestorProfileSetupBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestorProfileSetupFragment extends Fragment {

    private FragmentInvestorProfileSetupBinding binding;
    private InvestorProfileSetupViewModel viewModel;

    // Launcher para selecionar a foto de perfil
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onProfileImageSelected
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvestorProfileSetupBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(InvestorProfileSetupViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // A Toolbar não deve ter botão "voltar" nesta tela
        binding.toolbar.setNavigationIcon(null);

        populateAreasChips();
        setupClickListeners();
        setupObservers();
    }

    private void setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener(v -> filePickerLauncher.launch("image/*"));
        binding.imageViewProfilePic.setOnClickListener(v -> filePickerLauncher.launch("image/*"));

        binding.btnSalvarPerfil.setOnClickListener(v -> saveProfile());
    }

    private void setupObservers() {
        viewModel.investor.observe(getViewLifecycleOwner(), investor -> {
            if (investor == null) return;
            // Preenche os campos se os dados já existirem (ex: re-edição)
            binding.editTextBio.setText(investor.getBio());
            binding.editTextLinkedin.setText(investor.getLinkedinUrl());
            binding.editTextSite.setText(investor.getSiteUrl());
            binding.editTextTese.setText(investor.getTese());
            binding.editTextTicket.setText(investor.getTicketMedio());
            Glide.with(this)
                    .load(investor.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(binding.imageViewProfilePic);

            List<String> estagiosSalvos = investor.getEstagios();
            if (estagiosSalvos != null) {
                // Itera sobre os chips DENTRO do chipGroup
                for (int i = 0; i < binding.chipGroupEstagios.getChildCount(); i++) {
                    View childView = binding.chipGroupEstagios.getChildAt(i);
                    if (childView instanceof Chip) {
                        Chip chip = (Chip) childView;
                        // Verifica se o texto do chip está na lista salva
                        if (estagiosSalvos.contains(chip.getText().toString())) {
                            chip.setChecked(true);
                        }
                    }
                }
            }
            List<String> areasSalvas = investor.getAreas();
            if (areasSalvas != null) {
                // Itera sobre os chips DENTRO do chipGroup
                for (int i = 0; i < binding.chipGroupAreas.getChildCount(); i++) {
                    View childView = binding.chipGroupAreas.getChildAt(i);
                    if (childView instanceof Chip) {
                        Chip chip = (Chip) childView;
                        if (areasSalvas.contains(chip.getText().toString())) {
                            chip.setChecked(true);
                        }
                    }
                }
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSalvarPerfil.setEnabled(!isLoading);
            binding.btnSalvarPerfil.setText(isLoading ? "" : "Salvar Perfil e Concluir");
        });

        viewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.navigationEvent.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                // Concluído! Navega para a tela principal (ou para a home).
                // Vamos navegar de volta para a tela de Perfil.
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_investorProfileSetupFragment_to_perfilFragment);
            }
        });
    }

    private void onProfileImageSelected(Uri uri) {
        if (uri != null) {
            viewModel.setProfilePicUri(uri);
            // Exibe a imagem selecionada
            Glide.with(this).load(uri).into(binding.imageViewProfilePic);
        }
    }

    private void saveProfile() {
        // Validação (simplificada, adicione mais se necessário)
        String bio = binding.editTextBio.getText().toString().trim();
        String linkedin = binding.editTextLinkedin.getText().toString().trim();
        String ticket = binding.editTextTicket.getText().toString().trim();

        if (bio.isEmpty() || linkedin.isEmpty() || ticket.isEmpty()) {
            Toast.makeText(getContext(), "Bio, LinkedIn e Ticket Médio são obrigatórios.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveProfile(
                bio,
                linkedin,
                binding.editTextSite.getText().toString().trim(),
                binding.editTextTese.getText().toString().trim(),
                ticket,
                getSelectedChips(binding.chipGroupAreas),
                getSelectedChips(binding.chipGroupEstagios)
        );
    }

    /**
     * Pega os textos dos chips selecionados em um ChipGroup.
     */
    private List<String> getSelectedChips(ChipGroup chipGroup) {
        List<String> selected = new ArrayList<>();
        for (int id : chipGroup.getCheckedChipIds()) {
            Chip chip = chipGroup.findViewById(id);
            selected.add(chip.getText().toString());
        }
        return selected;
    }

    /**
     * Lê o string-array de 'areas.xml' e cria os Chips dinamicamente.
     */
    private void populateAreasChips() {
        String[] areas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        for (String area : areas) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter_layout, binding.chipGroupAreas, false);
            chip.setText(area);
            binding.chipGroupAreas.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}