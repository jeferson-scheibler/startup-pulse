package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.startuppulse.databinding.FragmentIdeiasHostBinding;
import com.example.startuppulse.ui.main.MainHostFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IdeiasHostFragment extends Fragment {

    private FragmentIdeiasHostBinding binding; // O binding gerado
    private VortexPagerAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIdeiasHostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new VortexPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);


        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    // Isto é chamado para CADA aba (4 vezes)
                    // Aqui definimos os ÍCONES e o texto inicial
                    switch (position) {
                        case 0:
                            tab.setIcon(R.drawable.ic_vortex); // (O seu novo ícone)
                            tab.setText("Vórtex"); // O primeiro é selecionado, então mostramos o texto
                            break;
                        case 1:
                            tab.setIcon(R.drawable.ic_feed); // (O seu novo ícone)
                            // tab.setText("Feed"); // (Deixe em branco, por defeito)
                            break;
                        case 2:
                            tab.setIcon(R.drawable.ic_lightbulb_outline); //
                            // tab.setText("Rascunhos");
                            break;
                        case 3:
                            tab.setIcon(R.drawable.ic_rocket_launch); //
                            // tab.setText("Propulsor");
                            break;
                    }
                }
        ).attach();

        // 3. Conectar os cliques do ViewPager aos botões (swipe)
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setBounds(0, 0, dpToPx(18), dpToPx(18));
                }
                // QUANDO UMA ABA É SELECIONADA
                switch (tab.getPosition()) {
                    case 0:
                        tab.setText("Vórtex");
                        break;
                    case 1:
                        tab.setText("Feed");
                        break;
                    case 2:
                        tab.setText("Rascunhos");
                        break;
                    case 3:
                        tab.setText("Propulsor");
                        break;
                }

                View tabView = tab.view;
                tabView.animate().scaleX(1.10f).scaleY(1.10f).setDuration(150).start();

                // Lógica do FAB (que já tínhamos)
                boolean shouldShowFab = (tab.getPosition() == 1 || tab.getPosition() == 2);
                if (getParentFragment() != null && getParentFragment().getParentFragment() instanceof MainHostFragment) {
                    ((MainHostFragment) getParentFragment().getParentFragment()).setFabAddIdeaVisibility(shouldShowFab);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

                // Reduz ícone (18dp)
                if (tab.getIcon() != null) {
                    tab.getIcon().setBounds(0, 0, dpToPx(12), dpToPx(12));
                }

                // Remove texto da aba não selecionada
                tab.setText(null);

                // Voltar ao tamanho normal
                View tabView = tab.view;
                tabView.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // (Opcional) Pode ser usado para "scroll to top"
            }
        });

        if (getParentFragment() != null && getParentFragment().getParentFragment() instanceof MainHostFragment) {
            // A posição 0 (Vórtex) não tem FAB
            ((MainHostFragment) getParentFragment().getParentFragment()).setFabAddIdeaVisibility(false);
        }
    }
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }


    @Override
    public void onDestroyView() {
        if (getParentFragment() != null && getParentFragment().getParentFragment() instanceof MainHostFragment) {
            ((MainHostFragment) getParentFragment().getParentFragment()).setFabAddIdeaVisibility(false);
        }
        super.onDestroyView();
        binding = null;
    }
}