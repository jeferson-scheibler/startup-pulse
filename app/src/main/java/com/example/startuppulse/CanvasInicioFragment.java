package com.example.startuppulse;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CanvasInicioFragment extends Fragment {

    // Args
    private static final String ARG_IDEIA     = "ideia";
    private static final String ARG_ETAPA     = "etapa";       // recebido só por consistência
    private static final String ARG_READ_ONLY = "isReadOnly";

    // UI
    private EditText editTextNomeIdeia;
    private EditText editTextDescricaoIdeia;

    // Estado
    private Ideia ideia;
    private boolean isReadOnly = false;
    private boolean suppressWatcher = false;

    // Debounce
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_MS = 300L;
    private final Runnable notifyRunnable = new Runnable() {
        @Override public void run() { notifyIfChanged(); }
    };

    // Callback p/ Activity
    public interface InicioStateListener {
        void onFieldsChanged(Ideia ideiaAtualizada);
    }
    private InicioStateListener listener;

    public static CanvasInicioFragment newInstance(Ideia ideia, CanvasEtapa etapa, boolean isReadOnly) {
        CanvasInicioFragment f = new CanvasInicioFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_IDEIA, ideia);
        b.putSerializable(ARG_ETAPA, etapa);
        b.putBoolean(ARG_READ_ONLY, isReadOnly);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof InicioStateListener) {
            listener = (InicioStateListener) context;
        } else {
            throw new RuntimeException(context + " must implement InicioStateListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            ideia = (Ideia) args.getSerializable(ARG_IDEIA);
            isReadOnly = args.getBoolean(ARG_READ_ONLY, false);
        }
        if (ideia == null) ideia = new Ideia(); // segurança
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_canvas_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextNomeIdeia = view.findViewById(R.id.editTextTituloIdeia);
        editTextDescricaoIdeia = view.findViewById(R.id.editTextDescricaoIdeia);

        // Preenche UI sem disparar watcher
        suppressWatcher = true;
        editTextNomeIdeia.setText(ideia.getNome());
        editTextDescricaoIdeia.setText(ideia.getDescricao());
        suppressWatcher = false;

        // Read-only
        editTextNomeIdeia.setEnabled(!isReadOnly);
        editTextDescricaoIdeia.setEnabled(!isReadOnly);

        // Watchers (apenas se editável)
        if (!isReadOnly) addTextChangeListeners();
    }

    private void addTextChangeListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressWatcher) return;
                // Debounce: reagenda notificação
                handler.removeCallbacks(notifyRunnable);
                handler.postDelayed(notifyRunnable, DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        editTextNomeIdeia.addTextChangedListener(watcher);
        editTextDescricaoIdeia.addTextChangedListener(watcher);
    }

    /** Compara e notifica a Activity somente quando houve mudança real. */
    private void notifyIfChanged() {
        if (listener == null || ideia == null) return;

        String novoNome = safe(editTextNomeIdeia.getText());
        String novaDesc = safe(editTextDescricaoIdeia.getText());

        boolean mudou = notEquals(novoNome, ideia.getNome()) || notEquals(novaDesc, ideia.getDescricao());
        if (mudou) {
            ideia.setNome(novoNome);
            ideia.setDescricao(novaDesc);
            listener.onFieldsChanged(ideia);
        }
    }

    private String safe(CharSequence cs) { return cs == null ? "" : cs.toString(); }
    private boolean notEquals(String a, String b) { return a == null ? b != null : !a.equals(b); }

    /** Permite à Activity atualizar a ideia (ex.: ao receber do Firestore) sem loop. */
    public void bindIdeia(@NonNull Ideia atualizada) {
        this.ideia = atualizada;
        if (getView() == null) return;

        suppressWatcher = true;
        editTextNomeIdeia.setText(ideia.getNome());
        editTextDescricaoIdeia.setText(ideia.getDescricao());
        suppressWatcher = false;
    }

    /** Caso precise alternar o modo leitura em runtime. */
    public void setReadOnly(boolean ro) {
        isReadOnly = ro;
        if (getView() == null) return;
        editTextNomeIdeia.setEnabled(!isReadOnly);
        editTextDescricaoIdeia.setEnabled(!isReadOnly);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(notifyRunnable);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}