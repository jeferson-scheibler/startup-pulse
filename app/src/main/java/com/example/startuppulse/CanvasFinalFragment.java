package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.startuppulse.databinding.FragmentCanvasFinalBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento final do Canvas, exibe uma animação e mensagem de conclusão.
 * Esta tela é puramente visual e não contém lógica de botões. A ação de publicar
 * é gerenciada pelo fragmento pai (CanvasIdeiaFragment).
 */
@AndroidEntryPoint
public class CanvasFinalFragment extends Fragment {

    private FragmentCanvasFinalBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;

    public CanvasFinalFragment() {
        // Construtor público vazio é obrigatório.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasFinalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null && binding != null) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}