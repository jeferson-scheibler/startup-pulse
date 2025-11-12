package com.example.startuppulse.ui.vortex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.startuppulse.R;

/**
 * Fragmento placeholder para a nova aba "Vórtex".
 * Ele apenas infla o layout 'fragment_vortex.xml'.
 */
public class VortexFragment extends Fragment {

    public VortexFragment() {
        // Construtor público vazio obrigatório
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout XML do placeholder
        return inflater.inflate(R.layout.fragment_vortex, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // A lógica do mapa (MapLibre) entrará aqui na Etapa 3
    }
}