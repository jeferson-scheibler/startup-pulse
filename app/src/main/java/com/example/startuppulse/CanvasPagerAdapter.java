package com.example.startuppulse;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class CanvasPagerAdapter extends FragmentStateAdapter {

    private final Ideia ideia;
    private final List<CanvasEtapa> etapas;
    private final boolean isReadOnly;

    public CanvasPagerAdapter(@NonNull FragmentActivity fa, Ideia ideia, List<CanvasEtapa> etapas, boolean isReadOnly) {
        super(fa);
        this.ideia = ideia;
        this.etapas = etapas;
        this.isReadOnly = isReadOnly;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        CanvasEtapa etapaAtual = etapas.get(position);

        switch (etapaAtual.getKey()) {
            case "INICIO":
                return CanvasInicioFragment.newInstance(ideia, etapaAtual, isReadOnly);
            case "AMBIENTE_CHECK":
                return new AmbienteCheckFragment();
            case "FINAL":
                return new CanvasFinalFragment();
            case "STATUS": // <-- NOVO CASE para a tela de status
                return IdeiaStatusFragment.newInstance(ideia);
            default: // Para todos os outros blocos do canvas
                return CanvasBlockFragment.newInstance(ideia, etapaAtual, isReadOnly);
        }
    }

    @Override
    public int getItemCount() {
        return etapas.size();
    }
}