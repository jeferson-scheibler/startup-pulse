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
// NOVO IMPORT: Para encontrar o NavController principal
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.data.User;
import com.example.startuppulse.databinding.FragmentPerfilBinding;
import com.example.startuppulse.ui.perfil.PerfilViewModel;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private PerfilViewModel viewModel;
    // Teremos dois NavControllers: um para navegação interna (assinatura)
    private NavController localNavController;
    // e um para navegação global (logout)

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

        localNavController = Navigation.findNavController(view);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.userProfile.observe(getViewLifecycleOwner(), user -> {
            if (user != null && binding != null) {
                populateUi(user);
            }
        });
    }

    private void setupClickListeners() {
        binding.btnSair.setOnClickListener(v -> viewModel.logout());

        binding.btnGerenciarAssinatura.setOnClickListener(v -> {
            // Usa o localNavController para a ação dentro do mesmo gráfico
            localNavController.navigate(R.id.action_perfilFragment_to_assinaturaFragment);
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
