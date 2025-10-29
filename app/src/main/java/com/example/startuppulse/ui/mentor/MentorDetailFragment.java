package com.example.startuppulse.ui.mentor;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User; // ADICIONADO
import com.example.startuppulse.common.Result;
import com.example.startuppulse.databinding.FragmentMentorDetailBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MentorDetailFragment extends Fragment {

    private FragmentMentorDetailBinding binding;
    private MentorDetailViewModel viewModel;
    private NavController navController;

    // ADICIONADO: Para guardar os dados separados
    private User mUser;
    private Mentor mMentor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMentorDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MentorDetailViewModel.class);
        navController = NavHostFragment.findNavController(this);

        binding.buttonLinkedin.setVisibility(View.GONE);

        setupToolbar();
        setupObservers();
        setupClickListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> navController.navigateUp());
    }

    private void setupObservers() {

        // MUDADO: Observador 1 - Dados do USER
        viewModel.userDetails.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.errorView.setVisibility(View.GONE);
            } else if (result instanceof Result.Success) {
                binding.progressBar.setVisibility(View.GONE);
                this.mUser = ((Result.Success<User>) result).data;
                populateUserUI(mUser); // Popula a parte do User
            } else if (result instanceof Result.Error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.errorView.setVisibility(View.VISIBLE);
                String errorMessage = ((Result.Error<User>) result).error.getMessage();
                Toast.makeText(getContext(), "Erro: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // MUDADO: Observador 2 - Dados do MENTOR
        viewModel.mentorDetails.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                // O loading já é tratado pelo observador do User
            } else if (result instanceof Result.Success) {
                this.mMentor = ((Result.Success<Mentor>) result).data;
                populateMentorUI(mMentor); // Popula a parte do Mentor
            } else if (result instanceof Result.Error) {
                // Erro ao carregar o mentor (bio, etc.) é menos crítico que o User
                Toast.makeText(getContext(), "Erro ao carregar bio do mentor.", Toast.LENGTH_SHORT).show();
            }
        });

        // Observador para verificar se o usuário é o dono (sem mudança)
        viewModel.isProfileOwner.observe(getViewLifecycleOwner(), isOwner -> {
            if (isOwner) {
                binding.fabEditMentor.show();
                binding.btnRequestMentorship.setVisibility(View.GONE);
            } else {
                binding.fabEditMentor.hide();
                binding.btnRequestMentorship.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupClickListeners() {
        binding.fabEditMentor.setOnClickListener(v -> {
            navController.navigate(R.id.action_mentorDetailFragment_to_editProfileFragment);
        });

        binding.btnRequestMentorship.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Funcionalidade ainda não implementada.", Toast.LENGTH_SHORT).show();
        });

        // O listener do LinkedIn foi movido para um método helper (setupLinkedIn)
    }

    /**
     * ADICIONADO: Popula a UI com dados do objeto User (nome, foto, linkedin, areas)
     */
    private void populateUserUI(User user) {
        if (user == null || !isAdded()) return;

        binding.mentorName.setText(user.getNome());
        binding.mentorHeadline.setText(user.getProfissao());

        Glide.with(this)
                .load(user.getFotoUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.mentorAvatar);

        setupChipGroup(user.getAreasDeInteresse());
        setupLinkedIn(user.getLinkedinUrl());
    }

    /**
     * ADICIONADO: Popula a UI com dados do objeto Mentor (bio, banner, etc.)
     */
    private void populateMentorUI(Mentor mentor) {
        if (mentor == null || !isAdded()) return;

        binding.mentorBio.setText(mentor.getBio());

        Glide.with(this)
                .load(mentor.getBannerUrl())
                .placeholder(R.drawable.fundo_mentores)
                .fallback(R.drawable.fundo_mentores)
                .error(R.drawable.fundo_mentores)
                .into(binding.mentorBannerImage);

        // ADICIONADO: Mostrar o selo de verificado
        binding.iconVerificadoDetalhe.setVisibility(mentor.isVerified() ? View.VISIBLE : View.GONE);
    }

    /**
     * ADICIONADO: Método helper para configurar o botão do LinkedIn
     */
    private void setupLinkedIn(String url) {
        if (url != null && !url.trim().isEmpty()) {
            binding.buttonLinkedin.setVisibility(View.VISIBLE);
            binding.buttonLinkedin.setOnClickListener(v -> abrirLink(url));
        } else {
            binding.buttonLinkedin.setVisibility(View.GONE);
        }
    }

    private void abrirLink(String url) {
        try {
            // Adiciona "https://" se estiver faltando, para o Intent funcionar
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

        } catch (Exception e) {
            // Tratar erro (ex: URL mal formada ou nenhum app de navegador)
            Toast.makeText(getContext(), "Não foi possível abrir o link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupChipGroup(List<String> areas) {
        if (!isAdded() || binding == null || getContext() == null) return;

        ChipGroup chipGroup = binding.chipGroupAreas;
        TextView label = binding.labelEspecialidades;

        if (areas == null || areas.isEmpty()) {
            label.setVisibility(View.GONE);
            chipGroup.setVisibility(View.GONE);
            return;
        }

        label.setVisibility(View.VISIBLE);
        chipGroup.setVisibility(View.VISIBLE);
        chipGroup.removeAllViews();

        Context chipContext = new ContextThemeWrapper(getContext(), R.style.Widget_StartupPulse_Chip_Area_Small);
        ColorStateList iconTint = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorPrimary));

        for (String area : areas) {
            Chip chip = new Chip(chipContext);
            chip.setText(area);

            // Seu estilo 'Widget.StartupPulse.Chip.Area.Small' já deve fazer isso,
            // mas se não fizer, estas linhas garantem:
            chip.setChipIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_bolt));
            chip.setChipIconTint(iconTint);

            // CORRIGIDO: Removida linha duplicada de setChipIcon

            chipGroup.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Essencial para evitar vazamentos de memória com View Binding
    }
}