package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.startuppulse.databinding.FragmentIdeiasHostBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IdeiasHostFragment extends Fragment {

    private FragmentIdeiasHostBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiasHostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.viewPager.setAdapter(new IdeiasPagerAdapter(this));

        // Conecta os botões ao ViewPager
        binding.toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_publicadas) {
                    binding.viewPager.setCurrentItem(0, true);
                } else if (checkedId == R.id.btn_rascunhos) {
                    binding.viewPager.setCurrentItem(1, true);
                }
            }
        });

        // Conecta o deslizar do ViewPager aos botões
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    binding.toggleButtonGroup.check(R.id.btn_publicadas);
                } else {
                    binding.toggleButtonGroup.check(R.id.btn_rascunhos);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // O Adapter agora recebe um Fragment em vez de uma FragmentActivity
    private static class IdeiasPagerAdapter extends FragmentStateAdapter {
        public IdeiasPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new IdeiasFragment();
            } else {
                return new MeusRascunhosFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}