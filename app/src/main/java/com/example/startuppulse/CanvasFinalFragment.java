package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

// Imports para a nova arquitetura
import com.example.startuppulse.databinding.FragmentCanvasFinalBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasFinalFragment extends Fragment {

    private FragmentCanvasFinalBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;

    /**
     * O método newInstance foi removido.
     * O CanvasPagerAdapter agora pode simplesmente chamar 'new CanvasFinalFragment()'.
     */
    public CanvasFinalFragment() {
        // Construtor público vazio é obrigatório.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Usa View Binding para inflar o layout
        binding = FragmentCanvasFinalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtém a instância do ViewModel compartilhado
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);

        // Configura os observers e listeners
        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // Observa o objeto Ideia para atualizar a UI, se necessário.
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia != null) {
                // Exemplo: Atualizar um texto com o nome da ideia
                // binding.textViewNomeIdeiaFinal.setText(ideia.getNome());
            }
        });

        // Observa o estado de 'somente leitura' para habilitar/desabilitar botões.
        sharedViewModel.isReadOnly.observe(getViewLifecycleOwner(), isReadOnly -> {
            if (isReadOnly != null) {
                // Exemplo: Habilitar ou desabilitar o botão de publicar
                // binding.btnPublicar.setEnabled(!isReadOnly);
            }
        });
    }

    private void setupClickListeners() {
        // Exemplo de como um botão de "Publicar" funcionaria nesta tela.
        // A lógica de negócio é delegada para o ViewModel.
        /*
        binding.btnPublicar.setOnClickListener(v -> {
            // O fragment não precisa saber dos detalhes, apenas notifica o ViewModel.
            sharedViewModel.iniciarProcessoDePublicacao(null);
        });
        */
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Previne vazamentos de memória com View Binding
    }
}