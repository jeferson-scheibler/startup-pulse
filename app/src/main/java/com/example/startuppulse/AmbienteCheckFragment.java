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
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AmbienteCheckFragment extends Fragment implements SensorEventListener {

    // Enum para controlar os estados da UI
    private enum State { INSTRUCTIONS, CALIBRATING, RESULTS }
    private State currentState = State.INSTRUCTIONS;

    public interface AmbienteCheckListener {
        void onAmbienteIdealDetectado(boolean isIdeal);
        void onPularCheck();
    }
    private static final long CALIBRATION_DURATION_MS = 20000; // 20 segundos

    private AmbienteCheckListener listener;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private MediaRecorder mediaRecorder;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String tempAudioFile = null;

    private final List<Float> lightReadings = new ArrayList<>();
    private final List<Double> soundReadings = new ArrayList<>();

    // Componentes da UI
    private TextView textTitle, statusLuz, statusSom, textResultRecommendation;
    private LottieAnimationView lottieAnimation;
    private ProgressBar progressBar;
    private MaterialCardView cardIndicadores;
    private ImageView iconLuz, iconSom;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AmbienteCheckListener) {
            listener = (AmbienteCheckListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AmbienteCheckListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) startSoundMeter();
        });
        tempAudioFile = requireContext().getExternalCacheDir().getAbsolutePath() + "/temp_audio_check.3gp";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ambiente_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textTitle = view.findViewById(R.id.text_view_title);
        lottieAnimation = view.findViewById(R.id.lottie_animation);
        progressBar = view.findViewById(R.id.progress_bar_calibration);
        cardIndicadores = view.findViewById(R.id.card_indicadores);
        statusLuz = view.findViewById(R.id.status_luz);
        statusSom = view.findViewById(R.id.status_som);
        iconLuz = view.findViewById(R.id.icon_luz);
        iconSom = view.findViewById(R.id.icon_som);
        textResultRecommendation = view.findViewById(R.id.text_overall_recommendation);
        MaterialButton btnPular = view.findViewById(R.id.btn_pular_check);
        btnPular.setOnClickListener(v -> listener.onPularCheck());

        updateUiForState(State.INSTRUCTIONS);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        stopSoundMeter();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (currentState == State.CALIBRATING && event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightReadings.add(event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void startCalibration() {
        lightReadings.clear();
        soundReadings.clear();
        updateUiForState(State.CALIBRATING);

        if (lightSensor != null) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        checkAudioPermissionAndStart();

        handler.post(soundSamplingRunnable);
        handler.postDelayed(this::processResults, CALIBRATION_DURATION_MS); // Termina a calibração após a duração definida
        handler.post(progressBarUpdater); // Inicia a atualização da barra de progresso
    }

    private void processResults() {
        handler.removeCallbacks(soundSamplingRunnable);
        sensorManager.unregisterListener(this);
        stopSoundMeter();

        double avgLight = lightReadings.stream().mapToDouble(f -> f).average().orElse(0.0);
        double avgSound = soundReadings.stream().mapToDouble(d -> d).average().orElse(0.0);

        boolean isLightIdeal = (avgLight >= 50 && avgLight < 400);
        boolean isSoundIdeal = (avgSound >= 500 && avgSound < 8000);
        boolean isOverallIdeal = isLightIdeal && isSoundIdeal;

        updateUiForState(State.RESULTS);
        updateResultsUI(isLightIdeal, isSoundIdeal, avgLight, avgSound);
        listener.onAmbienteIdealDetectado(true);
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
                handler.postDelayed(this::startCalibration, 5000);
                break;
            case CALIBRATING:
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0); // Reinicia a barra de progresso
                cardIndicadores.setVisibility(View.GONE);
                textResultRecommendation.setVisibility(View.GONE);
                textTitle.setText("A analisar o espaço... Mova o telemóvel.");
                lottieAnimation.setAnimation(R.raw.anim_ambiente_scanning);
                lottieAnimation.playAnimation();
                break;
            case RESULTS:
                progressBar.setVisibility(View.GONE);
                cardIndicadores.setVisibility(View.VISIBLE);
                textResultRecommendation.setVisibility(View.VISIBLE);
                textTitle.setText("Análise Concluída");
                break;
        }
    }

    private void updateResultsUI(boolean isLightIdeal, boolean isSoundIdeal, double avgLight, double avgSound) {
        int lightColor = ContextCompat.getColor(requireContext(), isLightIdeal ? R.color.green_success : R.color.white_translucent);
        iconLuz.setImageTintList(ColorStateList.valueOf(lightColor));
        statusLuz.setTextColor(lightColor);
        statusLuz.setText(isLightIdeal ? "Ideal" : (avgLight < 50 ? "Baixa" : "Alta"));

        int soundColor = ContextCompat.getColor(requireContext(), isSoundIdeal ? R.color.green_success : R.color.white_translucent);
        iconSom.setImageTintList(ColorStateList.valueOf(soundColor));
        statusSom.setTextColor(soundColor);
        statusSom.setText(isSoundIdeal ? "Ideal" : (avgSound < 500 ? "Silencioso" : "Barulhento"));

        if (isLightIdeal && isSoundIdeal) {
            lottieAnimation.setAnimation(R.raw.anim_ambiente_ideal);
            textResultRecommendation.setText("Ambiente perfeito! Deixe as ideias fluírem.");
        } else {
            lottieAnimation.setAnimation(R.raw.anim_ambiente_distracao);
            textResultRecommendation.setText("O seu ambiente não é o ideal, mas não há problema. Boas ideias podem surgir em qualquer lugar!");
        }
        lottieAnimation.playAnimation();
    }

    private void checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSoundMeter();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startSoundMeter() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(tempAudioFile);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void stopSoundMeter() {
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); mediaRecorder.release(); } catch (Exception e) { e.printStackTrace(); }
            mediaRecorder = null;
            new File(tempAudioFile).delete();
        }
    }

    private final Runnable soundSamplingRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentState == State.CALIBRATING && mediaRecorder != null) {
                soundReadings.add((double) mediaRecorder.getMaxAmplitude());
                handler.postDelayed(this, 250);
            }
        }
    };

    // NOVO: Runnable para atualizar a barra de progresso ao longo do tempo
    private final Runnable progressBarUpdater = new Runnable() {
        @Override
        public void run() {
            if (currentState == State.CALIBRATING) {
                long elapsedTime = 0;
                // Este é um truque para obter o tempo de forma mais precisa
                final long startTime = System.currentTimeMillis();

                // Atualiza a cada 100ms
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        long elapsedTime = now - startTime;
                        int progress = (int) ((elapsedTime * 100) / CALIBRATION_DURATION_MS);
                        progressBar.setProgress(Math.min(progress, 100));

                        if (elapsedTime < CALIBRATION_DURATION_MS) {
                            new Handler(Looper.getMainLooper()).postDelayed(this, 100);
                        }
                    }
                });
            }
        }
    };
}
