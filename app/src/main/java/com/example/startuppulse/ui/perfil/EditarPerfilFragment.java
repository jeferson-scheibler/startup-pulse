package com.example.startuppulse.ui.perfil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.example.startuppulse.data.models.Cidade;
import com.example.startuppulse.data.models.Estado;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.databinding.FragmentEditarPerfilBinding;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditarPerfilFragment extends Fragment {

    private FragmentEditarPerfilBinding binding;
    private EditarPerfilViewModel viewModel;
    private Uri selectedImageUri;
    private ArrayList<String> selectedAreas = new ArrayList<>();
    private String[] allAreas;
    private boolean[] checkedItems;

    // ADICIONADO: Listas para os spinners
    private List<Estado> listaDeEstados = new ArrayList<>();
    private List<Cidade> listaDeCidades = new ArrayList<>();

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

        // ADICIONADO: Listener para o AutoComplete de ESTADO
        binding.autoCompleteEstado.setOnItemClickListener((parent, view, position, id) -> {
            String estadoSelecionadoNome = (String) parent.getItemAtPosition(position);
            Estado estadoObj = listaDeEstados.stream()
                    .filter(e -> e.getNome().equals(estadoSelecionadoNome))
                    .findFirst()
                    .orElse(null);

            if (estadoObj != null) {
                binding.autoCompleteCidade.setText("");
                binding.layoutCidade.setEnabled(false);
                viewModel.onEstadoSelected(estadoObj.getSigla());
            }
        });

        // AJUSTADO: Botão Salvar
        binding.btnSalvar.setOnClickListener(v -> {
            // 1. Coleta dados do User
            String newName = binding.inputEditTextNome.getText().toString().trim();
            String newUserBio = binding.inputEditTextBio.getText().toString().trim();
            String newProfession = binding.inputEditTextProfissao.getText().toString().trim();
            String linkedinProfileId = binding.inputEditTextLinkedin.getText().toString().trim();
            String finalLinkedinUrl = ""; // Começa como vazio
            if (!linkedinProfileId.isEmpty()) {
                finalLinkedinUrl = "https://www.linkedin.com/in/" + linkedinProfileId;
            }

            if (newName.isEmpty()) {
                binding.inputLayoutNome.setError("O nome não pode ficar em branco.");
                return;
            } else {
                binding.inputLayoutNome.setError(null);
            }

            // 2. Coleta dados do Mentor (dos novos campos)
            String mentorBio = binding.inputEditTextMentorBio.getText().toString().trim();
            String estado = binding.autoCompleteEstado.getText().toString().trim();
            String cidade = binding.autoCompleteCidade.getText().toString().trim();

            // 3. Chama o novo método unificado do ViewModel
            viewModel.saveUnifiedProfile(
                    newName, newUserBio, selectedImageUri, newProfession,
                    finalLinkedinUrl, selectedAreas,
                    mentorBio, estado, cidade
            );
        });
    }

    private void setupObservers() {
        // Observador do USUÁRIO (ajustado para mostrar/ocultar campos de mentor)
        viewModel.userProfile.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                User user = ((Result.Success<User>) result).data;
                if (user != null) {
                    populateUserData(user);

                    // --- LÓGICA DE VISIBILIDADE ---
                    if (user.isMentor()) {
                        binding.groupMentorFields.setVisibility(View.VISIBLE);
                    } else {
                        binding.groupMentorFields.setVisibility(View.GONE);
                    }
                }
            } else if (result instanceof Result.Error) {
                Toast.makeText(getContext(), "Erro ao carregar perfil", Toast.LENGTH_SHORT).show();
            }
        });

        // ADICIONADO: Observador do MENTOR
        viewModel.mentorProfile.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Mentor mentor = ((Result.Success<Mentor>) result).data;
                if (mentor != null) {
                    populateMentorData(mentor);
                }
            } else if (result instanceof Result.Error) {
                // Não é um erro crítico se o perfil de mentor não carregar (pode não existir)
                Toast.makeText(getContext(), "Não foi possível carregar dados de mentor.", Toast.LENGTH_SHORT).show();
            }
        });

        // ADICIONADO: Observador de ESTADOS
        viewModel.estados.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                this.listaDeEstados = ((Result.Success<List<Estado>>) result).data;
                List<String> nomesEstados = this.listaDeEstados.stream()
                        .map(Estado::getNome)
                        .collect(Collectors.toList());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nomesEstados);
                binding.autoCompleteEstado.setAdapter(adapter);

                // Se o mentor já foi carregado, tenta pré-carregar as cidades
                checkAndPreloadCities();
            }
            // Opcional: tratar Loading/Error
        });

        // ADICIONADO: Observador de CIDADES
        viewModel.cidades.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                this.listaDeCidades = ((Result.Success<List<Cidade>>) result).data;
                List<String> nomesCidades = this.listaDeCidades.stream()
                        .map(Cidade::getNome)
                        .collect(Collectors.toList());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nomesCidades);
                binding.autoCompleteCidade.setAdapter(adapter);
                binding.layoutCidade.setEnabled(true);
            }
            // Opcional: tratar Loading/Error
        });


        // Observador do RESULTADO DA ATUALIZAÇÃO (sem alteração)
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

    private void populateUserData(User user) {
        binding.inputEditTextNome.setText(user.getNome());
        binding.inputEditTextBio.setText(user.getBio());
        binding.inputEditTextProfissao.setText(user.getProfissao());
        binding.inputEditTextLinkedin.setText(user.getLinkedinUrl());
        String fullLinkedinUrl = user.getLinkedinUrl();
        if (fullLinkedinUrl != null && !fullLinkedinUrl.isEmpty()) {
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

    // ADICIONADO: Popula os dados do mentor
    private void populateMentorData(Mentor mentor) {
        binding.inputEditTextMentorBio.setText(mentor.getBio());
        // Seta o texto sem disparar o filtro do autocomplete
        binding.autoCompleteEstado.setText(mentor.getState(), false);
        binding.autoCompleteCidade.setText(mentor.getCity(), false);

        // Habilita o campo de cidade, pois ela já está selecionada
        binding.layoutCidade.setEnabled(true);

        // Tenta carregar as cidades do estado pré-selecionado
        checkAndPreloadCities();
    }

    // ADICIONADO: Rotina para carregar cidades do estado salvo
    private void checkAndPreloadCities() {
        if (listaDeEstados.isEmpty()) {
            return; // Lista de estados não carregou ainda
        }

        String estadoSelecionadoNome = binding.autoCompleteEstado.getText().toString();
        if (estadoSelecionadoNome.isEmpty()) {
            return; // Nenhum estado selecionado
        }

        Estado estadoObj = listaDeEstados.stream()
                .filter(e -> e.getNome().equals(estadoSelecionadoNome))
                .findFirst()
                .orElse(null);

        if (estadoObj != null) {
            viewModel.onEstadoSelected(estadoObj.getSigla());
        }
    }


    private void showAreaSelectionDialog() {
        // Reseta o array de itens checados baseado nas áreas já selecionadas
        for (int i = 0; i < allAreas.length; i++) {
            checkedItems[i] = selectedAreas.contains(allAreas[i]);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.selecione_suas_areas_de_interesse)
                .setMultiChoiceItems(allAreas, checkedItems, (dialog, which, isChecked) -> {
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
            chip.setCloseIconVisible(false);
            binding.chipGroupAreas.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}