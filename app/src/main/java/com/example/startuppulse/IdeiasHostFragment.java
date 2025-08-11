package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.startuppulse.databinding.FragmentIdeiasHostBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class IdeiasHostFragment extends Fragment {

    private FragmentIdeiasHostBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiasHostBinding.inflate(inflater, container, false);

        // ViewPager2 + Adapter
        binding.viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int position) {
                if (position == 0) return new IdeiasFragment();         // Publicadas (seu fragment atual)
                else return new MeusRascunhosFragment();                 // Nova aba
            }
            @Override public int getItemCount() { return 2; }
        });

        // Tab titles
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, pos) -> tab.setText(pos == 0 ? "Publicadas" : "Meus rascunhos")
        ).attach();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
