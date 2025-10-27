package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.WindowManager;
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
import com.example.startuppulse.databinding.DialogMentorFeedbackBinding;
import com.example.startuppulse.databinding.FragmentIdeiaStatusBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.example.startuppulse.util.PdfGenerator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

import android.location.Location;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
                Bundle args = new Bundle();
                args.putString("ideiaId", currentIdeia.getId());
                // A action 'action_global_to_preparacaoInvestidorFragment' precisa estar definida no seu nav_graph
                navController.navigate(R.id.action_global_to_preparacaoInvestidorFragment, args);
            }
        });

        // --- Listeners da IA (Corretos) ---
        binding.btnSolicitarAnaliseIa.setOnClickListener(v -> {
            sharedViewModel.solicitarAnaliseIA();
        });

        binding.btnVerAnaliseIa.setOnClickListener(v -> {
            if (currentIdeia != null && currentIdeia.getAvaliacaoIA() != null) {
                mostrarDialogAnalise(currentIdeia.getAvaliacaoIA());
            } else {
                Toast.makeText(getContext(), "Análise ainda não disponível.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupObservers() {
        // Observa a ideia principal para atualizar toda a UI
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null || !isAdded() || binding == null) return;
            this.currentIdeia = ideia;
            updateUI(ideia);
        });

        // Observa o nome do mentor para preencher o campo específico
        sharedViewModel.mentorNome.observe(getViewLifecycleOwner(), nome -> {
            if (binding != null && currentIdeia != null && currentIdeia.getMentorId() != null) {
                binding.textMentorName.setText(nome != null ? nome : "Carregando...");
            }
        });

        sharedViewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        // Observa o carregamento do MATCHMAKING
        sharedViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null) {
                binding.btnProcurarMentor.setEnabled(!isLoading);
                if (isLoading && (currentIdeia == null || currentIdeia.getMentorId() == null)) {
                    binding.textMentorName.setText("Procurando...");
                }
            }
        });

        // Observa o carregamento da ANÁLISE DE IA
        sharedViewModel.isIaLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null || !isAdded()) return;
            if (isLoading) {
                binding.progressBarIa.setVisibility(View.VISIBLE);
                binding.btnSolicitarAnaliseIa.setEnabled(false);
                binding.btnSolicitarAnaliseIa.setText("Analisando...");
            } else {
                binding.progressBarIa.setVisibility(View.GONE);
                binding.btnSolicitarAnaliseIa.setEnabled(true);
                binding.btnSolicitarAnaliseIa.setText("Solicitar Pré-Análise com IA");
            }
        });
    }

    private void updateUI(Ideia ideia) {
        if (binding == null || getContext() == null) return;

        binding.textIdeiaTitle.setText(ideia.getNome());
        binding.textIdeiaDescription.setText(ideia.getDescricao());

        boolean isOwner = sharedViewModel.isCurrentUserOwner();
        boolean hasMentor = ideia.getMentorId() != null && !ideia.getMentorId().isEmpty();
        boolean isMentor = sharedViewModel.isCurrentUserTheMentor();

        // --- Lógica do Mentor ---
        int colorInactive = ContextCompat.getColor(requireContext(), R.color.white_translucent);
        int colorActive = ContextCompat.getColor(requireContext(), R.color.colorPrimary);

        binding.cardMentor.setStrokeColor(hasMentor ? colorActive : colorInactive);
        binding.iconMentor.setImageTintList(ColorStateList.valueOf(hasMentor ? colorActive : colorInactive));
        binding.btnProcurarMentor.setVisibility(isOwner && !hasMentor ? View.VISIBLE : View.GONE);
        binding.btnVerFeedback.setEnabled(false);
        binding.btnDownloadPdf.setEnabled(false);
        binding.btnPrepararInvestidores.setEnabled(false);


        if (!hasMentor) {
            binding.textMentorName.setText("A procurar o mentor ideal...");
        }

        Ideia.Status status = ideia.getStatus();

        // 2. Defina os booleanos com base no Enum (MUITO MAIS SEGURO)
        boolean isAprovada = (status == Ideia.Status.AVALIADA_APROVADA);
        boolean isReprovada = (status == Ideia.Status.AVALIADA_REPROVADA);
        boolean isAvaliada = isAprovada || isReprovada; // Verdadeiro para ambos

        int colorSuccess = ContextCompat.getColor(requireContext(), R.color.green_success);

        int colorFailure = ContextCompat.getColor(requireContext(), R.color.colorError);

        // 3. Atualize o card de Avaliação
        int avaliacaoColor = colorInactive;
        if (isAprovada) {
            avaliacaoColor = colorSuccess;
        } else if (isReprovada) {
            avaliacaoColor = colorFailure;
        }

        binding.cardAvaliacao.setStrokeColor(avaliacaoColor);
        binding.iconAvaliacao.setImageTintList(ColorStateList.valueOf(avaliacaoColor));

        binding.textAvaliacaoStatus.setTextColor(colorInactive);

        if (ideia.isProntaParaInvestidores() && isAprovada) {
            binding.textAvaliacaoStatus.setText("Ideia pronta para investidores!");
            binding.textAvaliacaoStatus.setTextColor(colorSuccess);
        } else if (isAprovada) {
            binding.textAvaliacaoStatus.setText("Ideia aprovada!");
            binding.textAvaliacaoStatus.setTextColor(colorSuccess);
        } else if (isReprovada) {
            binding.textAvaliacaoStatus.setText("Ideia reprovada");
            binding.textAvaliacaoStatus.setTextColor(colorFailure);
        } else {
            binding.textAvaliacaoStatus.setText("Aguardando avaliação do mentor");
        }

        // --- Lógica da IA (NOVO) ---
        boolean hasIAAnalysis = ideia.getAvaliacaoIA() != null;
        int iaColor = hasIAAnalysis ? colorActive : colorInactive; // Reutiliza a cor ativa

        binding.cardIa.setStrokeColor(iaColor);
        binding.iconIa.setImageTintList(ColorStateList.valueOf(iaColor));

        if (hasIAAnalysis) {
            binding.textIaStatus.setText("Sua análise de IA está pronta.");
            binding.btnVerAnaliseIa.setVisibility(View.VISIBLE);
            // Mostra o botão de solicitar novamente, caso o usuário queira atualizar
            binding.btnSolicitarAnaliseIa.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            binding.btnSolicitarAnaliseIa.setText("Solicitar nova análise");
        } else {
            binding.textIaStatus.setText("Receba um feedback instantâneo sobre sua ideia.");
            binding.btnVerAnaliseIa.setVisibility(View.GONE);
            binding.btnSolicitarAnaliseIa.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            binding.btnSolicitarAnaliseIa.setText("Solicitar pré-análise com IA");
        }
        // Oculta tudo se não for o dono
        binding.cardIa.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        binding.btnVerFeedback.setEnabled(isAvaliada && ideia.getAvaliacoes() != null && !ideia.getAvaliacoes().isEmpty());
        binding.btnDownloadPdf.setEnabled(isOwner || isMentor);
        binding.btnPrepararInvestidores.setEnabled(isOwner && isAprovada && !ideia.isProntaParaInvestidores());

        if (isAprovada) {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_sucess);
        } else if (isReprovada) {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_ambiente_distracao);
        } else if (hasMentor) {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_analise_mentor);
        } else {
            binding.lottieStatusAnimation.setAnimation(R.raw.anim_mapa_procurando);
        }
        binding.lottieStatusAnimation.playAnimation();
    }

    /**
     * Cria e exibe um Dialog simples com os resultados da IA.
     */
    private void mostrarDialogAnalise(Map<String, Object> avaliacaoIA) {
        if (getContext() == null) return;
        // Extrai os dados do Map (com checagem de tipo)
        StringBuilder mensagem = new StringBuilder();

        String resumo = (String) avaliacaoIA.get("resumo_geral");
        mensagem.append("Resumo Geral:\n").append(resumo).append("\n\n");

        List<String> fortes = (List<String>) avaliacaoIA.get("pontos_fortes");
        if (fortes != null) {
            mensagem.append("Pontos Fortes:\n");
            for (String ponto : fortes) {
                mensagem.append("- ").append(ponto).append("\n");
            }
            mensagem.append("\n");
        }

        List<String> fracos = (List<String>) avaliacaoIA.get("pontos_fracos_e_riscos");
        if (fracos != null) {
            mensagem.append("Pontos Fracos e Riscos:\n");
            for (String ponto : fracos) {
                mensagem.append("- ").append(ponto).append("\n");
            }
            mensagem.append("\n");
        }

        List<String> sugestoes = (List<String>) avaliacaoIA.get("sugestoes_proximos_passos");
        if (sugestoes != null) {
            mensagem.append("Próximos Passos Sugeridos:\n");
            for (String passo : sugestoes) {
                mensagem.append("- ").append(passo).append("\n");
            }
        }

        // Exibe o Dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Análise do Mentor IA")
                .setMessage(mensagem.toString())
                .setPositiveButton("Entendido", null)
                .show();
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
        // 1. Verificações de segurança (mantidas)
        if (!isAdded() || currentIdeia == null || currentIdeia.getAvaliacoes() == null || currentIdeia.getAvaliacoes().isEmpty()) return;

        // Adiciona uma verificação de contexto
        if (getContext() == null) return;

        // 2. Preparação dos dados (exatamente como estava, está ótimo)
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

        // --- LÓGICA DO DIÁLOGO ANTIGO REMOVIDA ---
        // new AlertDialog.Builder(requireContext())...

        // --- INÍCIO DA LÓGICA DO NOVO DIÁLOGO "GLASS" ---

        // 3. Infla o layout customizado
        DialogMentorFeedbackBinding dialogBinding = DialogMentorFeedbackBinding.inflate(LayoutInflater.from(getContext()));

        // 4. Constrói o diálogo
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogBinding.getRoot());
        builder.setCancelable(false); // Força o usuário a usar o botão "Fechar"

        AlertDialog dialog = builder.create();

        // 5. Aplica o estilo "Glass" (fundo transparente + dim/escurecimento)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.dimAmount = 0.7f; // Escurece o fundo
            lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            dialog.getWindow().setAttributes(lp);
        }

        // 6. Define o conteúdo dinâmico
        dialogBinding.dialogTitle.setText("Feedback de " + mentorNome);
        dialogBinding.dialogFeedbackText.setText(formattedFeedback);

        // 7. Configura o botão de fechar
        dialogBinding.dialogButtonClose.setOnClickListener(v -> {
            dialog.dismiss();
        });

        // 8. Exibe o diálogo
        dialog.show();
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