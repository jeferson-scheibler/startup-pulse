package com.example.startuppulse.ui.canvas;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.startuppulse.CanvasPagerAdapter;
import com.example.startuppulse.LocationService;
import com.example.startuppulse.R;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.databinding.FragmentCanvasIdeiaBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasIdeiaFragment extends Fragment {

    private static final String TAG = "CanvasIdeia_DEBUG";

    private FragmentCanvasIdeiaBinding binding;
    private CanvasIdeiaViewModel viewModel;
    private NavController navController;
    private CanvasPagerAdapter pagerAdapter;

    private LocationService locationService;
    private Location userLocation;

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permissão concedida, tenta obter a localização novamente
                    iniciarFluxoDeLocalizacao();
                } else {
                    // Permissão negada, publica sem localização
                    Toast.makeText(getContext(), "Permissão negada. Buscando mentores sem usar sua localização.", Toast.LENGTH_LONG).show();
                    publicarIdeiaComLocalizacao(null);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasIdeiaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        locationService = new LocationService(requireActivity());

        viewModel = new ViewModelProvider(this).get(CanvasIdeiaViewModel.class);
        navController = NavHostFragment.findNavController(this);
        String ideiaId = getArguments() != null ? getArguments().getString("ideiaId") : null;

        setupViewPager();
        setupClickListeners();
        setupObservers();

        viewModel.loadIdeia(ideiaId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupViewPager() {
        pagerAdapter = new CanvasPagerAdapter(this);
        binding.viewPagerCanvas.setAdapter(pagerAdapter);
        binding.viewPagerCanvas.setUserInputEnabled(false);
        new TabLayoutMediator(binding.tabLayout, binding.viewPagerCanvas, (tab, position) -> {}).attach();
    }

    private void setupObservers() {
        viewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            Log.d(TAG, "Observer 'ideia': Acionado.");
            if (binding == null) return;

            // Garante que o grupo de conteúdo só fique visível quando tivermos uma ideia para mostrar
            binding.contentGroup.setVisibility(View.VISIBLE);
            updateUiState(ideia); // Atualiza os botões e outros componentes
        });

        viewModel.etapas.observe(getViewLifecycleOwner(), etapas -> {
            if (etapas != null) {
                pagerAdapter.setEtapas(etapas);
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Observer 'isLoading': Acionado. isLoading = " + isLoading);
            if (binding != null) {
                binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                // CORREÇÃO: Remove o controle sobre 'contentGroup' para evitar a race condition
                if (isLoading) {
                    binding.contentGroup.setVisibility(View.GONE);
                }
            }
        });

        viewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.closeScreenEvent.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                navController.navigateUp();
            }
        });

        viewModel.isPublishEnabled.observe(getViewLifecycleOwner(), isEnabled -> {
            if (binding != null && isEnabled != null) {
                binding.btnPublicarIdeia.setEnabled(isEnabled);
                binding.btnPublicarIdeia.setAlpha(isEnabled ? 1.0f : 0.5f);
            }
        });
    }

    private void setupClickListeners() {
        binding.btnVoltar.setOnClickListener(v -> viewModel.saveAndFinish());
        binding.btnAnterior.setOnClickListener(v -> binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() - 1, true));
        binding.btnProximo.setOnClickListener(v -> binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1, true));
        binding.btnPublicarIdeia.setOnClickListener(v -> verificarPermissaoEPublicar());

        binding.btnAvaliarIdeia.setOnClickListener(v -> {
            if (viewModel.ideia.getValue() != null) {
                Bundle args = new Bundle();
                args.putString("ideiaId", viewModel.ideia.getValue().getId());
                navController.navigate(R.id.action_canvasIdeiaFragment_to_avaliacaoFragment, args);
            }
        });

        binding.btnDespublicarIdeia.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Reverter para Rascunho")
                    .setMessage("Sua ideia voltará a ser um rascunho privado e perderá a associação com o mentor atual. Deseja continuar?")
                    .setPositiveButton("Sim", (dialog, which) -> viewModel.despublicarIdeia())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void updateUiState(@Nullable Ideia ideia) {
        // Esconde todos os botões e componentes que dependem do estado
        binding.btnPublicarIdeia.setVisibility(View.GONE);
        binding.btnAvaliarIdeia.setVisibility(View.GONE);
        binding.btnDespublicarIdeia.setVisibility(View.GONE);
        binding.btnAnterior.setVisibility(View.GONE);
        binding.btnProximo.setVisibility(View.GONE);
        binding.tabLayout.setVisibility(View.GONE);

        if (ideia == null) return;

        boolean isOwner = viewModel.isCurrentUserOwner();
        boolean isMentorPodeAvaliar = viewModel.isCurrentUserTheMentor() && ideia.getStatus() == Ideia.Status.EM_AVALIACAO;
        boolean isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;

        // Mostra os componentes de navegação apenas se a ideia for editável
        if (!isReadOnly) {
            binding.btnAnterior.setVisibility(View.VISIBLE);
            binding.btnProximo.setVisibility(View.VISIBLE);
            binding.tabLayout.setVisibility(View.VISIBLE);
        }

        // Mostra o botão de ação correto na toolbar
        if (isOwner) {
            if (ideia.getStatus() == Ideia.Status.RASCUNHO) {
                binding.btnPublicarIdeia.setVisibility(View.VISIBLE);
            } else {
                binding.btnDespublicarIdeia.setVisibility(View.VISIBLE);
            }
        } else if (isMentorPodeAvaliar) {
            binding.btnAvaliarIdeia.setVisibility(View.VISIBLE);
        }
    }

    private void verificarPermissaoEPublicar() {
        if (Boolean.FALSE.equals(viewModel.isPublishEnabled.getValue())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Requisitos para Publicação")
                    .setMessage("Para publicar, certifique-se de que cumpriu todos os requisitos:\n\n" +
                            "• Título e Descrição preenchidos.\n" +
                            "• Pelo menos 2 áreas de atuação selecionadas.\n" +
                            "• Pelo menos um post-it em cada bloco do Canvas.")
                    .setPositiveButton("Entendi", null)
                    .show();
            return;
        }

        // Verifica se já tem permissão
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarFluxoDeLocalizacao();
        } else {
            // Se não tem, pede a permissão. O resultado acionará o `requestLocationPermissionLauncher`.
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void iniciarFluxoDeLocalizacao() {
        if (getContext() == null) return;

        // Verifica se o GPS está ligado
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar o mentor ideal, precisamos da sua localização. Por favor, ative o GPS.")
                    .setPositiveButton("Ativar GPS", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Continuar sem GPS", (dialog, which) -> {
                        // O usuário escolheu continuar sem GPS
                        publicarIdeiaComLocalizacao(null);
                    })
                    .show();
            return;
        }

        // GPS está ativo, busca a localização
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationResult(Location location) {
                userLocation = location; // Armazena a localização
                showLocationConfirmationDialog(location); // Pergunta ao usuário
            }

            @Override
            public void onLocationError(String error) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                // Mesmo com erro, permite publicar sem localização
                publicarIdeiaComLocalizacao(null);
            }
        });
    }
    private void showLocationConfirmationDialog(Location location) {
        // A sua observação está correta. A comparação de proximidade é feita por coordenadas
        // no MentorMatchService. Tentar converter a localização para um nome de cidade aqui
        // (usando Geocoder) pode causar falhas de rede e é desnecessário para a lógica principal.
        // Esta versão simplificada apenas confirma o uso das coordenadas, evitando o erro.

        @SuppressLint("DefaultLocale") String message = String.format(
                "Deseja buscar mentores perto da sua localização atual (Lat: %.4f, Lon: %.4f)?",
                location.getLatitude(),
                location.getLongitude()
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Localização")
                .setMessage(message)
                .setPositiveButton("Sim, usar esta", (dialog, which) -> {
                    // O usuário confirmou. Prossegue para a publicação COM a localização.
                    publicarIdeiaComLocalizacao(location);
                })
                .setNegativeButton("Escolher outra", (dialog, which) -> {
                    // Futuramente, aqui abriria o seletor de mapa.
                    Snackbar.make(binding.getRoot(), "Funcionalidade de mapa a ser implementada.", Snackbar.LENGTH_SHORT).show();

                    // Importante: Permite ao usuário continuar sem a localização se ele não quiser usar a atual.
                    publicarIdeiaComLocalizacao(null);
                })
                .setCancelable(false)
                .show();
    }
    private void publicarIdeiaComLocalizacao(@Nullable Location location) {
        // A mágica acontece aqui!
        // Passamos a localização (que pode ser nula) para o ViewModel.
        viewModel.procurarNovoMentor(requireContext(), location);
    }
}