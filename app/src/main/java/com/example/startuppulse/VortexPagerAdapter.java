package com.example.startuppulse;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.startuppulse.ui.propulsor.PropulsorFragment;
import com.example.startuppulse.ui.vortex.VortexFragment;

public class VortexPagerAdapter extends FragmentStateAdapter {

    public VortexPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Posição 0: Vórtex (Novo)
                return new VortexFragment();

            // --- INÍCIO DA ADIÇÃO ---
            case 1:
                // Posição 1: Feed Global (O seu fragmento antigo)
                return new IdeiasFragment(); //
            // --- FIM DA ADIÇÃO ---

            case 2:
                // Posição 2: Rascunhos (Existente)
                return new MeusRascunhosFragment(); //
            case 3:
                // Posição 3: Propulsor (Novo)
                return new PropulsorFragment();
            default:
                return new VortexFragment(); // Fallback
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}