package com.example.startuppulse.ui.canvas;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.databinding.FragmentCanvasIdeiaBinding;
import com.example.startuppulse.ui.match.LocationChoiceDialog;
import com.example.startuppulse.ui.match.MatchCandidatesDialog;
import com.example.startuppulse.ui.match.MatchLoadingDialog;
import com.example.startuppulse.ui.match.ReMatchDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.Locale;

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
    private MatchLoadingDialog matchLoadingDialog;

    private ReMatchDialog reMatchDialog;


    // --- PERMISSÃO DE LOCALIZAÇÃO ---
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    capturarLocalizacaoEPublicar(); // Busca localização atual e segue com publicação
                } else {
                    Toast.makeText(getContext(),
                            "Permissão negada. Publicando sem usar sua localização.",
                            Toast.LENGTH_LONG).show();
                    publicarIdeia(null); // Publica sem coordenadas
                }
            });


    // ------------------------------------------------------------------
    // CICLO DE VIDA
    // ------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCanvasIdeiaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
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
        if (matchLoadingDialog != null && matchLoadingDialog.isShowing()) {
            matchLoadingDialog.dismiss();
        }
        matchLoadingDialog = null;
    }

    // ------------------------------------------------------------------
    // UI SETUP
    // ------------------------------------------------------------------

    private void setupViewPager() {
        pagerAdapter = new CanvasPagerAdapter(this);
        binding.viewPagerCanvas.setAdapter(pagerAdapter);
        binding.viewPagerCanvas.setUserInputEnabled(false);
        new TabLayoutMediator(binding.tabLayout, binding.viewPagerCanvas, (tab, position) -> {
        }).attach();
    }

    private void setupObservers() {

        // ------------------------------
        // IDEIA PRINCIPAL
        // ------------------------------
        viewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            Log.d(TAG, "Observer 'ideia' acionado: " + (ideia != null ? ideia.getNome() : "null"));
            if (binding == null) return;
            binding.contentGroup.setVisibility(View.VISIBLE);
            updateUiState(ideia);
        });

        // ------------------------------
        // ETAPAS
        // ------------------------------
        viewModel.etapas.observe(getViewLifecycleOwner(), etapas -> {
            if (etapas != null) {
                pagerAdapter.setEtapas(etapas);
            }
        });

        // ------------------------------
        // LOADING PADRÃO
        // ------------------------------
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null) return;

            if (Boolean.TRUE.equals(isLoading)) {
                binding.loadingIndicator.setVisibility(View.VISIBLE);
                binding.contentGroup.setVisibility(View.GONE);
            } else {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.contentGroup.setVisibility(View.VISIBLE);
            }
        });

        // ------------------------------
        // LOADING DE MATCH (DIALOG)
        // ------------------------------
        viewModel.matchLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                if (matchLoadingDialog == null)
                    matchLoadingDialog = new MatchLoadingDialog(requireContext());
                matchLoadingDialog.show();
            } else if (matchLoadingDialog != null && matchLoadingDialog.isShowing()) {
                matchLoadingDialog.dismiss();
            }
        });

        // ------------------------------
        // MENSAGEM DE PROGRESSO DO MATCH
        // ------------------------------
        viewModel.matchProgressMessage.observe(getViewLifecycleOwner(), message -> {
            if (matchLoadingDialog != null) {
                matchLoadingDialog.updateMessage(message);
            }
        });

        // ------------------------------
        // ESCOLHA DE LOCALIZAÇÃO
        // ------------------------------
        viewModel.matchLocationRequest.observe(getViewLifecycleOwner(), event -> {
            CanvasIdeiaViewModel.MatchLocationChoiceRequest req = event.getContentIfNotHandled();
            if (req != null) {
                LocationChoiceDialog dialog = new LocationChoiceDialog(
                        req.hasIdeaLocation(),
                        req.hasUserLocation(),
                        choice -> viewModel.handleMatchLocationChoice(choice, null, true)
                );
                dialog.show(getParentFragmentManager(), "LocationChoiceDialog");
            }
        });


        // ------------------------------
        // LISTA DE MENTORES
        // ------------------------------
        viewModel.matchCandidatesEvent.observe(getViewLifecycleOwner(), event -> {
            List<User> mentores = event.getContentIfNotHandled();
            if (mentores != null && !mentores.isEmpty()) {
                new MatchCandidatesDialog(requireContext(), mentores, mentor ->
                        viewModel.confirmarEscolhaDeMentor(mentor, false)
                ).show();
            } else {
                Log.w(TAG, "Nenhum mentor disponível encontrado.");
            }
        });

        // ------------------------------
        // TOASTS
        // ------------------------------
        viewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // ------------------------------
        // FECHAR TELA
        // ------------------------------
        viewModel.closeScreenEvent.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                navController.navigateUp();
            }
        });

        // ------------------------------
