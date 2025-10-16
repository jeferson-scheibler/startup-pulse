package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.startuppulse.databinding.FragmentPreparacaoInvestidorBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreparacaoInvestidorFragment extends Fragment {

    private FragmentPreparacaoInvestidorBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Você precisará criar o layout 'fragment_preparacao_investidor.xml'
        // baseado no seu 'activity_preparacao_investidor.xml' antigo.
        binding = FragmentPreparacaoInvestidorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}