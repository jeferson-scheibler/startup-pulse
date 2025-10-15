// Em: app/src/main/java/com/example/startuppulse/IdeiaStatusFragment.java

package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.example.startuppulse.data.Avaliacao;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.FragmentIdeiaStatusBinding;
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel;
import com.example.startuppulse.util.PdfGenerator;

import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IdeiaStatusFragment extends Fragment {

    private FragmentIdeiaStatusBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;
    private Ideia currentIdeia; // Mantém uma referência local para facilitar o acesso

    // Launcher para permissão de escrita (para o PDF)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    gerarPdf();
                } else {
                    Toast.makeText(getContext(), "Permissão para salvar arquivos é necessária.", Toast.LENGTH_LONG).show();
                }
            });

    public IdeiaStatusFragment() {
        // Construtor vazio obrigatório
    }

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

        setupClickListeners();
        setupObservers();
    }

    private void setupClickListeners() {
        binding.btnDownloadPdf.setOnClickListener(v -> verificarPermissaoEGerarPdf());
        binding.btnPrepararInvestidores.setOnClickListener(v -> {
            if (currentIdeia != null && currentIdeia.getId() != null) {
                Intent intent = new Intent(getActivity(), PreparacaoInvestidorActivity.class);
                intent.putExtra("IDEIA_ID", currentIdeia.getId());
                startActivity(intent);
            }
        });
        binding.btnProcurarMentor.setOnClickListener(v -> tentarNovoMatchmaking());
        binding.btnVerFeedback.setOnClickListener(v -> showFeedbackDialog());
    }

    private void setupObservers() {
        sharedViewModel.ideia.observe(getViewLifecycleOwner(), ideia -> {
            if (ideia == null || !isAdded()) return;
            this.currentIdeia = ideia; // Atualiza a referência local
            updateUI(ideia);
        });

        // Observa o nome do mentor que o ViewModel buscará
        sharedViewModel.mentorNome.observe(getViewLifecycleOwner(), nome -> {
            if (binding != null) {
                binding.textMentorName.setText(nome != null ? nome : "Mentor não encontrado");
            }
        });
    }

    private void updateUI(Ideia ideia) {
        binding.textIdeiaTitle.setText(ideia.getNome());
        binding.textIdeiaDescription.setText(ideia.getDescricao());

        boolean isOwner = sharedViewModel.isCurrentUserOwner();
        boolean hasMentor = ideia.getMentorId() != null && !ideia.getMentorId().isEmpty();

        // --- Lógica do Mentor ---
        binding.btnProcurarMentor.setVisibility(View.GONE);
        if (hasMentor) {
            int colorActive = ContextCompat.getColor(requireContext(), R.color.primary_color);
            binding.cardMentor.setStrokeColor(colorActive);
            binding.iconMentor.setImageTintList(ColorStateList.valueOf(colorActive));
        } else {
            binding.textMentorName.setText("A procurar o mentor ideal...");
            if (isOwner) {
                binding.btnProcurarMentor.setVisibility(View.VISIBLE);
            }
        }

        // --- Lógica da Avaliação ---
        boolean isAvaliada = "Avaliada".equals(ideia.getAvaliacaoStatus());
        if (isAvaliada) {
            int colorSuccess = ContextCompat.getColor(requireContext(), R.color.green_success);
            binding.cardAvaliacao.setStrokeColor(colorSuccess);
            binding.iconAvaliacao.setImageTintList(ColorStateList.valueOf(colorSuccess));
            binding.textAvaliacaoStatus.setText("Ideia Avaliada!");
            binding.btnVerFeedback.setVisibility(ideia.getAvaliacoes() != null && !ideia.getAvaliacoes().isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            binding.textAvaliacaoStatus.setText("Aguardando avaliação do mentor");
            binding.btnVerFeedback.setVisibility(View.GONE);
        }

        // --- Lógica dos Botões de Ação ---
        binding.btnDownloadPdf.setVisibility(isAvaliada ? View.VISIBLE : View.GONE);
        binding.btnPrepararInvestidores.setVisibility(isAvaliada && !ideia.isProntaParaInvestidores() ? View.VISIBLE : View.GONE);

        if (ideia.isProntaParaInvestidores()) {
            binding.textAvaliacaoStatus.setText("Ideia Pronta para Investidores!");
            binding.textAvaliacaoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success));
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

    private void tentarNovoMatchmaking() {
        // A lógica de permissão e GPS permanece no Fragment, pois é relacionada à UI
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Permissão de localização é necessária.", Toast.LENGTH_LONG).show();
            // Lançar um launcher para pedir permissão seria o ideal aqui.
            return;
        }
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar um mentor, ative o GPS.")
                    .setPositiveButton("Ativar GPS", (dialog, which) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        binding.btnProcurarMentor.setEnabled(false);
        Toast.makeText(getContext(), "Procurando novo mentor...", Toast.LENGTH_SHORT).show();
        // Delega a ação para o ViewModel
        sharedViewModel.procurarNovoMentor(requireContext());
        // Re-habilita o botão após um tempo para evitar spam
        new Handler().postDelayed(() -> {
            if(isAdded()) binding.btnProcurarMentor.setEnabled(true);
        }, 5000); // 5 segundos
    }

    private void showFeedbackDialog() {
        if (!isAdded() || currentIdeia == null) return;
        List<Avaliacao> avaliacoes = currentIdeia.getAvaliacoes();
        if (avaliacoes == null || avaliacoes.isEmpty()) return;

        String mentorNome = sharedViewModel.mentorNome.getValue();
        if (mentorNome == null) mentorNome = "Mentor";

        SpannableStringBuilder formattedFeedback = new SpannableStringBuilder();
        for (Avaliacao criterio : avaliacoes) {
            String header = criterio.getCriterio() + String.format(Locale.getDefault(), ": %.1f/10\n", criterio.getNota());
            int start = formattedFeedback.length();
            formattedFeedback.append(header);
            formattedFeedback.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, formattedFeedback.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            formattedFeedback.append(criterio.getFeedback() != null && !criterio.getFeedback().trim().isEmpty() ? criterio.getFeedback() : "Nenhum feedback adicional.").append("\n\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Feedback de " + mentorNome)
                .setMessage(formattedFeedback)
                .setPositiveButton("Entendi", null)
                .show();
    }

    private void verificarPermissaoEGerarPdf() {
        if (getContext() == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                gerarPdf();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}