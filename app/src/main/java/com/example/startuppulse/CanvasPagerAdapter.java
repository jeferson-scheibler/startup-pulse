package com.example.startuppulse;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;

/**
 * Adapter para gerenciar as páginas (etapas) da criação e edição de uma Ideia.
 * Este adapter é responsável por criar o fragmento correto para cada etapa.
 */
public class CanvasPagerAdapter extends FragmentStateAdapter {

    private final List<CanvasEtapa> etapas;
    private final Ideia ideia;
    private final boolean isReadOnly;

    public CanvasPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<CanvasEtapa> etapas, Ideia ideia, boolean isReadOnly) {
        super(fragmentActivity);
        this.etapas = etapas;
        this.ideia = ideia;
        this.isReadOnly = isReadOnly;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        CanvasEtapa etapa = etapas.get(position);
        String chave = etapa.getChave();

        switch (chave) {
            case CanvasEtapa.CHAVE_INICIO:
                return CanvasInicioFragment.newInstance(ideia, isReadOnly);
            case CanvasEtapa.CHAVE_EQUIPE:
                return CanvasEquipeFragment.newInstance(ideia, isReadOnly);
            case CanvasEtapa.CHAVE_FINAL:
                return CanvasFinalFragment.newInstance(ideia, isReadOnly);

            // Etapas que precisam de um ID específico
            case CanvasEtapa.CHAVE_STATUS:
                return IdeiaStatusFragment.newInstance(ideia.getId());

            // Etapas independentes que não precisam de dados
            case CanvasEtapa.CHAVE_AMBIENTE_CHECK:
                return AmbienteCheckFragment.newInstance();

            // O caso padrão para todos os blocos do Business Model Canvas
            default:
                return CanvasBlockFragment.newInstance(etapa, chave, ideia, isReadOnly);
        }
    }

    @Override
    public int getItemCount() {
        return etapas.size();
    }
}