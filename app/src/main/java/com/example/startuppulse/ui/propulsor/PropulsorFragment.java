package com.example.startuppulse.ui.propulsor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.startuppulse.R;

/**
 * Fragmento placeholder para a nova aba "Propulsor".
 * Ele apenas infla o layout 'fragment_propulsor.xml'.
 */
public class PropulsorFragment extends Fragment {

    public PropulsorFragment() {
        // Construtor público vazio obrigatório
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout XML do placeholder
        return inflater.inflate(R.layout.fragment_propulsor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // A lógica do chat com IA entrará aqui na Etapa 2
    }
}