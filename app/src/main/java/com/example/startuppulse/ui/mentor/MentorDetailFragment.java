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
// Removido: import android.widget.Button; (Usaremos o binding)
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

        // Acessa o botão diretamente pelo binding
        binding.buttonLinkedin.setVisibility(View.GONE);

        setupToolbar();
        setupObservers();
        setupClickListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> navController.navigateUp());
    }

    private void setupObservers() {
        // Observador principal para os detalhes do mentor
        viewModel.mentorDetails.observe(getViewLifecycleOwner(), result -> {
            // Esconde todos os componentes antes de tratar o estado
            binding.progressBar.setVisibility(View.GONE);
            binding.errorView.setVisibility(View.GONE);
            if (result instanceof Result.Loading) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else if (result instanceof Result.Success) {
                Mentor mentor = ((Result.Success<Mentor>) result).data;
                populateUI(mentor);
            } else if (result instanceof Result.Error) {
                binding.errorView.setVisibility(View.VISIBLE);
                String errorMessage = ((Result.Error<Mentor>) result).error.getMessage();
                Toast.makeText(getContext(), "Erro: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observador para verificar se o usuário é o dono
        viewModel.isProfileOwner.observe(getViewLifecycleOwner(), isOwner -> {
            if (isOwner) {
                // Se for o dono, mostra o FAB de editar
                binding.fabEditMentor.show();
                // E esconde o botão "Solicitar Mentoria"
                binding.btnRequestMentorship.setVisibility(View.GONE);
            } else {
                // Se NÃO for o dono, esconde o FAB
                binding.fabEditMentor.hide();
                // E mostra o botão "Solicitar Mentoria"
                binding.btnRequestMentorship.setVisibility(View.VISIBLE);
            }
        });

        // REMOVIDO: O bloco 'viewModel.getMentorUser().observe' foi removido
        // pois era contraditório. A lógica do LinkedIn foi movida
        // para dentro do 'populateUI', usando o objeto Mentor.
    }

    private void setupClickListeners() {
        binding.fabEditMentor.setOnClickListener(v -> {
            navController.navigate(R.id.action_mentorDetailFragment_to_editProfileFragment);
        });

        binding.btnRequestMentorship.setOnClickListener(v -> {
            // Lógica para solicitar mentoria...
            Toast.makeText(getContext(), "Funcionalidade ainda não implementada.", Toast.LENGTH_SHORT).show();
        });
        binding.buttonLinkedin.setOnClickListener(v -> {
            String url = (String) v.getTag();
            if (url != null && !url.isEmpty()) {
                abrirLink(url);
            }
        });
    }

    // Removido: handleMentorResult() não era necessário,
    // a lógica foi movida para dentro do observador.

    private void populateUI(Mentor mentor) {
        if (mentor == null) return;

        binding.mentorName.setText(mentor.getName());
        binding.mentorHeadline.setText(mentor.getHeadline());
        binding.mentorBio.setText(mentor.getBio());

        // Lógica do LinkedIn, lendo do objeto Mentor
        String url = mentor.getLinkedinUrl(); // Pega o link do objeto
        if (url != null && !url.trim().isEmpty()) {
            binding.buttonLinkedin.setVisibility(View.VISIBLE); // Mostra o botão
            binding.buttonLinkedin.setTag(url);
        } else {
            binding.buttonLinkedin.setVisibility(View.GONE); // Esconde o botão
        }

        Glide.with(this)
                .load(mentor.getFotoUrl())
                .placeholder(R.drawable.ic_person) // Placeholder
                .error(R.drawable.ic_person) // Imagem de erro
                .into(binding.mentorAvatar);

         Glide.with(this)
                .load(mentor.getBannerUrl())
                .placeholder(R.drawable.fundo_mentores)
                .fallback(R.drawable.fundo_mentores)
                .error(R.drawable.fundo_mentores)
                .into(binding.mentorBannerImage);

        setupChipGroup(mentor.getAreas());
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