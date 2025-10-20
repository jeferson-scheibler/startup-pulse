package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
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
import com.example.startuppulse.data.Avaliacao;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.databinding.FragmentIdeiaStatusBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.example.startuppulse.util.PdfGenerator;
import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

import android.location.Location;

import com.google.android.material.snackbar.Snackbar;

@AndroidEntryPoint
public class IdeiaStatusFragment extends Fragment {

    private FragmentIdeiaStatusBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;
    private NavController navController;
    private Ideia currentIdeia;
    private LocationService locationService;

    // Launcher para permissões
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permissão foi concedida, tenta o matchmaking novamente
                    tentarNovoMatchmaking();
                } else {
                    // O usuário negou. Tenta o match sem localização.
                    Toast.makeText(getContext(), "Permissão negada. Buscando novo mentor sem usar sua localização.", Toast.LENGTH_LONG).show();
                    sharedViewModel.procurarNovoMentor(requireContext(), null);
                }
            });

    private final ActivityResultLauncher<String> requestStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    gerarPdf();
                } else {
                    Toast.makeText(getContext(), "Permissão para salvar arquivos é necessária.", Toast.LENGTH_LONG).show();
                }
            });

    // --- Ciclo de Vida do Fragment ---

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiaStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Obtém o ViewModel compartilhado com o fragmento pai (CanvasIdeiaFragment)
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);
        navController = NavHostFragment.findNavController(this);

        locationService = new LocationService(requireActivity());

        setupClickListeners();
        setupObservers();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // --- Configuração da UI e Observadores ---

    private void setupClickListeners() {
        binding.btnDownloadPdf.setOnClickListener(v -> verificarPermissaoEGerarPdf());
        binding.btnProcurarMentor.setOnClickListener(v -> tentarNovoMatchmaking());
        binding.btnVerFeedback.setOnClickListener(v -> showFeedbackDialog());

        binding.btnPrepararInvestidores.setOnClickListener(v -> {
            if (currentIdeia != null && currentIdeia.getId() != null) {
                // CORRIGIDO: Usando NavController para navegação segura
                Bundle args = new Bundle();
                args.putString("ideiaId", currentIdeia.getId());
                // A action 'action_global_to_preparacaoInvestidorFragment' precisa estar definida no seu nav_graph
                navController.navigate(R.id.action_global_to_preparacaoInvestidorFragment, args);
            }
        });
    }

    private void setupObservers() {
        // Observa a ideia principal para atualizar toda a UI
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null || !isAdded()) return;
            this.currentIdeia = ideia;
            updateUI(ideia);
        });

        // Observa o nome do mentor para preencher o campo específico
        sharedViewModel.mentorNome.observe(getViewLifecycleOwner(), nome -> {
            if (binding != null && currentIdeia != null && currentIdeia.getMentorId() != null) {
                binding.textMentorName.setText(nome != null ? nome : "Carregando...");
            }
        });

        // Observa eventos de Toast para feedback do matchmaking e outras operações
        sharedViewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // Observa o estado de carregamento para dar feedback visual
        sharedViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null) {
                binding.btnProcurarMentor.setEnabled(!isLoading);
                if (isLoading && (currentIdeia == null || currentIdeia.getMentorId() == null)) {
                    binding.textMentorName.setText("Procurando...");
                }
            }
        });
    }

    private void updateUI(Ideia ideia) {
        if (binding == null || getContext() == null) return;

        binding.textIdeiaTitle.setText(ideia.getNome());
        binding.textIdeiaDescription.setText(ideia.getDescricao());

        boolean isOwner = sharedViewModel.isCurrentUserOwner();
        boolean hasMentor = ideia.getMentorId() != null && !ideia.getMentorId().isEmpty();

        // --- Lógica do Mentor ---
        int colorInactive = ContextCompat.getColor(requireContext(), R.color.strokeSoft);
        int colorActive = ContextCompat.getColor(requireContext(), R.color.colorPrimary);

        binding.cardMentor.setStrokeColor(hasMentor ? colorActive : colorInactive);
        binding.iconMentor.setImageTintList(ColorStateList.valueOf(hasMentor ? colorActive : colorInactive));
        binding.btnProcurarMentor.setVisibility(isOwner && !hasMentor ? View.VISIBLE : View.GONE);

        if (!hasMentor) {
            binding.textMentorName.setText("A procurar o mentor ideal...");
        }

        // --- Lógica da Avaliação ---
        boolean isAvaliada = "Avaliada".equals(ideia.getAvaliacaoStatus());
        int colorSuccess = ContextCompat.getColor(requireContext(), R.color.green_success);

        binding.cardAvaliacao.setStrokeColor(isAvaliada ? colorSuccess : colorInactive);
        binding.iconAvaliacao.setImageTintList(ColorStateList.valueOf(isAvaliada ? colorSuccess : colorInactive));
        binding.textAvaliacaoStatus.setText(isAvaliada ? "Ideia Avaliada!" : "Aguardando avaliação do mentor");
        binding.btnVerFeedback.setVisibility(isAvaliada && ideia.getAvaliacoes() != null && !ideia.getAvaliacoes().isEmpty() ? View.VISIBLE : View.GONE);

        // --- Lógica dos Botões de Ação ---
        binding.btnDownloadPdf.setVisibility(isAvaliada ? View.VISIBLE : View.GONE);
        binding.btnPrepararInvestidores.setVisibility(isAvaliada && !ideia.isProntaParaInvestidores() ? View.VISIBLE : View.GONE);

        if (ideia.isProntaParaInvestidores()) {
            binding.textAvaliacaoStatus.setText("Ideia Pronta para Investidores!");
            binding.textAvaliacaoStatus.setTextColor(colorSuccess);
        }

        // --- Animação Lottie ---
        if (isAvaliada) {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_sucess);
        } else if (hasMentor) {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_analise_mentor);
        } else {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_mapa_procurando);
        }
        binding.lottieStatusAnimation.playAnimation();
    }

    // --- Métodos de Ação do Usuário ---

    private void tentarNovoMatchmaking() {
        if (getContext() == null) return;

        // PASSO 1: Checar permissão
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Se não tem, pede a permissão. O fluxo continuará no launcher.
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        // PASSO 2: Checar se o GPS está ativo (já com permissão)
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar o melhor mentor para sua ideia, por favor, ative o GPS.")
                    .setPositiveButton("Ativar", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Continuar sem", (dialog, which) -> {
                        // Usuário decidiu continuar sem GPS, chama o método com localização nula
                        sharedViewModel.procurarNovoMentor(requireContext(), null);
                    })
                    .show();
            return;
        }

        // PASSO 3: Permissão e GPS OK. Buscar a localização.
        locationService.getCurrentLocation(new LocationService.LocationCallback() {
            @Override
            public void onLocationResult(Location location) {
                // Sucesso! Chama o ViewModel com a localização.
                Snackbar.make(requireView(), "Localização obtida. Buscando mentor...", Snackbar.LENGTH_SHORT).show();
                sharedViewModel.procurarNovoMentor(requireContext(), location);
            }

            @Override
            public void onLocationError(String error) {
                // Houve um erro. Informa o usuário e continua sem localização.
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
                sharedViewModel.procurarNovoMentor(requireContext(), null);
            }
        });
    }

    private void showFeedbackDialog() {
        if (!isAdded() || currentIdeia == null || currentIdeia.getAvaliacoes() == null || currentIdeia.getAvaliacoes().isEmpty()) return;

        String mentorNome = sharedViewModel.mentorNome.getValue() != null ? sharedViewModel.mentorNome.getValue() : "Mentor";
        List<Avaliacao> avaliacoes = currentIdeia.getAvaliacoes();

        SpannableStringBuilder formattedFeedback = new SpannableStringBuilder();
        for (Avaliacao criterio : avaliacoes) {
            String header = criterio.getCriterio() + String.format(Locale.getDefault(), ": %.1f/10\n", criterio.getNota());
            int start = formattedFeedback.length();
            formattedFeedback.append(header);
            formattedFeedback.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, formattedFeedback.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String feedbackText = (criterio.getFeedback() != null && !criterio.getFeedback().trim().isEmpty()) ? criterio.getFeedback() : "Nenhum feedback adicional.";
            formattedFeedback.append(feedbackText).append("\n\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Feedback de " + mentorNome)
                .setMessage(formattedFeedback)
                .setPositiveButton("Entendi", null)
                .show();
    }

    private void verificarPermissaoEGerarPdf() {
        if (getContext() == null) return;
        // A partir do Android Q, não é mais necessária a permissão para salvar em pastas públicas
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                gerarPdf();
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            gerarPdf();
        }
    }

    private void gerarPdf() {
        if (getContext() == null || currentIdeia == null) return;
        Toast.makeText(getContext(), "Gerando PDF...", Toast.LENGTH_SHORT).show();
        PdfGenerator.gerarCanvas(getContext(), currentIdeia, (success, message) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(getContext(), "Erro: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}