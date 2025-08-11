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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.startuppulse.common.Result;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
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
        if (ideia == null || !isAdded()) return;

        // Views
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

        // Dados básicos
        textIdeiaTitle.setText(ideia.getNome());
        textIdeiaDescription.setText(ideia.getDescricao());

        // Status do Mentor
        boolean hasMentor = ideia.getMentorId() != null && !ideia.getMentorId().isEmpty();
        if (hasMentor) {
            int colorActive = ContextCompat.getColor(requireContext(), R.color.primary_color);
            cardMentor.setStrokeColor(colorActive);
            iconMentor.setImageTintList(ColorStateList.valueOf(colorActive));

            new FirestoreHelper().findMentorById(ideia.getMentorId(), r -> {
                if (!isAdded()) return;
                if (r.isOk() && r.data != null) {
                    textMentorName.setText(r.data.getNome());
                } else if (r.isOk()) {
                    textMentorName.setText("Mentor não encontrado");
                } else {
                    textMentorName.setText("Erro de conexão");
                }
            });

        } else {
            textMentorName.setText("A procurar o mentor ideal...");
        }

        // Status da Avaliação
        boolean isAvaliada = "Avaliada".equals(ideia.getAvaliacaoStatus());
        if (isAvaliada) {
            int colorActive = ContextCompat.getColor(requireContext(), R.color.green_success);
            cardAvaliacao.setStrokeColor(colorActive);
            iconAvaliacao.setImageTintList(ColorStateList.valueOf(colorActive));
            textAvaliacaoStatus.setText("Ideia Avaliada!");

            List<?> avals = ideia.getAvaliacoes();
            if (avals != null && !avals.isEmpty()) {
                btnVerFeedback.setVisibility(View.VISIBLE);
                btnVerFeedback.setOnClickListener(v -> showFeedbackDialog());
            } else {
                btnVerFeedback.setVisibility(View.GONE);
            }
        } else {
            textAvaliacaoStatus.setText("Aguardando avaliação");
            btnVerFeedback.setVisibility(View.GONE);
        }

        // Animação
        if (isAvaliada) {
            lottieAnimation.setAnimation(R.raw.anim_sucess);
        } else if (hasMentor) {
            lottieAnimation.setAnimation(R.raw.anim_analise_mentor);
        } else {
            lottieAnimation.setAnimation(R.raw.anim_mapa_procurando);
        }
        lottieAnimation.playAnimation();
    }

    private void showFeedbackDialog() {
        if (!isAdded() || ideia.getAvaliacoes() == null || ideia.getAvaliacoes().isEmpty()) return;

        SpannableStringBuilder formatted = new SpannableStringBuilder();

        for (Object item : ideia.getAvaliacoes()) {
            String criterio = null;
            String feedback = null;
            double nota = -1;

            // Aceita tanto Avaliacao (objeto) quanto Map (persistido pelo Firestore)
            if (item instanceof Avaliacao) {
                Avaliacao av = (Avaliacao) item;
                criterio = av.getCriterio();
                feedback = av.getFeedback();
                nota = av.getNota();
            } else if (item instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) item;
                Object c = m.get("criterio");
                Object f = m.get("feedback");
                Object n = m.get("nota");
                if (c instanceof String) criterio = (String) c;
                if (f instanceof String) feedback = (String) f;
                if (n instanceof Number) nota = ((Number) n).doubleValue();
            }

            if (criterio == null) continue;

            String header = criterio + (nota >= 0 ? (": " + nota + "/10") : "") + "\n";
            int start = formatted.length();
            formatted.append(header);
            formatted.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, formatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (feedback != null && !feedback.trim().isEmpty()) {
                formatted.append(feedback).append("\n\n");
            } else {
                formatted.append("Nenhum feedback específico.\n\n");
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Feedback do Mentor")
                .setMessage(formatted)
                .setPositiveButton("Entendi", null)
                .show();
    }
}
