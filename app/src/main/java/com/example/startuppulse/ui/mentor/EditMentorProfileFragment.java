package com.example.startuppulse.ui.mentor;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.databinding.FragmentEditMentorProfileBinding;
import com.example.startuppulse.util.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditMentorProfileFragment extends Fragment {

    private FragmentEditMentorProfileBinding binding;
    private EditMentorProfileViewModel viewModel;
    private NavController navController;

    private ActivityResultLauncher<String> avatarPickerLauncher;
    private ActivityResultLauncher<String> bannerPickerLauncher;

    private Uri newAvatarUri = null;
    private Uri newBannerUri = null;
    private final List<String> selectedAreas = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa os seletores de imagem
        avatarPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        newAvatarUri = uri;
                        // Exibe a nova imagem como prévia
                        Glide.with(this).load(newAvatarUri).into(binding.mentorAvatar);
                    }
                });

        bannerPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        newBannerUri = uri;
                        // Exibe a nova imagem como prévia
                        Glide.with(this).load(newBannerUri).into(binding.mentorBannerImage);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditMentorProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditMentorProfileViewModel.class);
        navController = NavHostFragment.findNavController(this);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // Observador de loading
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSalvar.setEnabled(!isLoading);
        });

        // Observador de Toast
        viewModel.toastEvent.observe(getViewLifecycleOwner(), new Event.EventObserver<>(
                content -> Toast.makeText(getContext(), content, Toast.LENGTH_SHORT).show()
        ));

        // Observador de Sucesso ao Salvar (fecha a tela)
        viewModel.saveSuccessEvent.observe(getViewLifecycleOwner(), new Event.EventObserver<>(
                success -> {
                    if (success) navController.navigateUp();
                }
        ));

        // Observador principal: popula a UI quando os dados do mentor chegam
        viewModel.mentorDetails.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                populateUi(((Result.Success<Mentor>) result).data);
            } else if (result instanceof Result.Error) {
                Toast.makeText(getContext(), "Erro ao carregar perfil", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        // Botão Fechar (Toolbar)
        binding.toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Botão Salvar
        binding.btnSalvar.setOnClickListener(v -> saveProfile());

        // Botões de Edição de Imagem
        binding.btnEditAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        binding.btnEditBanner.setOnClickListener(v -> bannerPickerLauncher.launch("image/*"));

        // Botão Selecionar Áreas
        binding.btnSelecionarAreas.setOnClickListener(v -> showAreaSelectionDialog());
    }

    /**
     * Preenche os campos da UI com os dados do mentor
     */
    private void populateUi(Mentor mentor) {
        if (!isAdded()) return;

        binding.editTextNome.setText(mentor.getName());
        binding.editTextHeadline.setText(mentor.getHeadline());
        binding.editTextBio.setText(mentor.getBio());

        String fullLinkedinUrl = mentor.getLinkedinUrl(); // Assumindo que mentor.getLinkedinUrl() existe
        if (fullLinkedinUrl != null && !fullLinkedinUrl.isEmpty()) {
            String profileId = fullLinkedinUrl.replace("https://www.linkedin.com/in/", "");
            binding.editTextLinkedin.setText(profileId);
        } else {
            binding.editTextLinkedin.setText("");
        }

        Glide.with(this).load(mentor.getFotoUrl())
                .placeholder(R.drawable.avatar_default_placeholder) // Crie um placeholder
                .into(binding.mentorAvatar);

        Glide.with(this).load(mentor.getBannerUrl())
                .placeholder(R.drawable.fundo_mentores)
                .fallback(R.drawable.fundo_mentores)
                .into(binding.mentorBannerImage);

        // Popula a lista de áreas e atualiza os chips
        if (mentor.getAreas() != null) {
            selectedAreas.clear();
            selectedAreas.addAll(mentor.getAreas());
            updateChipGroup();
        }
    }

    /**
     * Coleta dados da UI e chama o ViewModel para salvar
     */
    private void saveProfile() {
        String nome = binding.editTextNome.getText().toString().trim();
        String headline = binding.editTextHeadline.getText().toString().trim();
        String bio = binding.editTextBio.getText().toString().trim();

        String linkedinProfileId = binding.editTextLinkedin.getText().toString().trim();
        String finalLinkedinUrl = "";
        if (!linkedinProfileId.isEmpty()) {
            finalLinkedinUrl = "https://www.linkedin.com/in/" + linkedinProfileId;
        }

        // Validação simples
        if (nome.isEmpty() || headline.isEmpty()) {
            Toast.makeText(getContext(), "Nome e Headline são obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveProfile(nome, headline, bio, selectedAreas, newAvatarUri, newBannerUri, finalLinkedinUrl);
    }

    /**
     * Mostra o diálogo de seleção de áreas (similar ao do Canvas)
     */
    private void showAreaSelectionDialog() {
        String[] allAreas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        boolean[] checkedItems = new boolean[allAreas.length];
        Set<String> currentAreasSet = new HashSet<>(selectedAreas);
        ArrayList<String> dialogSelectedList = new ArrayList<>(selectedAreas);

        for (int i = 0; i < allAreas.length; i++) {
            checkedItems[i] = currentAreasSet.contains(allAreas[i]);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Selecione suas especialidades")
                .setMultiChoiceItems(allAreas, checkedItems, (dialog, which, isChecked) -> {
                    String selected = allAreas[which];
                    if (isChecked) {
                        dialogSelectedList.add(selected);
                    } else {
                        dialogSelectedList.remove(selected);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    selectedAreas.clear();
                    selectedAreas.addAll(dialogSelectedList);
                    updateChipGroup(); // Atualiza a UI com os novos chips
                })
                .show();
    }

    /**
     * Limpa e recria os chips no ChipGroup
     */
    private void updateChipGroup() {
        binding.chipGroupAreas.removeAllViews();
        for (String area : selectedAreas) {
            Chip chip = new Chip(getContext());
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