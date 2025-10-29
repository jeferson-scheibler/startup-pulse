package com.example.startuppulse.ui.mentor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.startuppulse.R;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.Cidade;
import com.example.startuppulse.data.Estado;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.databinding.FragmentCanvasMentorBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasMentorFragment extends Fragment {

    private FragmentCanvasMentorBinding binding;
    private CanvasMentorViewModel viewModel;

    // Listas para guardar os dados da API
    private List<Estado> listaDeEstados = new ArrayList<>();
    private List<Cidade> listaDeCidades = new ArrayList<>();

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

        // Popula o ChipGroup com as áreas de atuação
        populateAreasChipGroup();

        setupListeners();
        observeViewModel();

        // Opcional: Adicionar um EditText para Bio no seu XML (fragment_canvas_mentor.xml)
        // Se não houver campo de Bio, apenas passe uma string vazia no saveMentorData()
    }

    /**
     * Carrega as áreas de atuação do XML (R.array.areas_de_atuacao)
     * e cria os Chips dinamicamente.
     */
    private void populateAreasChipGroup() {
        String[] areas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String area : areas) {
            Chip chip = new Chip(requireContext());
            chip.setText(area);
            chip.setCheckable(true);
            binding.chipGroupAreasSelect.addView(chip);
        }
    }

    private void setupListeners() {
        binding.buttonSalvar.setOnClickListener(v -> saveMentorData());
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        // Listener para o AutoComplete de ESTADO
        binding.autoCompleteEstado.setOnItemClickListener((parent, view, position, id) -> {
            String estadoSelecionadoNome = (String) parent.getItemAtPosition(position);
            // Encontra o objeto Estado correspondente
            Estado estadoObj = listaDeEstados.stream()
                    .filter(e -> e.getNome().equals(estadoSelecionadoNome))
                    .findFirst()
                    .orElse(null);

            if (estadoObj != null) {
                // Limpa o campo de cidade
                binding.autoCompleteCidade.setText("");
                binding.layoutCidade.setEnabled(false);
                // Busca as cidades do estado selecionado
                viewModel.onEstadoSelected(estadoObj.getSigla());
            }
        });
    }

    private void observeViewModel() {
        // Observa os dados do Usuário
        viewModel.user.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Popula os campos compartilhados vindos do User
                binding.editTextNome.setText(user.getNome());
                binding.editTextProfissao.setText(user.getProfissao());
                // Pré-seleciona as áreas de interesse do usuário
                preselectChips(user.getAreasDeInteresse());
            }
        });

        // Observa a lista de ESTADOS
        viewModel.estados.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                showLoading(true);
            } else if (result instanceof Result.Success) {
                showLoading(false);
                this.listaDeEstados = ((Result.Success<List<Estado>>) result).data;
                List<String> nomesEstados = this.listaDeEstados.stream()
                        .map(Estado::getNome)
                        .collect(Collectors.toList());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nomesEstados);
                binding.autoCompleteEstado.setAdapter(adapter);
            } else if (result instanceof Result.Error) {
                showLoading(false);
                Toast.makeText(getContext(), "Erro ao carregar estados", Toast.LENGTH_SHORT).show();
            }
        });

        // Observa a lista de CIDADES (corrigindo o bug)
        viewModel.cidades.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                showLoading(true);
            } else if (result instanceof Result.Success) {
                showLoading(false);
                this.listaDeCidades = ((Result.Success<List<Cidade>>) result).data;
                List<String> nomesCidades = this.listaDeCidades.stream()
                        .map(Cidade::getNome)
                        .collect(Collectors.toList());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nomesCidades);
                binding.autoCompleteCidade.setAdapter(adapter);
                // Habilita o campo de cidade APÓS carregar
                binding.layoutCidade.setEnabled(true);
            } else if (result instanceof Result.Error) {
                showLoading(false);
                Toast.makeText(getContext(), "Erro ao carregar cidades", Toast.LENGTH_SHORT).show();
            }
        });

        // Observa o resultado de SALVAR
        viewModel.saveResult.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                showLoading(true);
                binding.buttonSalvar.setEnabled(false);
            } else if (result instanceof Result.Success) {
                showLoading(false);
                Toast.makeText(getContext(), "Perfil de mentor salvo com sucesso!", Toast.LENGTH_SHORT).show();
            } else if (result instanceof Result.Error) {
                showLoading(false);
                binding.buttonSalvar.setEnabled(true);
                String error = ((Result.Error<Void>) result).error.getMessage();
                Toast.makeText(getContext(), "Erro ao salvar: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Observa a navegação
        viewModel.navigateToProfile.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void saveMentorData() {
        // Validação dos campos
        String profissao = binding.editTextProfissao.getText().toString().trim();
        String estado = binding.autoCompleteEstado.getText().toString().trim();
        String cidade = binding.autoCompleteCidade.getText().toString().trim();
        List<String> areas = getSelectedChipTexts();

        // Validação (Bio é opcional)
        // String bio = binding.editTextBio.getText().toString().trim(); // Descomente se adicionar o campo
        String bio = ""; // Passe uma bio vazia por enquanto

        if (profissao.isEmpty()) {
            binding.editTextProfissao.setError("Campo obrigatório");
            return;
        }
        if (estado.isEmpty()) {
            binding.autoCompleteEstado.setError("Campo obrigatório");
            return;
        }
        if (cidade.isEmpty()) {
            binding.autoCompleteCidade.setError("Campo obrigatório");
            return;
        }

        // Envia os dados para o ViewModel
        viewModel.saveMentorProfile(profissao, areas, estado, cidade, bio);
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

    private void preselectChips(List<String> userAreas) {
        if (userAreas == null || userAreas.isEmpty()) return;

        for (int i = 0; i < binding.chipGroupAreasSelect.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupAreasSelect.getChildAt(i);
            if (userAreas.contains(chip.getText().toString())) {
                chip.setChecked(true);
            }
        }
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}