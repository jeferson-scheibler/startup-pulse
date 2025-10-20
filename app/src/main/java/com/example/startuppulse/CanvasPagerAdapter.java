package com.example.startuppulse;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.startuppulse.data.CanvasEtapa;
import com.example.startuppulse.data.models.Ideia;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para gerenciar as páginas (etapas) da criação e edição de uma Ideia.
 * Este adapter foi refatorado para ser mais flexível e desacoplado do estado inicial.
 */
public class CanvasPagerAdapter extends FragmentStateAdapter {

    // O adapter agora gerencia sua própria lista de etapas e o estado da ideia.
    private List<CanvasEtapa> etapas = new ArrayList<>();
    private Ideia ideia;
    private boolean isReadOnly;
    public CanvasPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public void setEtapas(List<CanvasEtapa> etapas) {
        this.etapas = (etapas != null) ? etapas : new ArrayList<>();
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // A lógica de criação de fragmentos permanece a mesma.
        CanvasEtapa etapa = etapas.get(position);
        String chave = etapa.getChave();

        switch (chave) {
            case CanvasEtapa.CHAVE_INICIO:
                return new CanvasInicioFragment();
            case CanvasEtapa.CHAVE_EQUIPE:
                return new CanvasEquipeFragment();
            case CanvasEtapa.CHAVE_FINAL:
                return new CanvasFinalFragment();
            case CanvasEtapa.CHAVE_STATUS:
                return new IdeiaStatusFragment();
            case CanvasEtapa.CHAVE_AMBIENTE_CHECK:
                return AmbienteCheckFragment.newInstance();
            default:
                return CanvasBlockFragment.newInstance(chave);
        }
    }

    @Override
    public int getItemCount() {
        return etapas.size();
    }
}