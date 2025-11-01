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
import com.example.startuppulse.databinding.DialogRematchLoadingBinding;

/**
 * Diálogo usado para refazer a busca de mentores (re-match),
 * exibido quando o usuário deseja tentar novamente o processo.
 */
public class ReMatchDialog extends Dialog {

    private final DialogRematchLoadingBinding binding;
    private final Animation pulseAnim;
    private final Handler handler = new Handler();
    private OnCancelListener cancelListener;

    public ReMatchDialog(@NonNull Context context) {
        super(context);

        binding = DialogRematchLoadingBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        // Fundo transparente e não cancelável por toque externo
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCancelable(false);

        // Animação pulsante
        pulseAnim = AnimationUtils.loadAnimation(context, R.anim.pulse_fade);
        binding.iconPulse.startAnimation(pulseAnim);

        // Listener do botão "Cancelar"
        binding.btnCancelar.setOnClickListener(v -> {
            if (cancelListener != null) cancelListener.onCancel();
            dismiss();
        });
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }

    /**
     * Atualiza a mensagem do diálogo.
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

    /**
     * Interface de callback para cancelar a busca.
     */
    public interface OnCancelListener {
        void onCancel();
    }
}
