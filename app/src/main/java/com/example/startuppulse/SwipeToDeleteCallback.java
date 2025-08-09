package com.example.startuppulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Callback simplificado para o gesto de deslizar para excluir.
 * Comunica-se diretamente com o IdeiasAdapter.
 */
public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final IdeiasAdapter adapter;
    private final Drawable icon;
    private final ColorDrawable background;

    // O construtor agora recebe o adapter diretamente
    public SwipeToDeleteCallback(Context context, IdeiasAdapter adapter) {
        super(0, ItemTouchHelper.LEFT); // Permite deslizar apenas para a esquerda
        this.adapter = adapter;
        this.icon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        this.background = new ColorDrawable(Color.parseColor("#B71C1C")); // Vermelho escuro
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // Não precisamos da funcionalidade de mover/arrastar
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Pega a posição do item que foi deslizado
        int position = viewHolder.getAdapterPosition();
        // Chama o método no adapter para iniciar o processo de exclusão
        adapter.iniciarExclusao(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 20;

        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        if (dX < 0) { // Deslizando para a esquerda
            int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        } else { // Não deslizando
            background.setBounds(0, 0, 0, 0);
        }

        background.draw(c);
        icon.draw(c);
    }
}