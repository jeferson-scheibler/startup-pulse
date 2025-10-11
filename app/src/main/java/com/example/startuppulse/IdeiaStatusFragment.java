package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.startuppulse.databinding.FragmentIdeiaStatusBinding; // Importar o ViewBinding
import com.example.startuppulse.util.PdfGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List; // Import necessário
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fragmento para exibir o status de uma ideia publicada, incluindo o mentor
 * associado e o estado da avaliação.
 */
public class IdeiaStatusFragment extends Fragment {

    private FragmentIdeiaStatusBinding binding; // Objeto de ViewBinding
    private Ideia ideia;
    private FirestoreHelper firestoreHelper;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (ideia != null) {
                        gerarPdf();
                    }
                } else {
                    Toast.makeText(getContext(), "Permissão para salvar arquivos é necessária.", Toast.LENGTH_LONG).show();
                }
            });
    public static IdeiaStatusFragment newInstance(Ideia ideia) {
        IdeiaStatusFragment fragment = new IdeiaStatusFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreHelper = new FirestoreHelper();
        if (getArguments() != null) {
            ideia = (Ideia) getArguments().getSerializable("ideia");
        }
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
        if (ideia == null || !isAdded()) {
            return;
        }

        binding.btnDownloadPdf.setOnClickListener(v -> verificarPermissaoEGerarPdf());

        binding.btnPrepararInvestidores.setOnClickListener(v -> {
            if (ideia != null && ideia.getId() != null) {
                Intent intent = new Intent(getActivity(), PreparacaoInvestidorActivity.class);
                intent.putExtra("IDEIA_ID", ideia.getId());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Erro: ID da ideia não encontrado.", Toast.LENGTH_SHORT).show();
            }
        });

        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateUI() {
        // --- Dados Básicos da Ideia ---
        binding.textIdeiaTitle.setText(ideia.getNome());
        binding.textIdeiaDescription.setText(ideia.getDescricao());

        // --- Status do Mentor ---
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = user != null && user.getUid().equals(ideia.getOwnerId());

        boolean hasMentor = ideia.getMentorId() != null && !ideia.getMentorId().isEmpty();
        binding.btnProcurarMentor.setVisibility(View.GONE);

        if (hasMentor) {
            int colorActive = ContextCompat.getColor(requireContext(), R.color.primary_color);
            binding.cardMentor.setStrokeColor(colorActive);
            binding.iconMentor.setImageTintList(ColorStateList.valueOf(colorActive));

            // Busca o nome do mentor
            firestoreHelper.findMentorById(ideia.getMentorId(), r -> {
                if (isAdded() && r.isOk() && r.data != null) {
                    binding.textMentorName.setText(r.data.getNome());
                } else {
                    binding.textMentorName.setText("Mentor não encontrado");
                }
            });
        } else {
            binding.textMentorName.setText("A procurar o mentor ideal...");
            if (isOwner) {
                binding.btnProcurarMentor.setVisibility(View.VISIBLE);
                binding.btnProcurarMentor.setOnClickListener(v -> tentarNovoMatchmaking());
            }
        }

        String statusAvaliacao = ideia.getAvaliacaoStatus();
        boolean isAvaliada = "Avaliada".equals(statusAvaliacao);
        boolean isEmAvaliacao = "Pendente".equals(statusAvaliacao);

        if (isAvaliada || isEmAvaliacao) {
            binding.btnDownloadPdf.setVisibility(View.VISIBLE);
        } else {
            binding.btnDownloadPdf.setVisibility(View.GONE);
        }

        if (isAvaliada && !ideia.isProntaParaInvestidores()) {
            // Só mostra o botão se a ideia foi avaliada E AINDA NÃO está pronta para investidores
            binding.btnPrepararInvestidores.setVisibility(View.VISIBLE);
        } else {
            binding.btnPrepararInvestidores.setVisibility(View.GONE);
        }

        // Se a ideia já estiver pronta, podemos mostrar uma mensagem de sucesso
        if (ideia.isProntaParaInvestidores()) {
            binding.textAvaliacaoStatus.setText("Ideia Pronta para Investidores!");
            binding.textAvaliacaoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success));
        }

        if (isAvaliada) {
            int colorSuccess = ContextCompat.getColor(requireContext(), R.color.green_success);
            binding.cardAvaliacao.setStrokeColor(colorSuccess);
            binding.iconAvaliacao.setImageTintList(ColorStateList.valueOf(colorSuccess));
            binding.textAvaliacaoStatus.setText("Ideia Avaliada!");

            if (ideia.getAvaliacoes() != null && !ideia.getAvaliacoes().isEmpty()) {
                binding.btnVerFeedback.setVisibility(View.VISIBLE);
                binding.btnVerFeedback.setOnClickListener(v -> showFeedbackDialog());
            } else {
                binding.btnVerFeedback.setVisibility(View.GONE);
            }
        } else {
            binding.textAvaliacaoStatus.setText("Aguardando avaliação do mentor");
            binding.btnVerFeedback.setVisibility(View.GONE);
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
        // 1. Verificar permissão de localização
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Se não tiver permissão, solicite-a.
            // O ideal é usar um ActivityResultLauncher aqui, mas por simplicidade, notificamos.
            Toast.makeText(getContext(), "Permissão de localização é necessária para encontrar um mentor.", Toast.LENGTH_LONG).show();
            // Você pode adicionar um launcher para pedir a permissão aqui.
            return;
        }

        // 2. Verificar se o GPS está ligado
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS Desativado")
                    .setMessage("Para encontrar o mentor ideal, precisamos da sua localização. Por favor, ative o GPS.")
                    .setPositiveButton("Ativar GPS", (dialog, which) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        // 3. Se tudo estiver OK, iniciar a busca
        binding.btnProcurarMentor.setEnabled(false); // Desativa o botão para evitar cliques duplos

        firestoreHelper.realizarMatchmakingParaIdeia(requireContext(), ideia, r -> {
            // Garante que o fragmento ainda está "vivo" antes de atualizar a UI
            if (!isAdded()) {
                return;
            }

            binding.btnProcurarMentor.setEnabled(true);

            if (r.isOk() && r.data != null) {
                Toast.makeText(getContext(), "Sucesso! Um novo mentor foi vinculado à sua ideia.", Toast.LENGTH_LONG).show();
                // A UI será atualizada automaticamente pelo listener da Activity que hospeda este fragmento.
                // Se não houver listener, você precisaria chamar `updateUI()` aqui com a ideia atualizada.
            } else {
                // Mostra o diálogo que já criamos, dando a opção de tentar novamente.
                new AlertDialog.Builder(requireContext())
                        .setTitle("Nenhum Mentor Encontrado")
                        .setMessage("Não encontramos um mentor compatível no momento. Você pode tentar novamente mais tarde.")
                        .setPositiveButton("Entendi", null)
                        .show();
            }
        });
    }

    public void atualizarDadosIdeia(Ideia ideiaAtualizada) {
        this.ideia = ideiaAtualizada;
        // Se a view já foi criada, atualiza a UI imediatamente.
        if (binding != null) {
            updateUI();
        }
    }

 private void showFeedbackDialog() {
        if (!isAdded() || ideia == null) return;

        List<Avaliacao> avaliacoes = ideia.getAvaliacoes();

        if (avaliacoes == null || avaliacoes.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum detalhe de feedback encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Busca o nome do mentor para usar no título
        final String[] mentorNome = {"Mentor"}; // Fallback
        if (ideia.getMentorId() != null && !ideia.getMentorId().isEmpty()) {
            firestoreHelper.findMentorById(ideia.getMentorId(), r -> {
                if (r.isOk() && r.data != null) {
                    mentorNome[0] = r.data.getNome();
                }
                // Mostra o diálogo depois de tentar obter o nome
                exibirDialogoComFeedback(mentorNome[0], avaliacoes);
            });
        } else {
            exibirDialogoComFeedback(mentorNome[0], avaliacoes);
        }
    }

    private void exibirDialogoComFeedback(String mentorNome, List<Avaliacao> avaliacoes) {
        SpannableStringBuilder formattedFeedback = new SpannableStringBuilder();

        for (Avaliacao criterio : avaliacoes) {
            String header = criterio.getCriterio() + String.format(Locale.getDefault(), ": %.1f/10\n", criterio.getNota());
            int start = formattedFeedback.length();
            formattedFeedback.append(header);
            formattedFeedback.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, formattedFeedback.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (criterio.getFeedback() != null && !criterio.getFeedback().trim().isEmpty()) {
                formattedFeedback.append(criterio.getFeedback()).append("\n\n");
            } else {
                formattedFeedback.append("Nenhum feedback adicional.\n\n");
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Feedback de " + mentorNome)
                .setMessage(formattedFeedback)
                .setPositiveButton("Entendi", null)
                .show();
    }


    private void verificarPermissaoEGerarPdf() {
        if (getContext() == null) return; // Checagem de segurança

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                gerarPdf();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            gerarPdf();
        }
    }

    private void gerarPdf() {
        if (getContext() == null) return; // Checagem de segurança

        // Exibe um feedback para o usuário
        Toast.makeText(getContext(), "Gerando PDF...", Toast.LENGTH_SHORT).show();

        PdfGenerator.gerarCanvas(getContext(), ideia, (success, message) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!success) {
                        Toast.makeText(getContext(), "Erro: " + message, Toast.LENGTH_LONG).show();
                    }
                    // A mensagem de sucesso já é mostrada dentro do PdfGenerator.
                });
            }
        });
    }
}
