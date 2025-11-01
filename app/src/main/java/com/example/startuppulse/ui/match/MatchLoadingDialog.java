package com.example.startuppulse.ui.match;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;

import com.example.startuppulse.R;
import com.example.startuppulse.databinding.DialogMatchLoadingBinding;

/**
 * Diálogo elegante usado durante o processo de "match" entre ideia e mentor.
 * Mostra um loading animado e atualiza a mensagem conforme o progresso.
 */
public class MatchLoadingDialog extends Dialog {

    private final DialogMatchLoadingBinding binding;
    private final Animation pulseAnim;
    private final Handler handler = new Handler();

    public MatchLoadingDialog(@NonNull Context context) {
        super(context);

        // Infla o layout via ViewBinding
        binding = DialogMatchLoadingBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        // Configuração visual
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCancelable(false);

        // Animação pulsante
        pulseAnim = AnimationUtils.loadAnimation(context, R.anim.pulse_fade);
        binding.iconPulse.startAnimation(pulseAnim);
    }

    /**
     * Atualiza a mensagem de status exibida no diálogo.
     */
    public void updateMessage(String message) {
        if (message == null) return;

        handler.post(() -> {
            binding.textMessage.setText(message);
            binding.textMessage.setAlpha(0f);
            binding.textMessage.animate().alpha(1f).setDuration(250).start();
        });
    }

    @Override
    public void show() {
        super.show();

        if (getWindow() != null) {
            // Centraliza e define margens e layout mais confortável
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (binding.iconPulse != null && pulseAnim != null) {
            binding.iconPulse.startAnimation(pulseAnim);
        }
    }



    @Override
    public void dismiss() {
        if (binding.iconPulse != null) {
            binding.iconPulse.clearAnimation();
        }
        super.dismiss();
    }
}
