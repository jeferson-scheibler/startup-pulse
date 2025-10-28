package com.example.startuppulse.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils; // Para interpolação de cores

import com.example.startuppulse.R; // Importe seu R

import java.util.Locale;

public class PulseVoteView extends View {

    private static final String TAG = "PulseVoteView";
    private static final float MIN_SCORE = 1.0f;
    private static final float MAX_SCORE = 5.0f;

    // --- Atributos Configuráveis ---
    private int pulseColorStart;
    private int pulseColorEnd;
    private int centerColor;
    private int textColor;
    private float minRadiusPx;
    private float maxRadiusMultiplier; // Será calculado em onSizeChanged
    private float centerRadiusPx;
    private float pulseStrokeWidthPx;
    private float textSizePx;
    private float currentScore; // Nota atual (1.0 a 5.0)
    private boolean showPulseWaves;
    private int numberOfWaves;

    // --- Variáveis Internas ---
    private Paint centerPaint;
    private Paint pulsePaint;
    private Paint textPaint;
    private float currentRadiusPx; // Raio atual do toque/pulso
    private float maxRadiusPx;     // Raio máximo calculado
    private float centerX, centerY;
    private boolean isDragging = false;
    private Rect textBounds = new Rect(); // Para centralizar o texto
    private Paint idlePulsePaint;
    private ValueAnimator pulseAnimator;
    private float animatedPulseRadius = 0f;
    private float averageScoreRadius = 0f; // Raio correspondente à média da comunidade
    private int idlePulseColor; // Cor para o pulso ambiente

    // --- Listener ---
    private OnVoteListener onVoteListener;

    // --- Construtores ---
    public PulseVoteView(Context context) {
        super(context);
        init(context, null);
    }

