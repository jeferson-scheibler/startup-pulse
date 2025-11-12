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

        // 1. Configurar o Adapter
        adapter = new VortexPagerAdapter(requireActivity());
        binding.viewPager.setAdapter(adapter);

        // 2. Conectar o ViewPager aos cliques dos botões
        binding.toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_vortex) {
                    binding.viewPager.setCurrentItem(0);
                } else if (checkedId == R.id.btn_rascunhos) {
                    binding.viewPager.setCurrentItem(1);
                } else if (checkedId == R.id.btn_propulsor) {
                    binding.viewPager.setCurrentItem(2);
                }
            }
        });

        // 3. Conectar os cliques do ViewPager aos botões (swipe)
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        binding.toggleButtonGroup.check(R.id.btn_vortex);
                        break;
                    case 1:
                        binding.toggleButtonGroup.check(R.id.btn_rascunhos);
                        break;
                    case 2:
                        binding.toggleButtonGroup.check(R.id.btn_propulsor);
                        break;
                }
            }
        });

        // Garantir que o estado inicial esteja correto
        binding.toggleButtonGroup.check(R.id.btn_vortex);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Evitar memory leaks
    }
}