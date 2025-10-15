package com.example.startuppulse.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.PostIt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class PdfGenerator {

    // --- Constantes de Layout e Estilo ---
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 30;
    private static final int CORNER_RADIUS = 8;

    // Cores (inspiradas no seu app)
    private static final int COLOR_PRIMARY_TEXT = Color.parseColor("#1A237E"); // Um azul escuro
    private static final int COLOR_SECONDARY_TEXT = Color.parseColor("#546E7A"); // Um cinza azulado
    private static final int COLOR_BORDER = Color.parseColor("#CFD8DC"); // Um cinza claro

    public interface PdfGenerationListener {
        void onPdfGenerated(boolean success, String message);
    }

    public static void gerarCanvas(Context context, Ideia ideia, PdfGenerationListener listener) {
        if (ideia == null) {
            listener.onPdfGenerated(false, "Objeto Ideia nulo.");
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        desenharCanvasNoPdf(page.getCanvas(), ideia);

        document.finishPage(page);
        salvarPdf(context, document, "Canvas_" + ideia.getNome().replaceAll("[^a-zA-Z0-9]", "_"), listener);
    }

    private static void desenharCanvasNoPdf(Canvas canvas, Ideia ideia) {
        // --- Configuração dos Pincéis (Paint) ---
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(COLOR_BORDER);
        borderPaint.setStrokeWidth(2);
        borderPaint.setAntiAlias(true);

        TextPaint mainTitlePaint = new TextPaint();
        mainTitlePaint.setColor(COLOR_PRIMARY_TEXT);
        mainTitlePaint.setTextSize(22);
        mainTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mainTitlePaint.setTextAlign(Paint.Align.CENTER);
        mainTitlePaint.setAntiAlias(true);

        TextPaint blockTitlePaint = new TextPaint();
        blockTitlePaint.setColor(COLOR_PRIMARY_TEXT);
        blockTitlePaint.setTextSize(11);
        blockTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        blockTitlePaint.setAntiAlias(true);

        TextPaint contentPaint = new TextPaint();
        contentPaint.setColor(COLOR_SECONDARY_TEXT);
        contentPaint.setTextSize(9);
        contentPaint.setAntiAlias(true);

        // --- Desenhar Título Principal ---
        canvas.drawText(ideia.getNome(), PAGE_WIDTH / 2f, MARGIN + 10, mainTitlePaint);

        // --- Definição das áreas dos blocos ---
        int contentWidth = PAGE_WIDTH - 2 * MARGIN;
        int contentHeight = PAGE_HEIGHT - (int)(MARGIN * 2.8);
        int topY = MARGIN + 45;

        int colWidth = contentWidth / 5;
        int rowHeight = (int) (contentHeight * 0.6);
        int bottomRowHeight = contentHeight - rowHeight;

        // Coordenadas dos blocos
        Rect parcerias = new Rect(MARGIN, topY, MARGIN + colWidth, topY + rowHeight);
        Rect atividades = new Rect(parcerias.right, topY, parcerias.right + colWidth, topY + rowHeight / 2);
        Rect recursos = new Rect(atividades.left, atividades.bottom, atividades.right, parcerias.bottom);
        Rect proposta = new Rect(atividades.right, topY, atividades.right + colWidth, topY + rowHeight);
        Rect relacionamento = new Rect(proposta.right, topY, proposta.right + colWidth, topY + rowHeight / 2);
        Rect canais = new Rect(relacionamento.left, relacionamento.bottom, relacionamento.right, parcerias.bottom);
        Rect segmentos = new Rect(relacionamento.right, topY, relacionamento.right + colWidth, topY + rowHeight);
        Rect custos = new Rect(MARGIN, parcerias.bottom, MARGIN + contentWidth / 2, parcerias.bottom + bottomRowHeight);
        Rect receitas = new Rect(custos.right, parcerias.bottom, custos.right + contentWidth / 2, custos.bottom);

        // --- Desenhar cada bloco ---
        drawTextBlock(canvas, "Parcerias-chave", formatPostIts(ideia.getPostItsPorChave("PARCERIAS_PRINCIPAIS")), parcerias, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Atividades-chave", formatPostIts(ideia.getPostItsPorChave("ATIVIDADES_CHAVE")), atividades, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Recursos-chave", formatPostIts(ideia.getPostItsPorChave("RECURSOS_PRINCIPAIS")), recursos, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Proposta de valor", formatPostIts(ideia.getPostItsPorChave("PROPOSTA_VALOR")), proposta, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Relacionamento com clientes", formatPostIts(ideia.getPostItsPorChave("RELACIONAMENTO_CLIENTES")), relacionamento, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Canais", formatPostIts(ideia.getPostItsPorChave("CANAIS")), canais, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Segmentos de clientes", formatPostIts(ideia.getPostItsPorChave("SEGMENTO_CLIENTES")), segmentos, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Estrutura de custos", formatPostIts(ideia.getPostItsPorChave("ESTRUTURA_CUSTOS")), custos, borderPaint, blockTitlePaint, contentPaint);
        drawTextBlock(canvas, "Fontes de receita", formatPostIts(ideia.getPostItsPorChave("FONTES_RENDA")), receitas, borderPaint, blockTitlePaint, contentPaint);
    }

    private static void drawTextBlock(Canvas canvas, String title, String content, Rect bounds, Paint borderPaint, TextPaint titlePaint, TextPaint contentPaint) {
        // Desenha a borda arredondada do bloco
        canvas.drawRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, CORNER_RADIUS, CORNER_RADIUS, borderPaint);

        int padding = 12;
        int textWidth = bounds.width() - 2 * padding;
        if (textWidth <= 0) return; // Evita erros se a área for muito pequena

        canvas.save();
        canvas.translate(bounds.left + padding, bounds.top + padding);

        // **CORREÇÃO DA QUEBRA DE LINHA DO TÍTULO**
        // Desenha o título usando StaticLayout para permitir quebra de linha
        StaticLayout titleLayout = StaticLayout.Builder.obtain(title, 0, title.length(), titlePaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();
        titleLayout.draw(canvas);

        // Move o canvas para baixo do título para desenhar o conteúdo
        canvas.translate(0, titleLayout.getHeight() + 5); // +5 para um pequeno espaçamento

        // **CORREÇÃO DO CONTEÚDO**
        // Desenha o conteúdo usando StaticLayout
        if (content != null && !content.isEmpty()) {
            StaticLayout contentLayout = StaticLayout.Builder.obtain(content, 0, content.length(), contentPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build();
            contentLayout.draw(canvas);
        }

        canvas.restore();
    }

    private static String formatPostIts(List<PostIt> postIts) {
        if (postIts == null || postIts.isEmpty()) {
            return ""; // Retorna string vazia se não houver post-its
        }
        // Formata cada post-it com um marcador de bolinha e espaçamento
        String bullet = "\u2022 "; // Caractere de bolinha (bullet point)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return postIts.stream()
                    .map(postIt -> bullet + postIt.getTexto())
                    .collect(Collectors.joining("\n\n"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < postIts.size(); i++) {
                sb.append(bullet).append(postIts.get(i).getTexto());
                if (i < postIts.size() - 1) {
                    sb.append("\n\n");
                }
            }
            return sb.toString();
        }
    }

    private static void salvarPdf(Context context, PdfDocument document, String nomeArquivo, PdfGenerationListener listener) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, nomeArquivo + ".pdf");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        }
        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                document.writeTo(outputStream);
                // Apenas notifica o listener. O Fragment/Activity decide se mostra o Toast.
                listener.onPdfGenerated(true, uri.toString());
            } catch (IOException e) {
                e.printStackTrace();
                listener.onPdfGenerated(false, "Erro ao salvar o PDF.");
            }
        } else {
            listener.onPdfGenerated(false, "Não foi possível criar o arquivo PDF.");
        }
        document.close();
    }
}