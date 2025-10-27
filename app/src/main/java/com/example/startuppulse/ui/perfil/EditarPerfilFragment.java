package com.example.startuppulse.ui.perfil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.databinding.FragmentEditarPerfilBinding;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditarPerfilFragment extends Fragment {

    private FragmentEditarPerfilBinding binding;
    private EditarPerfilViewModel viewModel;
    private Uri selectedImageUri;
    private ArrayList<String> selectedAreas = new ArrayList<>();
    private String[] allAreas;
    private boolean[] checkedItems;

    // Launcher para selecionar uma imagem da galeria
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this)
                            .load(selectedImageUri)
                            .circleCrop()
                            .into(binding.imageViewPerfil);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditarPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EditarPerfilViewModel.class);

        allAreas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        checkedItems = new boolean[allAreas.length];

        setupToolbar();
        setupClickListeners();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupClickListeners() {
        // O botão para alterar a foto agora é o ícone de câmera
        binding.btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        binding.btnSelecionarAreas.setOnClickListener(v -> showAreaSelectionDialog());

        binding.btnSalvar.setOnClickListener(v -> {
            String newName = binding.inputEditTextNome.getText().toString().trim();
            String newBio = binding.inputEditTextBio.getText().toString().trim();
            String newProfession = binding.inputEditTextProfissao.getText().toString().trim();
            String linkedinProfileId = binding.inputEditTextLinkedin.getText().toString().trim();
            String finalLinkedinUrl = ""; // Começa como vazio

            // Só monta a URL completa se o usuário tiver digitado algo
            if (!linkedinProfileId.isEmpty()) {
                finalLinkedinUrl = "https://www.linkedin.com/in/" + linkedinProfileId;
            }

            if (newName.isEmpty()) {
                binding.inputLayoutNome.setError("O nome não pode ficar em branco.");
                return;
            } else {
                binding.inputLayoutNome.setError(null);
            }

            // Passa os novos dados para o ViewModel
            viewModel.saveProfile(newName, newBio, selectedImageUri, newProfession, finalLinkedinUrl, selectedAreas);
        });
    }

    private void setupObservers() {
        viewModel.userProfile.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user != null) {
                    binding.inputEditTextNome.setText(user.getNome());
                    binding.inputEditTextBio.setText(user.getBio());
                    binding.inputEditTextProfissao.setText(user.getProfissao());
                    binding.inputEditTextLinkedin.setText(user.getLinkedinUrl());
                    String fullLinkedinUrl = user.getLinkedinUrl();
                    if (fullLinkedinUrl != null && !fullLinkedinUrl.isEmpty()) {
                        // Remove o prefixo padrão para exibir apenas o nome de usuário no campo
                        String profileId = fullLinkedinUrl.replace("https://www.linkedin.com/in/", "");
                        binding.inputEditTextLinkedin.setText(profileId);
                    } else {
                        binding.inputEditTextLinkedin.setText("");
                    }
                    selectedAreas.clear();
                    if (user.getAreasDeInteresse() != null) {
                        selectedAreas.addAll(user.getAreasDeInteresse());
                    }
                    updateChipGroup();
                    Glide.with(this)
                            .load(user.getFotoUrl())
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.imageViewPerfil);
                }
            } else if (result instanceof Result.Error) {
                Toast.makeText(getContext(), "Erro ao carregar perfil", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.updateResult.observe(getViewLifecycleOwner(), result -> {
            boolean isLoading = result instanceof Result.Loading;
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSalvar.setEnabled(!isLoading);


            if (result instanceof Result.Success) {
                Toast.makeText(getContext(), "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
            } else if (result instanceof Result.Error) {
                String errorMsg = ((Result.Error<Void>) result).error.getMessage();
                Toast.makeText(getContext(), "Erro ao salvar: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAreaSelectionDialog() {
        // Reseta o array de itens checados baseado nas áreas já selecionadas
        for (int i = 0; i < allAreas.length; i++) {
            checkedItems[i] = selectedAreas.contains(allAreas[i]);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.selecione_suas_areas_de_interesse)
                .setMultiChoiceItems(allAreas, checkedItems, (dialog, which, isChecked) -> {
                    // Atualiza o estado do item quando o usuário clica
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    selectedAreas.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedAreas.add(allAreas[i]);
                        }
                    }
                    updateChipGroup();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateChipGroup() {
        binding.chipGroupAreas.removeAllViews();
        for (String area : selectedAreas) {
            Chip chip = new Chip(requireContext());
            chip.setText(area);
            chip.setCloseIconVisible(false); // Ou true se quiser permitir remoção por aqui
            binding.chipGroupAreas.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}