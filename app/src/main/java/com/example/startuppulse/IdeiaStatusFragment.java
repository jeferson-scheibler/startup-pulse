package com.example.startuppulse;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.startuppulse.databinding.FragmentIdeiaStatusBinding; // Importar o ViewBinding

import java.util.Locale;

/**
 * Fragmento para exibir o status de uma ideia publicada, incluindo o mentor
 * associado e o estado da avaliação.
 */
public class IdeiaStatusFragment extends Fragment {

    private FragmentIdeiaStatusBinding binding; // Objeto de ViewBinding
    private Ideia ideia;
    private FirestoreHelper firestoreHelper;

    // Factory method atualizado para receber o objeto Ideia completo
    public static IdeiaStatusFragment newInstance(Ideia ideia) {
        IdeiaStatusFragment fragment = new IdeiaStatusFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia); // Passa o objeto inteiro
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
            // Se não houver dados, mostra um estado de erro ou simplesmente não faz nada.
            return;
        }
        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Previne memory leaks
    }

    /**
     * Centraliza toda a lógica de atualização da UI.
     */
    private void updateUI() {
        // --- Dados Básicos da Ideia ---
        binding.textIdeiaTitle.setText(ideia.getNome());
        binding.textIdeiaDescription.setText(ideia.getDescricao());

        // --- Status do Mentor ---
        boolean hasMentor = ideia.getMentorId() != null && !ideia.getMentorId().isEmpty();
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
        }

        // --- Status da Avaliação ---
        boolean isAvaliada = "Avaliada".equals(ideia.getAvaliacaoStatus());
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

    /**
     * Mostra o dialog com o feedback formatado.
     * Esta lógica foi simplificada para usar o modelo AvaliacaoCompleta.
     */
    private void showFeedbackDialog() {
        if (!isAdded() || ideia.getAvaliacoes() == null || ideia.getAvaliacoes().isEmpty()) return;

        // Assumindo que a lista contém objetos AvaliacaoCompleta
        AvaliacaoCompleta ultimaAvaliacao = ideia.getAvaliacoes().get(ideia.getAvaliacoes().size() - 1);
        SpannableStringBuilder formattedFeedback = new SpannableStringBuilder();

        for (Avaliacao criterio : ultimaAvaliacao.getCriteriosAvaliados()) {
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
                .setTitle("Feedback de " + ultimaAvaliacao.getMentorNome())
                .setMessage(formattedFeedback)
                .setPositiveButton("Entendi", null)
                .show();
    }
}