// REMATCH DIALOG
// ------------------------------
        viewModel.isRematching.observe(getViewLifecycleOwner(), isRematching -> {
            if (isRematching == null) return;

            if (isRematching) {
                if (reMatchDialog == null) {
                    reMatchDialog = new ReMatchDialog(requireContext());
                    reMatchDialog.setOnCancelListener(() -> viewModel.cancelarReMatch());
                }
                reMatchDialog.show();
            } else if (reMatchDialog != null && reMatchDialog.isShowing()) {
                reMatchDialog.dismiss();
            }
        });

        viewModel.rematchMessage.observe(getViewLifecycleOwner(), msg -> {
            if (reMatchDialog != null && reMatchDialog.isShowing()) {
                reMatchDialog.updateMessage(msg);
            }
        });


        // ------------------------------
        // HABILITAÇÃO DO BOTÃO PUBLICAR
        // ------------------------------
        viewModel.isPublishEnabled.observe(getViewLifecycleOwner(), isEnabled -> {
            if (binding != null && isEnabled != null) {
                binding.btnPublicarIdeia.setEnabled(isEnabled);
                binding.btnPublicarIdeia.setAlpha(isEnabled ? 1.0f : 0.5f);
            }
        });
    }

    // ------------------------------------------------------------------
    // BOTÕES E AÇÕES
    // ------------------------------------------------------------------

    private void setupClickListeners() {
        binding.btnVoltar.setOnClickListener(v -> viewModel.saveAndFinish());
        binding.btnAnterior.setOnClickListener(v ->
                binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() - 1, true)
        );
        binding.btnProximo.setOnClickListener(v ->
                binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1, true)
        );
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

    // ------------------------------------------------------------------
// PUBLICAÇÃO
// ------------------------------------------------------------------

    private void verificarPermissaoEPublicar() {
        if (Boolean.FALSE.equals(viewModel.isPublishEnabled.getValue())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Requisitos para Publicação")
                    .setMessage("Para publicar, certifique-se de:\n\n" +
                            "• Título e Descrição preenchidos.\n" +
                            "• Pelo menos 2 áreas de atuação selecionadas.\n" +
                            "• Pelo menos um post-it em cada bloco do Canvas.")
                    .setPositiveButton("Entendi", null)
                    .show();
            return;
        }

        // Verifica permissão de localização (necessário antes de publicar)
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            capturarLocalizacaoEPublicar();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void capturarLocalizacaoEPublicar() {
        if (getContext() == null) return;

        LocationManager locationManager =
                (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar o mentor ideal, precisamos da sua localização. Ative o GPS.")
                    .setPositiveButton("Ativar GPS", (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Continuar sem GPS", (dialog, which) ->
                            publicarIdeia(null))
                    .show();
            return;
        }

        // Busca a localização atual
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationResult(Location location) {
                userLocation = location;
                publicarIdeia(location);
            }

            @Override
            public void onLocationError(String error) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                publicarIdeia(null);
            }
        });
    }

    /**
     * Publica a ideia e delega a escolha de localização/match ao ViewModel.
     */
    private void publicarIdeia(@Nullable Location location) {
        Ideia ideiaAtual = viewModel.ideia.getValue();
        if (ideiaAtual == null) {
            Log.w(TAG, "Nenhuma ideia carregada no ViewModel. Cancelando publicação.");
            return;
        }

        // Define coordenadas se disponíveis
        if (location != null) {
            ideiaAtual.setLatitude(location.getLatitude());
            ideiaAtual.setLongitude(location.getLongitude());
        } else {
            ideiaAtual.setLatitude(null);
            ideiaAtual.setLongitude(null);
            ideiaAtual.setLocalizacaoTexto(null);
        }

        // Passa os dados ao ViewModel para salvar e continuar fluxo de match
        viewModel.publicarIdeiaComLocalizacaoAtualizada(requireContext(), ideiaAtual, location);
    }



    // ------------------------------------------------------------------
    // UI STATE
    // ------------------------------------------------------------------

    private void updateUiState(@Nullable Ideia ideia) {
        if (binding == null || ideia == null) return;

        binding.btnPublicarIdeia.setVisibility(View.GONE);
        binding.btnAvaliarIdeia.setVisibility(View.GONE);
        binding.btnDespublicarIdeia.setVisibility(View.GONE);
        binding.btnAnterior.setVisibility(View.GONE);
        binding.btnProximo.setVisibility(View.GONE);
        binding.tabLayout.setVisibility(View.GONE);

        boolean isOwner = viewModel.isCurrentUserOwner();
        boolean isMentorPodeAvaliar = viewModel.isCurrentUserTheMentor() &&
                ideia.getStatus() == Ideia.Status.EM_AVALIACAO;
        boolean isReadOnly = ideia.getStatus() != Ideia.Status.RASCUNHO;
        boolean isMentor = viewModel.isCurrentUserTheMentor();

        if (!isReadOnly) {
            binding.btnAnterior.setVisibility(View.VISIBLE);
            binding.btnProximo.setVisibility(View.VISIBLE);
            binding.tabLayout.setVisibility(View.VISIBLE);
        }

        if (isOwner) {
            if (ideia.getStatus() == Ideia.Status.RASCUNHO || ideia.getStatus() == null) {
                binding.btnPublicarIdeia.setVisibility(View.VISIBLE);
            } else if (ideia.getStatus() == Ideia.Status.EM_AVALIACAO) {
                binding.btnDespublicarIdeia.setVisibility(View.VISIBLE);
            }
        } else if (isMentorPodeAvaliar) {
            binding.btnAvaliarIdeia.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "updateUiState: Status=" + ideia.getStatus() +
                ", Owner=" + isOwner +
                ", Mentor=" + isMentor +
                ", btnPublicar=" + (binding.btnPublicarIdeia.getVisibility() == View.VISIBLE) +
                ", btnDespublicar=" + (binding.btnDespublicarIdeia.getVisibility() == View.VISIBLE) +
                ", btnAvaliar=" + (binding.btnAvaliarIdeia.getVisibility() == View.VISIBLE));

    }
}
