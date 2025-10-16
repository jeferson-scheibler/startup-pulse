// app/src/main/java/com/example/startuppulse/ui/main/MainHostFragment.java

package com.example.startuppulse.ui.main;

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

import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentMainHostBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainHostFragment extends Fragment {

    private FragmentMainHostBinding binding;
    private NavController navController;

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

        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.fragment_container);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            Log.e("MainHostFragment", "NavController não encontrado!");
            return;
        }

        verificarSeUsuarioEhMentor();
        setupButtonClickListeners();
        setupFabListeners(); // Vamos corrigir este método

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            handleFabVisibility(destination.getId());
        });

        // Estado inicial
        updateButtonState(binding.navButtonIdeias);
    }

    private void handleFabVisibility(int destinationId) {
        if (destinationId == R.id.ideiasFragment) {
            binding.fabAddIdea.show();
            binding.fabAddMentor.hide();
        } else if (destinationId == R.id.mentoresFragment) {
            binding.fabAddIdea.hide();
            // Mostra o FAB "Seja um Mentor" apenas se o usuário AINDA NÃO for um mentor
            if (!isMentor) {
                binding.fabAddMentor.show();
            } else {
                binding.fabAddMentor.hide();
            }
        } else {
            // Esconde ambos os FABs em todas as outras telas (Perfil, Investidores, etc.)
            binding.fabAddIdea.hide();
            binding.fabAddMentor.hide();
        }
    }


    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.bottomNavContainer.getLayoutParams();
            params.bottomMargin = systemBars.bottom + (int) (16 * getResources().getDisplayMetrics().density);
            binding.bottomNavContainer.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupButtonClickListeners() {
        if (navController == null) return;

        binding.navButtonIdeias.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.ideiasFragment) {
                navController.navigate(R.id.ideiasFragment);
            }
            updateButtonState(v);
        });

        binding.navButtonMentores.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.mentoresFragment) {
                navController.navigate(R.id.mentoresFragment);
            }
            updateButtonState(v);
        });

        binding.navButtonInvestidores.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.investidoresFragment) {
                navController.navigate(R.id.investidoresFragment);
            }
            updateButtonState(v);
        });

        binding.navButtonPerfil.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.perfilFragment) {
                navController.navigate(R.id.perfilFragment);
            }
            updateButtonState(v);
        });
    }

    private void setupFabListeners() {
        binding.fabAddIdea.setOnClickListener(v -> {
            // Navega para a criação de uma nova ideia
            navController.navigate(R.id.action_global_to_canvasIdeiaFragment);
        });

        // ===== AQUI ESTÁ A CORREÇÃO =====
        binding.fabAddMentor.setOnClickListener(v -> {
            // A navegação agora é feita pelo NavController para o CanvasMentorFragment.
            // Note que não há uma "action" global definida, então navegamos diretamente para o ID do fragmento.
            // Isso funciona porque estamos no mesmo grafo de navegação (main_nav_graph).
            navController.navigate(R.id.canvasMentorFragment);
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
                .addOnSuccessListener(documentSnapshot -> {
                    isMentor = documentSnapshot.exists();
                    // Após verificar, atualiza a visibilidade do FAB caso a tela de mentores já esteja visível
                    if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == R.id.mentoresFragment) {
                        handleFabVisibility(R.id.mentoresFragment);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}