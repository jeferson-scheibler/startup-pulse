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
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.data.User;
import com.example.startuppulse.databinding.FragmentPerfilBinding; // Importe o binding
import com.example.startuppulse.ui.perfil.PerfilViewModel; // Importe o ViewModel

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PerfilFragment extends Fragment {

    private static final String TAG = "PerfilFragment";

    // 1. Declare o binding e o ViewModel
    private FragmentPerfilBinding binding;
    private PerfilViewModel viewModel;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 2. Infla o layout com View Binding
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 3. Inicializa o ViewModel e o NavController
        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);
        navController = Navigation.findNavController(view);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // Observa os dados do perfil
        viewModel.userProfile.observe(getViewLifecycleOwner(), user -> {
            if (user != null && binding != null) {
                populateUi(user);
            }
        });

        // Observa o evento de logout para navegar para a tela de login
        viewModel.navigateToLogin.observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate) {
                irParaLogin();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnSair.setOnClickListener(v -> viewModel.logout());

        binding.btnGerenciarAssinatura.setOnClickListener(v -> {
            // CORRIGIDO: Usa o NavController para navegar para a tela de assinatura
            // Você precisará adicionar esta ação ao seu nav_graph
            navController.navigate(R.id.action_perfilFragment_to_assinaturaFragment);
        });
    }

    private void populateUi(User user) {
        // Popula a UI usando o objeto User e o binding
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

        // Lógica do Plano
        if (user.isPremium()) {
            binding.textViewNomePlano.setText("Plano Premium");
            binding.textViewValidadePlano.setText("Válido até " + user.getValidadePlano());
            binding.chipPremium.setVisibility(View.VISIBLE);
        } else {
            binding.textViewNomePlano.setText("Plano Básico");
            binding.textViewValidadePlano.setText("");
            binding.chipPremium.setVisibility(View.GONE);
        }

        // Lógica dos Stats
        binding.statPublicadas.setText(String.valueOf(user.getPublicadasCount()));
        binding.statSeguindo.setText(String.valueOf(user.getSeguindoCount()));
        binding.statDias.setText(String.valueOf(user.getDiasDeConta()));

        // Lógica do Chip "Membro desde"
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

    private void irParaLogin() {
        // CORRIGIDO: Usa o NavController para voltar ao início do gráfico de navegação (login)
        // Você precisará de uma ação global no seu nav_graph principal para isso
        if (isAdded()) {
            navController.navigate(R.id.action_global_return_to_login);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Previne memory leaks
    }
}