    public PulseVoteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PulseVoteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    // --- Inicialização ---
    private void init(Context context, @Nullable AttributeSet attrs) {
        // Valores padrão
        pulseColorStart = Color.parseColor("#FFD54F"); // Amarelo suave
        pulseColorEnd = Color.parseColor("#4FC3F7");   // Azul claro vibrante
        centerColor = Color.parseColor("#E0E0E0");     // Cinza claro
        textColor = Color.BLACK;
        minRadiusPx = dpToPx(20);
        maxRadiusMultiplier = 0.85f; // 85% do menor lado da view
        centerRadiusPx = dpToPx(15);
        pulseStrokeWidthPx = dpToPx(2);
        textSizePx = spToPx(24);
        currentScore = MIN_SCORE;
        showPulseWaves = false; // Começar com gradiente
        numberOfWaves = 5;
        idlePulseColor = pulseColorStart; // Começa com a cor inicial por padrão
        idlePulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        idlePulsePaint.setStyle(Paint.Style.STROKE);
        // Use uma espessura um pouco maior ou diferente para destacar
        idlePulsePaint.setStrokeWidth(pulseStrokeWidthPx * 1.5f);
        idlePulsePaint.setColor(idlePulseColor);

        // Carrega atributos do XML, se existirem
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PulseVoteView, 0, 0);
            try {
                pulseColorStart = ta.getColor(R.styleable.PulseVoteView_pulseColorStart, pulseColorStart);
                pulseColorEnd = ta.getColor(R.styleable.PulseVoteView_pulseColorEnd, pulseColorEnd);
                centerColor = ta.getColor(R.styleable.PulseVoteView_centerColor, centerColor);
                textColor = ta.getColor(R.styleable.PulseVoteView_textColor, textColor);
                minRadiusPx = ta.getDimension(R.styleable.PulseVoteView_minRadius, minRadiusPx);
                maxRadiusMultiplier = ta.getFloat(R.styleable.PulseVoteView_maxRadiusMultiplier, maxRadiusMultiplier);
                centerRadiusPx = ta.getDimension(R.styleable.PulseVoteView_centerRadius, centerRadiusPx);
                pulseStrokeWidthPx = ta.getDimension(R.styleable.PulseVoteView_pulseStrokeWidth, pulseStrokeWidthPx);
                textSizePx = ta.getDimension(R.styleable.PulseVoteView_textSize, textSizePx);
                currentScore = ta.getFloat(R.styleable.PulseVoteView_initialScore, currentScore);
                showPulseWaves = ta.getBoolean(R.styleable.PulseVoteView_showPulseWaves, showPulseWaves);
                numberOfWaves = ta.getInt(R.styleable.PulseVoteView_numberOfWaves, numberOfWaves);
            } finally {
                ta.recycle();
            }
        }

        // Valida a nota inicial
        currentScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore));
        currentRadiusPx = scoreToRadius(currentScore); // Define o raio inicial com base na nota

        // Configura os Paints
        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(centerColor);

        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (showPulseWaves) {
            pulsePaint.setStyle(Paint.Style.STROKE);
            pulsePaint.setStrokeWidth(pulseStrokeWidthPx);
        } else {
            pulsePaint.setStyle(Paint.Style.FILL);
        }
        // A cor/shader será definida em onDraw/onTouchEvent

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSizePx);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true); // Deixa o número destacado
        setupPulseAnimator();
    }

    private void setupPulseAnimator() {
        // Anima um valor de 0 a 1 e vice-versa
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f, 0f);
        pulseAnimator.setDuration(1500); // Duração total do pulso (ex: 1.5 segundos)
        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator()); // Variação suave

        pulseAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue(); // Valor entre 0 e 1
            // Calcula o raio animado: começa no raio da média e expande/contrai um pouco
            float pulseRange = dpToPx(5); // O quanto o pulso expande/contrai (ex: 5dp)
            animatedPulseRadius = averageScoreRadius + (pulseRange * fraction);

            // Calcula a opacidade (alpha): mais visível no meio da animação (quando fraction=1)
            int alpha = (int) (150 * fraction); // Máximo de ~60% de opacidade
            idlePulsePaint.setAlpha(Math.max(0, Math.min(255, alpha)));

            invalidate(); // Redesenha a cada frame da animação
        });
    }

    // --- Medidas ---
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Tenta fazer a view quadrada baseada na menor dimensão
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = Math.min(width, height);
        setMeasuredDimension(size, size); // Força a ser quadrada
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Calcula o centro e o raio máximo quando o tamanho da view muda
        centerX = w / 2f;
        centerY = h / 2f;
        // O raio máximo é uma porcentagem do menor lado (que agora é 'w' ou 'h')
        maxRadiusPx = (Math.min(w, h) / 2f) * maxRadiusMultiplier;

        averageScoreRadius = scoreToRadius(getAverageScore());
        currentRadiusPx = scoreToRadius(currentScore);

        // Reconfigura o shader do gradiente se estiver usando
        if (!showPulseWaves) {
            setupGradientShader();
        }

        if (averageScoreRadius > 0 && !isDragging) { // Só inicia se houver média e não estiver arrastando
            startPulseAnimation();
        }
    }

    // --- Desenho ---
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pulseAnimator != null && pulseAnimator.isRunning() && !isDragging && animatedPulseRadius > centerRadiusPx) {
            idlePulsePaint.setColor(idlePulseColor); // Define a cor base (alpha é definido no animator)
            canvas.drawCircle(centerX, centerY, animatedPulseRadius, idlePulsePaint);
        }

        // 1. Desenha o pulso (ondas ou gradiente)
        if (currentRadiusPx > centerRadiusPx && isDragging) { // <<< Só desenha se estiver arrastando
            if (showPulseWaves) {
                drawPulseWaves(canvas);
            } else {
                drawPulseGradient(canvas);
            }
        }

        // 2. Desenha o círculo central (sempre visível)
        canvas.drawCircle(centerX, centerY, centerRadiusPx, centerPaint);

        // 3. Desenha o texto da nota atual
        String scoreText = String.format(Locale.getDefault(), "%.1f", currentScore);
        textPaint.getTextBounds(scoreText, 0, scoreText.length(), textBounds);
        float textY = centerY - textBounds.exactCenterY(); // Centraliza verticalmente
        canvas.drawText(scoreText, centerX, textY, textPaint);
    }

    private void drawPulseGradient(Canvas canvas) {
        // A cor do shader já deve ter sido atualizada no onTouchEvent
        // ou init/onSizeChanged
        if (pulsePaint.getShader() != null) {
            canvas.drawCircle(centerX, centerY, currentRadiusPx, pulsePaint);
        }
    }

    private void setupGradientShader() {
        // Cria um gradiente radial suave
        RadialGradient gradient = new RadialGradient(
                centerX, centerY, currentRadiusPx, // O raio do gradiente é o raio atual
                interpolateColor(currentRadiusPx), // Cor no centro (mais intensa)
                ColorUtils.setAlphaComponent(interpolateColor(currentRadiusPx), 0), // Transparente na borda
                Shader.TileMode.CLAMP);
        pulsePaint.setShader(gradient);
    }

    private void drawPulseWaves(Canvas canvas) {
        if (numberOfWaves <= 0) return;

        float waveSpacing = (currentRadiusPx - centerRadiusPx) / numberOfWaves;
        for (int i = 1; i <= numberOfWaves; i++) {
            float waveRadius = centerRadiusPx + (i * waveSpacing);
            // Calcula a opacidade (alpha) - mais opaco perto do centro
            int alpha = (int) (255 * (1f - (float)(i-1) / numberOfWaves));
            // Interpola a cor com base no raio da onda atual
            int waveColor = interpolateColor(waveRadius);
            pulsePaint.setColor(ColorUtils.setAlphaComponent(waveColor, alpha));
            canvas.drawCircle(centerX, centerY, waveRadius, pulsePaint);
        }
    }

    // --- Interação ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float distance = calculateDistanceFromCenter(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Inicia o arraste apenas se o toque for próximo ao centro
                if (distance <= minRadiusPx * 1.5) { // Área de toque um pouco maior que o minRadius
                    isDragging = true;
                    stopPulseAnimation();
                    updatePulse(distance);
                    getParent().requestDisallowInterceptTouchEvent(true); // Impede ScrollView de roubar o evento
                    return true; // Indica que consumimos o evento DOWN
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    updatePulse(distance);
                    return true; // Indica que consumimos o evento MOVE
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false); // Libera o ScrollView

                    // Confirma o voto
                    float finalScore = radiusToScore(currentRadiusPx);
                    Log.d(TAG, "Voto confirmado: " + finalScore);
                    if (onVoteListener != null) {
                        onVoteListener.onVoteConfirmed(finalScore);
                    }

                    if (averageScoreRadius > 0) {
                        startPulseAnimation();
                    }

                    return true; // Indica que consumimos o evento UP/CANCEL
                }
                break;
        }
        // Se não consumimos o evento, deixa ele passar para outras views
        return super.onTouchEvent(event);
    }

    // Atualiza o raio, a nota e redesenha
    private void updatePulse(float distance) {
        // Limita o raio entre min e max
        currentRadiusPx = Math.max(minRadiusPx, Math.min(distance, maxRadiusPx));
        // Converte o raio para a nota
        currentScore = radiusToScore(currentRadiusPx);

        // Atualiza a cor/shader do Paint ANTES de invalidar
        if (!showPulseWaves) {
            setupGradientShader(); // Recria o gradiente com o novo raio e cor
        }
        // else: A cor das ondas será calculada em drawPulseWaves

        invalidate(); // Pede para redesenhar a view
    }

    public void setAverageScore(float averageScore) {
        // Garante que a nota média está nos limites
        averageScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, averageScore));

        // Calcula o raio correspondente à média
        if (maxRadiusPx > 0) { // Só calcula se a view já foi medida
            this.averageScoreRadius = scoreToRadius(averageScore);
            // Define a cor base para o pulso ambiente com base na média
            this.idlePulseColor = interpolateColor(this.averageScoreRadius);

            // Inicia ou reinicia a animação se não estiver arrastando
            if (!isDragging) {
                startPulseAnimation();
            }
        } else {
            // Se a view ainda não foi medida, armazena a média para usar em onSizeChanged
            // (Poderia usar uma variável temporária se necessário, mas averageScoreRadius já serve)
            // Nota: O cálculo inicial de averageScoreRadius em onSizeChanged usará isso
            this.averageScoreRadius = scoreToRadius(averageScore); // Calcula mesmo assim para ter valor
            this.idlePulseColor = interpolateColor(this.averageScoreRadius);
        }
    }

    // <<< NOVOS Helpers para controlar a animação >>>
    private void startPulseAnimation() {
        if (pulseAnimator != null && !pulseAnimator.isRunning() && averageScoreRadius > 0) {
            Log.d(TAG, "Iniciando animação de pulso ambiente no raio: " + averageScoreRadius);
            // Define a cor antes de iniciar
            idlePulsePaint.setColor(idlePulseColor);
            pulseAnimator.start();
        }
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            Log.d(TAG, "Parando animação de pulso ambiente.");
            pulseAnimator.cancel(); // Use cancel() para parar imediatamente
            animatedPulseRadius = 0f; // Reseta o raio animado para não desenhar
            invalidate(); // Limpa o pulso da tela
        }
    }

    // Helper para obter a média (usado em onSizeChanged) - simplificado
    private float getAverageScore() {
        // Retorna a nota correspondente ao raio já calculado
        return radiusToScore(this.averageScoreRadius);
    }

    // --- Cálculos e Helpers ---
    private float calculateDistanceFromCenter(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // Converte um raio (entre minRadiusPx e maxRadiusPx) para uma nota (1.0 a 5.0)
    private float radiusToScore(float radius) {
        if (maxRadiusPx <= minRadiusPx) return MIN_SCORE; // Evita divisão por zero
        // Garante que o raio está nos limites antes de calcular
        radius = Math.max(minRadiusPx, Math.min(radius, maxRadiusPx));
        float ratio = (radius - minRadiusPx) / (maxRadiusPx - minRadiusPx);
        return MIN_SCORE + (MAX_SCORE - MIN_SCORE) * ratio;
    }

    // Converte uma nota (1.0 a 5.0) para um raio (entre minRadiusPx e maxRadiusPx)
    private float scoreToRadius(float score) {
        // Garante que a nota está nos limites
        score = Math.max(MIN_SCORE, Math.min(score, MAX_SCORE));
        float ratio = (score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE);
        return minRadiusPx + (maxRadiusPx - minRadiusPx) * ratio;
    }

    // Interpola a cor entre start e end com base no raio atual
    private int interpolateColor(float radius) {
        if (maxRadiusPx <= minRadiusPx) return pulseColorStart; // Evita divisão por zero
        float ratio = (radius - minRadiusPx) / (maxRadiusPx - minRadiusPx);
        // Garante que ratio esteja entre 0.0 e 1.0
        ratio = Math.max(0f, Math.min(1f, ratio));
        return ColorUtils.blendARGB(pulseColorStart, pulseColorEnd, ratio);
    }

    // Converte DP para Pixels
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // Converte SP para Pixels
    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }


    // --- Listener Interface ---
    public interface OnVoteListener {
        void onVoteConfirmed(float score);
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.onVoteListener = listener;
    }

    // --- Métodos Públicos (Exemplo: para definir a nota externamente) ---
    public void setCurrentScore(float score) {
        this.currentScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
        // Calcula o raio correspondente à nova nota
        // Certifique-se de que maxRadiusPx já foi calculado (onSizeChanged)
        if (maxRadiusPx > 0) {
            this.currentRadiusPx = scoreToRadius(this.currentScore);
            if (!showPulseWaves) {
                setupGradientShader(); // Atualiza gradiente se necessário
            }
            invalidate(); // Redesenha com a nova nota/raio
        } else {
            // Adia a atualização do raio para onSizeChanged se ainda não foi medido
            // A nota (currentScore) já está atualizada
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Reinicia a animação quando a view fica visível (se houver média)
        if (averageScoreRadius > 0 && !isDragging) {
            startPulseAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Para a animação para evitar leaks quando a view é destruída
        stopPulseAnimation();
        if (pulseAnimator != null) {
            pulseAnimator.removeAllUpdateListeners(); // Limpa listeners
            pulseAnimator = null;
        }
    }
}