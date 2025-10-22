package com.example.startuppulse;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
// NOVO IMPORT: Para encontrar o NavController principal
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.databinding.FragmentPerfilBinding;
import com.example.startuppulse.ui.perfil.PerfilViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private PerfilViewModel viewModel;
    private NavController localNavController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

        Log.d("PerfilFragment", "onViewCreated: Initializing profile data load.");
        viewModel.loadUserProfile();
        setupObservers();
        setupClickListeners();
    }
    private void setupObservers() {
        viewModel.userProfileResult.observe(getViewLifecycleOwner(), result -> {
            if (binding == null) return;

            if (result instanceof Result.Success) {
                // Se for sucesso, DESEMBRULHA o objeto User antes de chamar populateUi
                User user = ((Result.Success<User>) result).data;
                if (user != null) {
                    populateUi(user);
                    Log.d("PerfilFragment", "User profile loaded, requesting stats...");
                    viewModel.carregarDadosEstatisticas();
                }
            } else if (result instanceof Result.Error) {
                String errorMsg = ((Result.Error<User>) result).error.getMessage();
                Toast.makeText(getContext(), "Erro: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isMentor.observe(getViewLifecycleOwner(), isMentor -> {
            if (binding != null) {
                binding.buttonTornarMentor.setVisibility(isMentor ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getIdeiasPublicas().observe(getViewLifecycleOwner(), valor -> {
            if (binding != null) {
                Log.d("PerfilFragment", "Observer IdeiasPublicas: " + valor);
                binding.statPublicadas.setText(String.valueOf(valor != null ? valor : 0));
            }
        });

        viewModel.getDiasAcessados().observe(getViewLifecycleOwner(), valor -> {
            if (binding != null) {
                Log.d("PerfilFragment", "Observer DiasAcessados: " + valor);
                binding.statDias.setText(String.valueOf(valor != null ? valor : 0));
            }
        });

        viewModel.getNivelEngajamento().observe(getViewLifecycleOwner(), valor -> {
            if (binding != null) {
                int score = (valor != null ? valor : 0);
                Log.d("PerfilFragment", "Observer NivelEngajamento: " + score);
                // Atualiza o texto central
                binding.statEngajamentoValue.setText(String.valueOf(score));
                // Atualiza a barra de progresso circular (com animação)
                binding.progressEngagement.setProgressCompat(score, true);
            }
        });

        viewModel.validadePlanoDisplay.observe(getViewLifecycleOwner(), validade -> {
            if (binding != null) {
                binding.textViewValidadePlano.setText(validade);
                // Mostra ou esconde o TextView baseado se a string está vazia
                binding.textViewValidadePlano.setVisibility(validade.isEmpty() ? View.GONE : View.VISIBLE);
                Log.d("PerfilFragment", "Observer ValidadePlanoDisplay: " + validade);
            }
        });


        viewModel.isPro.observe(getViewLifecycleOwner(), isPro -> {
            if (binding != null) {
                if (isPro) {
                    binding.buttonUpgradePro.setText(R.string.gerenciar_assinatura);
                    binding.textViewNomePlano.setText("Plano Premium");
                } else {
                    binding.buttonUpgradePro.setText(R.string.fazer_upgrade);
                    binding.textViewNomePlano.setText("Plano Básico");
                }
            }
        });
    }

    private void setupClickListeners() {
        NavController navController = NavHostFragment.findNavController(this);
        binding.btnSair.setOnClickListener(v -> viewModel.logout());
        binding.buttonUpgradePro.setOnClickListener(v ->
                navController.navigate(R.id.action_perfilFragment_to_assinaturaFragment)
        );
        binding.buttonTornarMentor.setOnClickListener(v ->
                navController.navigate(R.id.action_perfilFragment_to_canvasMentorFragment)
        );
        binding.btnEditarPerfil.setOnClickListener(v ->
                navController.navigate(R.id.action_perfilFragment_to_editarPerfilFragment)
        );
        binding.btnPrivacidade.setOnClickListener(v -> {
            navController.navigate(R.id.action_perfilFragment_to_privacidadeSegurancaFragment);
        });
        binding.btnAjuda.setOnClickListener(v -> {
            navController.navigate(R.id.action_perfilFragment_to_ajudaSuporteFragment);
        });
    }

    private void populateUi(User user) {
        binding.textViewNomePerfil.setText(TextUtils.isEmpty(user.getNome()) ? "Sem nome" : user.getNome());
        binding.textViewEmailPerfil.setText(TextUtils.isEmpty(user.getEmail()) ? "—" : user.getEmail());

        if (isAdded()) {
            Glide.with(this)
                    .load(user.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.imageViewPerfil);
        }
        if (user.isPremium()) {
            binding.textViewNomePlano.setText("Plano Premium");
            binding.textViewValidadePlano.setText("Válido até " + user.getValidadePlano());
            binding.chipPremium.setVisibility(View.VISIBLE);
        } else {
            binding.textViewNomePlano.setText("Plano Básico");
            binding.textViewValidadePlano.setText("");
            binding.chipPremium.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}