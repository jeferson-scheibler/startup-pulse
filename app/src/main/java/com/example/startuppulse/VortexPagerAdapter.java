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
                return new VortexFragment();
            case 1:
                return new MeusRascunhosFragment(); //
            case 2:
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