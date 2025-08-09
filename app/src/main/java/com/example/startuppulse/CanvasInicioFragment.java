package com.example.startuppulse;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragmento para a primeira página do Canvas, onde o usuário define o nome e a descrição da ideia.
 */
public class CanvasInicioFragment extends Fragment {

    private Ideia ideia;
    private EditText editTextNomeIdeia, editTextDescricaoIdeia;
    private InicioStateListener listener;
    private boolean isReadOnly = false;

    /**
     * Interface para notificar a Activity sobre mudanças nos campos.
     */
    public interface InicioStateListener {
        void onFieldsChanged(Ideia ideiaAtualizada);
    }

    /**
     * Método fábrica atualizado para aceitar o objeto CanvasEtapa, mantendo a consistência.
     */
    public static CanvasInicioFragment newInstance(Ideia ideia, CanvasEtapa etapa, boolean isReadOnly) {
        CanvasInicioFragment fragment = new CanvasInicioFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia);
        args.putSerializable("etapa", etapa);
        args.putBoolean("isReadOnly", isReadOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof InicioStateListener) {
            listener = (InicioStateListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement InicioStateListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ideia = (Ideia) getArguments().getSerializable("ideia");
            isReadOnly = getArguments().getBoolean("isReadOnly");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_canvas_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextNomeIdeia = view.findViewById(R.id.editTextTituloIdeia);
        editTextDescricaoIdeia = view.findViewById(R.id.editTextDescricaoIdeia);

        if (ideia != null) {
            editTextNomeIdeia.setText(ideia.getNome());
            editTextDescricaoIdeia.setText(ideia.getDescricao());
        }
        if (isReadOnly) {
            editTextNomeIdeia.setEnabled(false);
            editTextDescricaoIdeia.setEnabled(false);
        } else {
            addTextChangeListeners();
        }
        addTextChangeListeners();
    }

    private void addTextChangeListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ideia != null && listener != null) {
                    ideia.setNome(editTextNomeIdeia.getText().toString());
                    ideia.setDescricao(editTextDescricaoIdeia.getText().toString());
                    listener.onFieldsChanged(ideia);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editTextNomeIdeia.addTextChangedListener(textWatcher);
        editTextDescricaoIdeia.addTextChangedListener(textWatcher);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}