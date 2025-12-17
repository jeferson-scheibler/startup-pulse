package com.example.startuppulse.ui.propulsor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.databinding.FragmentPropulsorBinding;
import com.example.startuppulse.util.Event;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint // Essencial para Hilt e ViewModel
public class PropulsorFragment extends Fragment {

    private PropulsorViewModel viewModel;
    private FragmentPropulsorBinding binding;
    private ChatAdapter chatAdapter;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private static final String TAG_PROPULSOR = "PropulsorFragment";

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean fineGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineGranted != null && fineGranted) {
                        // Permissão exata dada, buscar localização
                        fetchLocationAndLaunch();
                    } else if (coarseGranted != null && coarseGranted) {
                        // Permissão aproximada dada, buscar localização
                        fetchLocationAndLaunch();
                    } else {
                        // Permissão negada
                        Toast.makeText(getContext(), "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
                    }
                });

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
            checkPermissionAndLaunchSpark();
        });
    }

    /**
     * 1. Ponto de entrada: Verifica se a permissão já foi dada.
     */
    private void checkPermissionAndLaunchSpark() {
        //
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            // Permissão já dada. Buscar localização.
            fetchLocationAndLaunch();
        } else {
            // Permissão não dada. Pedir.
            requestLocationPermission();
        }
    }

    /**
     * 2. Pede as permissões de localização ao utilizador.
     */
    private void requestLocationPermission() {
        // O AndroidManifest tem FINE e COARSE,
        // então pedimos ambas.
        locationPermissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    /**
     * 3. Busca a última localização conhecida e (se encontrada) chama o ViewModel.
     */
    @SuppressWarnings("MissingPermission") // Já verificámos a permissão no 'checkPermission...'
    private void fetchLocationAndLaunch() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> { // 'location' é do tipo android.location.Location
                    if (location != null) {
                        // SUCESSO! Temos a localização.
                        // Agora getLatitude() e getLongitude() serão encontrados.
                        Log.d(TAG_PROPULSOR, "Localização obtida: " + location.getLatitude() + ", " + location.getLongitude());
                        viewModel.launchToVortex(location.getLatitude(), location.getLongitude());
                    } else {
                        // Erro comum: Localização do telemóvel está desligada.
                        Log.w(TAG_PROPULSOR, "Falha ao obter localização: getLastLocation retornou nulo.");
                        Toast.makeText(getContext(), "Não foi possível obter a localização. Ative o GPS.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Erro do serviço
                    Log.e(TAG_PROPULSOR, "Erro do serviço de localização.", e);
                    Toast.makeText(getContext(), "Erro ao obter localização.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Limpar o binding
    }
}