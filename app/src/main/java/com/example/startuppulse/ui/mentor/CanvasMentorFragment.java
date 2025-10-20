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

import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.databinding.FragmentCanvasMentorBinding;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasMentorFragment extends Fragment {

    private FragmentCanvasMentorBinding binding;
    private CanvasMentorViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasMentorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CanvasMentorViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.buttonSalvar.setOnClickListener(v -> saveMentorData());
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void observeViewModel() {
        viewModel.saveResult.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                // Opcional: mostrar um dialog de loading
                binding.buttonSalvar.setEnabled(false);
            } else if (result instanceof Result.Success) {
                Toast.makeText(getContext(), "Perfil de mentor salvo com sucesso!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                binding.buttonSalvar.setEnabled(true);
                String error = ((Result.Error<String>) result).error.getMessage();
                Toast.makeText(getContext(), "Erro ao salvar: " + error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.navigateToProfile.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void saveMentorData() {
        // Validação básica
        String profissao = binding.editTextProfissao.getText().toString().trim();
        if (profissao.isEmpty()) {
            binding.editTextProfissao.setError("Campo obrigatório");
            return;
        }

        Mentor mentor = new Mentor();
        mentor.setHeadline(profissao); // Usando o campo 'headline' do novo modelo
        mentor.setCity(binding.autoCompleteCidade.getText().toString());
        mentor.setState(binding.autoCompleteEstado.getText().toString());
        mentor.setAreas(getSelectedChipTexts());

        // Outros campos podem ser setados aqui se necessário
        // mentor.setName(...) virá do perfil do usuário, que pode ser buscado no ViewModel

        viewModel.saveMentorProfile(mentor);
    }

    private List<String> getSelectedChipTexts() {
        List<String> selectedTexts = new ArrayList<>();
        for (int i = 0; i < binding.chipGroupAreasSelect.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupAreasSelect.getChildAt(i);
            if (chip.isChecked()) {
                selectedTexts.add(chip.getText().toString());
            }
        }
        return selectedTexts;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}