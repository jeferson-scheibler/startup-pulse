package com.example.startuppulse.util;

import android.graphics.*;
import androidx.annotation.NonNull;

public class AvatarUtils {
    public static Bitmap circleLetter(@NonNull String name, int sizePx, int bgColor, int textColor) {
        String letter = "?";
        if (name != null && !name.trim().isEmpty()) {
            letter = name.trim().substring(0, 1).toUpperCase();
        }
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bgColor);
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);

        p.setColor(textColor);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(sizePx * 0.46f);
        Paint.FontMetrics fm = p.getFontMetrics();
        float y = sizePx / 2f - (fm.ascent + fm.descent) / 2f;

        c.drawText(letter, sizePx / 2f, y, p);
        return bmp;
    }
}