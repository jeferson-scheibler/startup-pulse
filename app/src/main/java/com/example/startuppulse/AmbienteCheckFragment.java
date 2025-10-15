package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AmbienteCheckFragment extends DialogFragment implements SensorEventListener {

    public interface AmbienteCheckListener {
        void onAmbienteIdealDetectado(boolean isIdeal);
        void onPularCheck();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // Opcional: remove o fundo padrão para que o seu layout brilhe
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private enum State { INSTRUCTIONS, CALIBRATING, RESULTS }
    private State currentState = State.INSTRUCTIONS;

    private static final long CALIBRATION_DURATION_MS = 20_000L; // 20s
    private static final long SOUND_SAMPLE_MS = 250L;
    private static final long PROGRESS_TICK_MS = 100L;

    private AmbienteCheckListener listener;

    // Sensores / áudio
    private SensorManager sensorManager;
    @Nullable private Sensor lightSensor;
    @Nullable private MediaRecorder mediaRecorder;
    private String tempAudioFile;

    // Leituras
    private final List<Float> lightReadings = new ArrayList<>();
    private final List<Double> soundReadings = new ArrayList<>();
    private boolean soundEnabled = false; // ganha true se a permissão for concedida e o recorder iniciar

    // UI
    private TextView textTitle, statusLuz, statusSom, textResultRecommendation;
    private LottieAnimationView lottieAnimation;
    private ProgressBar progressBar;
    private MaterialCardView cardIndicadores;
    private ImageView iconLuz, iconSom;
    private MaterialButton btnPular;

    // Handlers
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startMillis = 0L;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public static AmbienteCheckFragment newInstance() {
        return new AmbienteCheckFragment();
    }

    // Runnables
    private final Runnable soundSamplingRunnable = new Runnable() {
        @Override public void run() {
            if (currentState == State.CALIBRATING && mediaRecorder != null) {
                try {
                    // 0..32767 (aprox). Mantemos amplitude bruta — thresholds abaixo consideram isso.
                    soundReadings.add((double) mediaRecorder.getMaxAmplitude());
                } catch (Exception ignored) { /* evita crash se recorder entrou em estado inválido */ }
                handler.postDelayed(this, SOUND_SAMPLE_MS);
            }
        }
    };

    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (currentState != State.CALIBRATING) return;
            long elapsed = System.currentTimeMillis() - startMillis;
            int progress = (int) Math.min(100, (elapsed * 100f / CALIBRATION_DURATION_MS));
            progressBar.setProgress(progress);
            if (elapsed < CALIBRATION_DURATION_MS) {
                handler.postDelayed(this, PROGRESS_TICK_MS);
            }
        }
    };

    @Override public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AmbienteCheckListener) {
            listener = (AmbienteCheckListener) context;
        } else {
            throw new IllegalStateException(context + " must implement AmbienteCheckListener");
        }
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        tempAudioFile = new File(requireContext().getCacheDir(), "temp_audio_check.3gp").getAbsolutePath();

        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        startSoundMeter();
                    } else {
                        soundEnabled = false; // segue só com luz
                    }
                });
    }

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ambiente_check, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        textTitle = v.findViewById(R.id.text_view_title);
        lottieAnimation = v.findViewById(R.id.lottie_animation);
        progressBar = v.findViewById(R.id.progress_bar_calibration);
        progressBar.setMax(100);
        cardIndicadores = v.findViewById(R.id.card_indicadores);
        statusLuz = v.findViewById(R.id.status_luz);
        statusSom = v.findViewById(R.id.status_som);
        iconLuz = v.findViewById(R.id.icon_luz);
        iconSom = v.findViewById(R.id.icon_som);
        textResultRecommendation = v.findViewById(R.id.text_overall_recommendation);
        btnPular = v.findViewById(R.id.btn_pular_check);

        btnPular.setOnClickListener(view -> {
            if (listener != null) listener.onPularCheck();
        });

        // A11y
        iconLuz.setContentDescription("Indicador de luz");
        iconSom.setContentDescription("Indicador de ruído");
        btnPular.setContentDescription("Pular verificação de ambiente");

        updateUiForState(State.INSTRUCTIONS);
    }

    @Override public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopSoundMeter();
        handler.removeCallbacksAndMessages(null);
    }

    // ======= Sensores =======
    @Override public void onSensorChanged(SensorEvent event) {
        if (currentState == State.CALIBRATING && event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightReadings.add(event.values[0]);
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ======= Fluxo =======
    private void startCalibration() {
        lightReadings.clear();
        soundReadings.clear();
        soundEnabled = false;

        updateUiForState(State.CALIBRATING);

        // Luz (se existir)
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            statusLuz.setText("Indisponível");
        }

        // Áudio (permite continuar mesmo que negado)
        checkAudioPermissionAndStart();

        // Agenda amostragem e progresso
        startMillis = System.currentTimeMillis();
        handler.post(soundSamplingRunnable);
        handler.post(progressTick);

        // Termina ao final do período
        handler.postDelayed(this::processResults, CALIBRATION_DURATION_MS);
    }

    private void processResults() {
        // Limpa agendamentos e sensores
        handler.removeCallbacks(soundSamplingRunnable);
        handler.removeCallbacks(progressTick);
        sensorManager.unregisterListener(this);
        stopSoundMeter();

        // Médias (se lista vazia → 0.0)
        double avgLight = lightReadings.stream().mapToDouble(f -> f).average().orElse(0.0);
        double avgSound = soundReadings.stream().mapToDouble(d -> d).average().orElse(0.0);

        // Heurísticas simples (mantive teus thresholds)
        boolean lightAvailable = lightSensor != null;
        boolean soundAvailable = !soundReadings.isEmpty() && soundEnabled;

        boolean isLightIdeal = lightAvailable && (avgLight >= 50 && avgLight < 400);
        boolean isSoundIdeal = soundAvailable && (avgSound >= 500 && avgSound < 8000);

        boolean isOverallIdeal;
        if (lightAvailable && soundAvailable) {
            isOverallIdeal = isLightIdeal && isSoundIdeal;
        } else if (lightAvailable) {
            isOverallIdeal = isLightIdeal; // cai para o que temos
        } else if (soundAvailable) {
            isOverallIdeal = isSoundIdeal;
        } else {
            isOverallIdeal = false; // sem dados, melhor ser conservador
        }

        updateUiForState(State.RESULTS);
        updateResultsUI(lightAvailable, soundAvailable, isLightIdeal, isSoundIdeal, avgLight, avgSound);

        if (listener != null) listener.onAmbienteIdealDetectado(isOverallIdeal);
    }

    private void updateUiForState(State newState) {
        currentState = newState;
        switch (newState) {
            case INSTRUCTIONS:
                progressBar.setVisibility(View.GONE);
                cardIndicadores.setVisibility(View.GONE);
                textResultRecommendation.setVisibility(View.GONE);

                textTitle.setText("Vamos calibrar o seu ambiente");
                lottieAnimation.setAnimation(R.raw.anim_instrucao_movimento);
                lottieAnimation.playAnimation();

                // Dá 3s para o usuário se situar e inicia
                handler.postDelayed(this::startCalibration, 3000);
                break;

            case CALIBRATING:
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                cardIndicadores.setVisibility(View.GONE);
                textResultRecommendation.setVisibility(View.GONE);

                textTitle.setText("Analisando o espaço... mova o telemóvel.");
                lottieAnimation.setAnimation(R.raw.anim_ambiente_scanning);
                lottieAnimation.playAnimation();
                break;

            case RESULTS:
                progressBar.setVisibility(View.GONE);
                cardIndicadores.setVisibility(View.VISIBLE);
                textResultRecommendation.setVisibility(View.VISIBLE);

                textTitle.setText("Análise concluída");
                break;
        }
    }

    private void updateResultsUI(boolean lightAvailable,
                                 boolean soundAvailable,
                                 boolean isLightIdeal,
                                 boolean isSoundIdeal,
                                 double avgLight,
                                 double avgSound) {

        // Luz
        if (lightAvailable) {
            int lightColor = ContextCompat.getColor(requireContext(),
                    isLightIdeal ? R.color.green_success : R.color.white_translucent);
            iconLuz.setImageTintList(ColorStateList.valueOf(lightColor));
            statusLuz.setTextColor(lightColor);
            statusLuz.setText(isLightIdeal ? "Ideal" : (avgLight < 50 ? "Baixa" : "Alta"));
        } else {
            iconLuz.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white_translucent)));
            statusLuz.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_translucent));
            statusLuz.setText("Indisponível");
        }

        // Som
        if (soundAvailable) {
            int soundColor = ContextCompat.getColor(requireContext(),
                    isSoundIdeal ? R.color.green_success : R.color.white_translucent);
            iconSom.setImageTintList(ColorStateList.valueOf(soundColor));
            statusSom.setTextColor(soundColor);
            statusSom.setText(isSoundIdeal ? "Ideal" : (avgSound < 500 ? "Silencioso" : "Barulhento"));
        } else {
            iconSom.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white_translucent)));
            statusSom.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_translucent));
            statusSom.setText("Indisponível");
        }

        // Recomendações e animação final
        boolean ok = (lightAvailable ? isLightIdeal : true) && (soundAvailable ? isSoundIdeal : true);
        if (ok) {
            lottieAnimation.setAnimation(R.raw.anim_ambiente_ideal);
            textResultRecommendation.setText("Ambiente perfeito! Deixe as ideias fluírem.");
        } else {
            lottieAnimation.setAnimation(R.raw.anim_ambiente_distracao);
            textResultRecommendation.setText("Seu ambiente não está ideal agora. Tudo bem — ajuste o que puder ou siga assim mesmo.");
        }
        lottieAnimation.playAnimation();
    }

    // ======= Áudio =======
    private void checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startSoundMeter();
        } else {
            // pede permissão; se negar, seguimos sem som
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startSoundMeter() {
        stopSoundMeter(); // garante estado limpo
        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(tempAudioFile);
            mediaRecorder.prepare();
            mediaRecorder.start();
            soundEnabled = true;
        } catch (Exception e) {
            soundEnabled = false;
            safeReleaseRecorder();
            Toast.makeText(requireContext(), "Não foi possível iniciar captura de áudio.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopSoundMeter() {
        if (mediaRecorder == null) return;
        try { mediaRecorder.stop(); } catch (Exception ignored) {}
        safeReleaseRecorder();
        // limpa arquivo temporário
        try { new File(tempAudioFile).delete(); } catch (Exception ignored) {}
    }

    private void safeReleaseRecorder() {
        try { mediaRecorder.reset(); } catch (Exception ignored) {}
        try { mediaRecorder.release(); } catch (Exception ignored) {}
        mediaRecorder = null;
    }
}