package com.example.startuppulse;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Map;

public class IdeiaStatusFragment extends Fragment {

    private Ideia ideia;

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
        if (getArguments() != null) {
            ideia = (Ideia) getArguments().getSerializable("ideia");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ideia_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ideia == null) return;

        // Mapeamento dos componentes
        TextView textIdeiaTitle = view.findViewById(R.id.text_ideia_title);
        TextView textIdeiaDescription = view.findViewById(R.id.text_ideia_description);
        LottieAnimationView lottieAnimation = view.findViewById(R.id.lottie_status_animation);

        MaterialCardView cardMentor = view.findViewById(R.id.card_mentor);
        ImageView iconMentor = view.findViewById(R.id.icon_mentor);
        TextView textMentorName = view.findViewById(R.id.text_mentor_name);

        MaterialCardView cardAvaliacao = view.findViewById(R.id.card_avaliacao);
        ImageView iconAvaliacao = view.findViewById(R.id.icon_avaliacao);
        TextView textAvaliacaoStatus = view.findViewById(R.id.text_avaliacao_status);
        MaterialButton btnVerFeedback = view.findViewById(R.id.btn_ver_feedback);

        // Preenche as informações básicas
        textIdeiaTitle.setText(ideia.getNome());
        textIdeiaDescription.setText(ideia.getDescricao());

        // Lógica para o status do Mentor
        boolean hasMentor = (ideia.getMentorId() != null && !ideia.getMentorId().isEmpty());
        if (hasMentor) {
            int colorActive = ContextCompat.getColor(requireContext(), R.color.primary_color);
            cardMentor.setStrokeColor(colorActive);
            iconMentor.setImageTintList(ColorStateList.valueOf(colorActive));
            // Busca o nome do mentor...
            FirestoreHelper firestoreHelper = new FirestoreHelper();
            firestoreHelper.findMentorById(ideia.getMentorId(), new FirestoreHelper.MentorListener() {
                @Override
                public void onMentorEncontrado(Mentor mentor) {
                    textMentorName.setText(mentor.getNome());
                }
                @Override
                public void onNenhumMentorEncontrado() { textMentorName.setText("Mentor não encontrado"); }
                @Override
                public void onError(Exception e) { textMentorName.setText("Erro de conexão"); }
            });
        } else {
            textMentorName.setText("A procurar o mentor ideal...");
        }

        // Lógica para o status da Avaliação
        boolean isAvaliada = "Avaliada".equals(ideia.getAvaliacaoStatus());
        if (isAvaliada) {
            int colorActive = ContextCompat.getColor(requireContext(), R.color.green_success);
            cardAvaliacao.setStrokeColor(colorActive);
            iconAvaliacao.setImageTintList(ColorStateList.valueOf(colorActive));
            textAvaliacaoStatus.setText("Ideia Avaliada!");

            if (ideia.getAvaliacoes() != null && !ideia.getAvaliacoes().isEmpty()) {
                btnVerFeedback.setVisibility(View.VISIBLE);
                btnVerFeedback.setOnClickListener(v -> showFeedbackDialog());
            }
        } else {
            textAvaliacaoStatus.setText("Aguardando avaliação");
        }

        // Define a animação principal
        if (isAvaliada) {
            lottieAnimation.setAnimation(R.raw.anim_sucess); // Animação de troféu/sucesso
        } else if (hasMentor) {
            lottieAnimation.setAnimation(R.raw.anim_analise_mentor); // Animação de análise
        } else {
            lottieAnimation.setAnimation(R.raw.anim_mapa_procurando); // Animação de busca
        }
        lottieAnimation.playAnimation();
    }

    private void showFeedbackDialog() {
        if (getContext() == null || ideia.getAvaliacoes() == null || ideia.getAvaliacoes().isEmpty()) return;

        SpannableStringBuilder formattedFeedback = new SpannableStringBuilder();

        for (Avaliacao avaliacao : ideia.getAvaliacoes()) {
            String criterio = avaliacao.getCriterio();
            double nota = avaliacao.getNota();
            String feedbackTexto = avaliacao.getFeedback();

            if (criterio != null) {
                // Adiciona o título do critério em negrito
                String criterioTitle = criterio + ": " + nota + "/10\n";
                int start = formattedFeedback.length();
                formattedFeedback.append(criterioTitle);
                formattedFeedback.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, formattedFeedback.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Adiciona o feedback em texto normal
                if (feedbackTexto != null && !feedbackTexto.trim().isEmpty()) {
                    formattedFeedback.append(feedbackTexto + "\n\n");
                } else {
                    formattedFeedback.append("Nenhum feedback específico.\n\n");
                }
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Feedback do Mentor")
                .setMessage(formattedFeedback)
                .setPositiveButton("Entendi", null)
                .show();
    }
}