package com.example.startuppulse.ui.propulsor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.databinding.FragmentPropulsorBinding;
import com.example.startuppulse.util.Event;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint // Essencial para Hilt e ViewModel
public class PropulsorFragment extends Fragment {

    private PropulsorViewModel viewModel;
    private FragmentPropulsorBinding binding;
    private ChatAdapter chatAdapter;

    public PropulsorFragment() {
        // Construtor público vazio obrigatório
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla usando ViewBinding
        binding = FragmentPropulsorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar o ViewModel
        viewModel = new ViewModelProvider(this).get(PropulsorViewModel.class);

        // 2. Configurar o RecyclerView
        setupRecyclerView();

        // 3. Configurar Observadores
        setupObservers();

        // 4. Configurar Listeners de Clique
        setupClickListeners();
        setupWhatsAppKeyboardBehavior(binding.getRoot());
    }

    private void setupWhatsAppKeyboardBehavior(View root) {

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {

            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int imeHeight = ime.bottom;
            int navHeight = sys.bottom;

            // Aqui está o segredo do WhatsApp:
            // Usa o MAIOR entre o teclado e a navbar.
            int effectiveHeight = Math.max(imeHeight, navHeight);

            boolean keyboardVisible = imeHeight > navHeight + dp(20);

            int translation = keyboardVisible ? -imeHeight : -navHeight;

            binding.inputLayout.setTranslationY(translation);
            binding.botoesAcaoLayout.setTranslationY(translation);

            binding.chatRecyclerView.setPadding(
                    0, 0, 0,
                    keyboardVisible ? imeHeight + dp(24) : navHeight + dp(24)
            );

            if (keyboardVisible && chatAdapter.getItemCount() > 0) {
                binding.chatRecyclerView.postDelayed(() ->
                                binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1),
                        80
                );
            }

            return insets;
        });
    }


    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Faz o chat começar de baixo
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupObservers() {
        // Observa o histórico do chat
        viewModel.getChatHistory().observe(getViewLifecycleOwner(), messages -> {
            chatAdapter.submitList(new ArrayList<>(messages)); // Envia nova lista
            // Auto-scroll para a nova mensagem
            binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount());
        });

        // Observa o estado de carregamento (Loading)
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.sendButton.setEnabled(!isLoading);
            binding.inputEditText.setEnabled(!isLoading);
        });

        // Observa os botões de ação
        viewModel.getShowActionButtons().observe(getViewLifecycleOwner(), show -> {
            binding.botoesAcaoLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        // Observa eventos de Toast
        viewModel.getToastEvent().observe(getViewLifecycleOwner(), new Event.EventObserver<>(message -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }));
    }

    private void setupClickListeners() {
        // Botão de Enviar
        binding.sendButton.setOnClickListener(v -> {
            String inputText = binding.inputEditText.getText().toString();
            viewModel.sendSparkToIA(inputText);
            binding.inputEditText.setText(""); // Limpa o campo
        });

        // Botão Salvar Rascunho
        binding.btnSalvarRascunho.setOnClickListener(v -> {
            viewModel.saveAsDraft();
        });

        // Botão Lançar ao Vórtex
        binding.btnLancarVortex.setOnClickListener(v -> {
            viewModel.launchToVortex();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Limpar o binding
    }
}