package com.example.startuppulse.ui.canvas;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
// Removido: import android.location.Location; // Não utilizado diretamente
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import androidx.navigation.Navigation;
// Removido: import androidx.viewpager2.widget.ViewPager2; // Não utilizado diretamente

import com.example.startuppulse.AvaliacaoActivity;
import com.example.startuppulse.CanvasPagerAdapter;
import com.example.startuppulse.R;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.FragmentCanvasIdeiaBinding;
import com.example.startuppulse.util.DialogEvent;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CanvasIdeiaFragment extends Fragment {

    private FragmentCanvasIdeiaBinding binding;
    private CanvasIdeiaViewModel viewModel;
    private NavController navController;
    private CanvasPagerAdapter pagerAdapter;
    // Removido: a flag isReadOnly local agora é controlada 100% pelo ViewModel
    // private boolean isReadOnly = false;
    private AlertDialog loadingDialog;

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarProcessoDePublicacaoComPermissao();
                } else {
                    Toast.makeText(getContext(), "Permissão de localização negada para matchmaking.", Toast.LENGTH_LONG).show();
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

        viewModel = new ViewModelProvider(this).get(CanvasIdeiaViewModel.class);
        navController = Navigation.findNavController(view);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        String ideiaId = getArguments() != null ? getArguments().getString("ideiaId") : null;
        boolean isReadOnly = getArguments() != null && getArguments().getBoolean("isReadOnly", false);

        // O ViewModel agora é responsável por carregar e gerenciar TODO o estado
        viewModel.loadIdeia(ideiaId, isReadOnly);

        setupViewPager();
        setupObservers();
        setupClickListeners();
    }

    private void setupViewPager() {
        pagerAdapter = new CanvasPagerAdapter(this);
        binding.viewPagerCanvas.setAdapter(pagerAdapter);
        binding.viewPagerCanvas.setUserInputEnabled(false);
        new TabLayoutMediator(binding.tabLayout, binding.viewPagerCanvas, (tab, position) -> {}).attach();
    }

    private void setupObservers() {
        viewModel.uiState.observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;
            updateTopButtonVisibility(state);
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.contentGroup.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        viewModel.isPublishEnabled.observe(getViewLifecycleOwner(), isEnabled -> {
            if (binding != null && isEnabled != null) {
                binding.btnPublicarIdeia.setEnabled(isEnabled);
                // Opcional: Mudar a aparência do botão quando desabilitado
                binding.btnPublicarIdeia.setAlpha(isEnabled ? 1.0f : 0.5f);
            }
        });

        viewModel.etapas.observe(getViewLifecycleOwner(), etapas -> {
            if (etapas != null) {
                pagerAdapter.setEtapas(etapas);
            }
        });

        viewModel.toastMessage.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.closeScreen.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                navController.navigateUp();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnVoltar.setOnClickListener(v -> {
            // ALTERAÇÃO 2: Confiar no ViewModel
            // Não passamos mais a ideia, o ViewModel já tem a versão mais atualizada.
            if (Boolean.TRUE.equals(viewModel.isReadOnly.getValue())) {
                navController.navigateUp();
            } else {
                viewModel.saveAndFinish();
            }
        });

        binding.btnAnterior.setOnClickListener(v -> binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() - 1, true));
        binding.btnProximo.setOnClickListener(v -> binding.viewPagerCanvas.setCurrentItem(binding.viewPagerCanvas.getCurrentItem() + 1, true));

        binding.btnPublicarIdeia.setOnClickListener(v -> {
            // Embora o botão deva estar desabilitado, esta é uma verificação de segurança final.
            if (Boolean.TRUE.equals(viewModel.isPublishEnabled.getValue())) {
                verificarPermissaoEPublicar();
            } else {
                // Informa ao usuário por que ele não pode publicar.
                Toast.makeText(getContext(), "Preencha todos os campos do canvas para publicar", Toast.LENGTH_LONG).show();
            }
        });

        binding.btnDespublicarIdeia.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Despublicar Ideia")
                        .setMessage("A sua ideia voltará a ser um rascunho privado. Deseja continuar?")
                        // ALTERAÇÃO 3: Confiar no ViewModel
                        .setPositiveButton("Sim, despublicar", (dialog, which) -> viewModel.despublicarIdeia())
                        .setNegativeButton("Cancelar", null)
                        .show()
        );

        binding.btnAvaliarIdeia.setOnClickListener(v -> {
            // Navega para a tela de avaliação, passando o ID da ideia.
            // Você precisará de uma Activity ou Fragment para a avaliação.
            if (viewModel.ideia.getValue() != null) {
                // Exemplo de navegação para uma futura AvaliacaoActivity
                Intent intent = new Intent(getActivity(), AvaliacaoActivity.class);
                intent.putExtra("ideiaId", viewModel.ideia.getValue().getId());
                startActivity(intent);
            }
        });
    }

    private void verificarPermissaoEPublicar() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciarProcessoDePublicacaoComPermissao();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void iniciarProcessoDePublicacaoComPermissao() {
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar o mentor ideal, precisamos da sua localização. Por favor, ative o GPS.")
                    .setPositiveButton("Ativar GPS", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        // ALTERAÇÃO 4: Confiar no ViewModel
        viewModel.iniciarProcessoDePublicacao(null); // O ViewModel já tem a ideia, não precisa passar.
    }

    private void updateTopButtonVisibility(CanvasUIState state) {
        binding.btnPublicarIdeia.setVisibility(View.GONE);
        binding.btnDespublicarIdeia.setVisibility(View.GONE);
        binding.btnAvaliarIdeia.setVisibility(View.GONE);

        // Se a tela for "somente leitura", nenhum botão de ação é exibido.
        if (state.isReadOnly) {
            return;
        }

        // Se a tela for interativa, determina a ação correta.
        if (state.isOwner) {
            if (state.ideia.getStatus() == Ideia.Status.RASCUNHO) {
                binding.btnPublicarIdeia.setVisibility(View.VISIBLE);
            } else {
                binding.btnDespublicarIdeia.setVisibility(View.VISIBLE);
            }
        } else if (state.isMentorPodeAvaliar) {
            binding.btnAvaliarIdeia.setVisibility(View.VISIBLE);
        }
    }

    private void setReadOnlyMode() {
        binding.btnAnterior.setEnabled(false);
        binding.btnProximo.setEnabled(false);
        binding.btnPublicarIdeia.setVisibility(View.GONE);
        binding.btnDespublicarIdeia.setVisibility(View.GONE);
        binding.btnAvaliarIdeia.setVisibility(View.GONE);
    }

    private void handleDialogEvent(DialogEvent event) {
        switch (event.type) {
            case LOADING:
                showLoadingDialog(event.message);
                break;
            case HIDE:
                hideLoadingDialog();
                break;
            case NO_MENTOR_FOUND:
                hideLoadingDialog();
                new AlertDialog.Builder(requireContext())
                        .setTitle("Nenhum Mentor Encontrado")
                        .setMessage("Não encontramos um mentor com o perfil ideal. Deseja publicar sua ideia para uma avaliação geral?")
                        // ALTERAÇÃO 5: Confiar no ViewModel
                        .setPositiveButton("Publicar Mesmo Assim", (dialog, which) -> viewModel.publicarIdeiaSemMentor())
                        .setNegativeButton("Cancelar", null)
                        .show();
                break;
        }
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null);
            builder.setView(dialogView);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
        TextView loadingText = loadingDialog.findViewById(R.id.loading_text);
        if (loadingText != null) loadingText.setText(message);
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}