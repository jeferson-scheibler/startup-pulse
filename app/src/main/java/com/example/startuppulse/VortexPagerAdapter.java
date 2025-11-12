package com.example.startuppulse;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.startuppulse.ui.propulsor.PropulsorFragment;
import com.example.startuppulse.ui.vortex.VortexFragment;

public class VortexPagerAdapter extends FragmentStateAdapter {

    public VortexPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Posição 0: Vórtex (Nosso novo placeholder)
                return new VortexFragment();
            case 1:
                // Posição 1: Rascunhos (O fragmento existente)
                return new MeusRascunhosFragment();
            case 2:
                // Posição 2: Propulsor (Nosso novo placeholder)
                return new PropulsorFragment();
            default:
                return new VortexFragment();
        }
    }

    @Override
    public int getItemCount() {
        // Agora temos 3 abas
        return 3;
    }
}