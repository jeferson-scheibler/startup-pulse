package com.example.startuppulse;

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

        viewModel.isPro.observe(getViewLifecycleOwner(), isPro -> {
            if (binding != null) {
                if (isPro) {
                    binding.btnGerenciarAssinatura.setText(R.string.gerenciar_assinatura);
                } else {
                    binding.btnGerenciarAssinatura.setText(R.string.fazer_upgrade);
                }
            }
        });
    }

    private void setupClickListeners() {
        NavController navController = NavHostFragment.findNavController(this);

        binding.btnSair.setOnClickListener(v -> viewModel.logout());

        binding.btnGerenciarAssinatura.setOnClickListener(v ->
                navController.navigate(R.id.action_perfilFragment_to_assinaturaFragment)
        );

        binding.buttonTornarMentor.setOnClickListener(v ->
                navController.navigate(R.id.action_perfilFragment_to_canvasMentorFragment)
        );
        binding.btnEditarPerfil.setOnClickListener(v -> {
            // TODO: Navegar para a tela de edição de perfil
            Toast.makeText(getContext(), "Editar Perfil Clicado", Toast.LENGTH_SHORT).show();
        });

        binding.btnPrivacidade.setOnClickListener(v -> {
            // TODO: Navegar para a tela de privacidade
            Toast.makeText(getContext(), "Privacidade Clicado", Toast.LENGTH_SHORT).show();
        });

        binding.btnAjuda.setOnClickListener(v -> {
            // TODO: Navegar para a tela de ajuda/suporte
            Toast.makeText(getContext(), "Ajuda e Suporte Clicado", Toast.LENGTH_SHORT).show();
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

        binding.statPublicadas.setText(String.valueOf(user.getPublicadasCount()));
        binding.statSeguindo.setText(String.valueOf(user.getSeguindoCount()));
        binding.statDias.setText(String.valueOf(user.getDiasDeConta()));

        long dias = user.getDiasDeConta();
        String textoChip;
        if (dias < 30) {
            textoChip = String.format(Locale.getDefault(), "Membro há %d dias", dias);
        } else {
            long meses = Math.max(1, dias / 30);
            textoChip = String.format(Locale.getDefault(), "Membro há %d %s",
                    meses, (meses == 1 ? "mês" : "meses"));
        }
        binding.chipMemberSince.setText(textoChip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}