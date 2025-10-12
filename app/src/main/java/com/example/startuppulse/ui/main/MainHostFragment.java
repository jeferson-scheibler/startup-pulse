package com.example.startuppulse.ui.main;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.startuppulse.CanvasIdeiaActivity;
import com.example.startuppulse.CanvasMentorActivity;
import com.example.startuppulse.R;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.Mentor;
import com.example.startuppulse.databinding.FragmentMainHostBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainHostFragment extends Fragment {

    private FragmentMainHostBinding binding;
    private NavController navController; // Mantém a referência

    private boolean isMentor = false;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainHostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupWindowInsets();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // CORRIGIDO: Forma mais robusta de obter o NavController.
        // O NavHostFragment é o próprio container que definimos no XML.
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.fragment_container);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            // Se o NavController não for encontrado, logamos um erro para saber o que aconteceu.
            Log.e("MainHostFragment", "NavController não encontrado!");
            return; // Impede que o resto do código execute e cause um crash.
        }

        verificarSeUsuarioEhMentor();
        setupButtonClickListeners();
        setupFabListeners();

        // Estado inicial
        updateButtonState(binding.navButtonIdeias);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            // Pega os insets (espaços) das barras do sistema (topo e rodapé)
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Aplica o padding ao contêiner raiz do seu fragment
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            // Ajusta a margem inferior da sua barra de navegação customizada
            // para que ela não fique colada no fundo da tela.
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.bottomNavContainer.getLayoutParams();
            params.bottomMargin = systemBars.bottom + (int) (16 * getResources().getDisplayMetrics().density); // 16dp de margem
            binding.bottomNavContainer.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupButtonClickListeners() {
        // Adiciona uma verificação para garantir que o navController não é nulo antes de usá-lo.
        if (navController == null) return;

        binding.navButtonIdeias.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.ideiasFragment) {
                navController.navigate(R.id.ideiasFragment);
            }
            updateButtonState(v);
            binding.fabAddIdea.setVisibility(View.VISIBLE);
            binding.fabAddMentor.setVisibility(View.GONE);
        });

        binding.navButtonMentores.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.mentoresFragment) {
                navController.navigate(R.id.mentoresFragment);
            }
            updateButtonState(v);
            binding.fabAddIdea.setVisibility(View.GONE);
            binding.fabAddMentor.setVisibility(isMentor ? View.GONE : View.VISIBLE);
        });

        binding.navButtonInvestidores.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.investidoresFragment) {
                navController.navigate(R.id.investidoresFragment);
            }
            updateButtonState(v);
            binding.fabAddIdea.setVisibility(View.GONE);
            binding.fabAddMentor.setVisibility(View.GONE);
        });

        binding.navButtonPerfil.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.perfilFragment) {
                navController.navigate(R.id.perfilFragment);
            }
            updateButtonState(v);
            binding.fabAddIdea.setVisibility(View.GONE);
            binding.fabAddMentor.setVisibility(View.GONE);
        });
    }

    // (O resto da sua classe: setupFabListeners, updateButtonState, etc., continua igual)
    // ...
    private void setupFabListeners() {
        binding.fabAddIdea.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), CanvasIdeiaActivity.class);
            Ideia novaIdeia = new Ideia();
            if (currentUser != null) {
                novaIdeia.setOwnerId(currentUser.getUid());
                novaIdeia.setAutorNome(currentUser.getDisplayName());
            }
            intent.putExtra("ideia", novaIdeia);
            startActivity(intent);
        });

        binding.fabAddMentor.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), CanvasMentorActivity.class);
            Mentor novoMentor = new Mentor();
            if (currentUser != null) {
                novoMentor.setId(currentUser.getUid());
                novoMentor.setNome(currentUser.getDisplayName());
            }
            intent.putExtra("mentor", novoMentor);
            startActivity(intent);
        });
    }

    private void updateButtonState(View selectedButton) {
        binding.navButtonIdeias.setSelected(false);
        binding.navButtonMentores.setSelected(false);
        binding.navButtonInvestidores.setSelected(false);
        binding.navButtonPerfil.setSelected(false);

        binding.navTextIdeias.setVisibility(View.GONE);
        binding.navTextMentores.setVisibility(View.GONE);
        binding.navTextInvestidores.setVisibility(View.GONE);
        binding.navTextPerfil.setVisibility(View.GONE);

        selectedButton.setSelected(true);

        int selectedId = selectedButton.getId();
        if (selectedId == R.id.nav_button_ideias) {
            binding.navTextIdeias.setVisibility(View.VISIBLE);
        } else if (selectedId == R.id.nav_button_mentores) {
            binding.navTextMentores.setVisibility(View.VISIBLE);
        } else if (selectedId == R.id.nav_button_investidores) {
            binding.navTextInvestidores.setVisibility(View.VISIBLE);
        } else if (selectedId == R.id.nav_button_perfil) {
            binding.navTextPerfil.setVisibility(View.VISIBLE);
        }

        tintNavIcons();
    }

    private void tintNavIcons() {
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        int normalColor = ContextCompat.getColor(requireContext(), R.color.strokeSoft);

        binding.navIconIdeias.setImageTintList(ColorStateList.valueOf(binding.navButtonIdeias.isSelected() ? selectedColor : normalColor));
        binding.navIconMentores.setImageTintList(ColorStateList.valueOf(binding.navButtonMentores.isSelected() ? selectedColor : normalColor));
        binding.navIconInvestidores.setImageTintList(ColorStateList.valueOf(binding.navButtonInvestidores.isSelected() ? selectedColor : normalColor));
        binding.navIconPerfil.setImageTintList(ColorStateList.valueOf(binding.navButtonPerfil.isSelected() ? selectedColor : normalColor));
    }

    private void verificarSeUsuarioEhMentor() {
        if (currentUser == null) return;
        FirebaseFirestore.getInstance().collection("mentores").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> isMentor = documentSnapshot.exists());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}