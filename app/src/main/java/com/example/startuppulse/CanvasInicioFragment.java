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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class CanvasInicioFragment extends Fragment {

    // Args
    private static final String ARG_IDEIA     = "ideia";
    private static final String ARG_ETAPA     = "etapa";
    private static final String ARG_READ_ONLY = "isReadOnly";
    private static final String KEY_POST_ITS_AREAS = "AREAS_IDEIA";
    // UI
    private EditText editTextNomeIdeia;
    private EditText editTextDescricaoIdeia;

    // Estado
    private Ideia ideia;
    private boolean isReadOnly = false;
    private CanvasEtapa etapa;
    private boolean suppressWatcher = false;

    // Debounce
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_MS = 300L;
    private final Runnable notifyRunnable = new Runnable() {
        @Override public void run() { notifyIfChanged(); }
    };
    private ChipGroup chipGroupAreas;

    // Callback p/ Activity
    public interface InicioStateListener {
        void onFieldsChanged(Ideia ideiaAtualizada);
    }
    private InicioStateListener listener;

    public static CanvasInicioFragment newInstance(Ideia ideia, CanvasEtapa etapa, boolean isReadOnly) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_IDEIA, ideia);
        args.putSerializable(ARG_ETAPA, etapa);
        args.putBoolean(ARG_READ_ONLY, isReadOnly);

        CanvasInicioFragment f = new CanvasInicioFragment();
        f.setArguments(args);
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

        Bundle args = getArguments();
        if (args != null) {
            ideia = (Ideia) args.getSerializable(ARG_IDEIA);
            etapa = (CanvasEtapa) args.getSerializable(ARG_ETAPA);
            isReadOnly = args.getBoolean(ARG_READ_ONLY, false);
        }

        chipGroupAreas = view.findViewById(R.id.chipGroupAreasInicio);

        // 1) Criar chips a partir do array de recursos
        String[] areas = getResources().getStringArray(R.array.areas_atuacao_opcoes);
        for (String area : areas) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(requireContext(), null,
                            com.google.android.material.R.attr.chipStyle);
            chip.setText(area);
            chip.setCheckable(true);
            chip.setClickable(!isReadOnly);
            chip.setEnabled(!isReadOnly);
            chip.setChipIconResource(R.drawable.ic_tag); // opcional, alinhado aos mentores
            chip.setChipIconTintResource(androidx.appcompat.R.color.material_grey_600);
            chip.setOnCheckedChangeListener((button, checked) -> {
                // feedback leve de validação conforme o usuário marca/desmarca
                if (!checked) return;
            });
            chipGroupAreas.addView(chip);
        }

        // 2) Reaplicar seleção pré-existente (se a ideia já tem as áreas salvas)
        if (ideia != null) {
            java.util.List<PostIt> atuais = ideia.getPostItsPorChave(KEY_POST_ITS_AREAS); // helper já existe em Ideia
            java.util.HashSet<String> marcadas = new java.util.HashSet<>();
            for (PostIt p : atuais) {
                if (p != null && p.getTexto() != null) marcadas.add(p.getTexto());
            }
            for (int i = 0; i < chipGroupAreas.getChildCount(); i++) {
                View child = chipGroupAreas.getChildAt(i);
                if (child instanceof com.google.android.material.chip.Chip) {
                    com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) child;
                    c.setChecked(marcadas.contains(String.valueOf(c.getText())));
                }
            }
        }

        editTextNomeIdeia = view.findViewById(R.id.editTextTituloIdeia);
        editTextDescricaoIdeia = view.findViewById(R.id.editTextDescricaoIdeia);
        chipGroupAreas = view.findViewById(R.id.chipGroupAreasInicio);

        // Preenche UI sem disparar watcher
        suppressWatcher = true;
        editTextNomeIdeia.setText(ideia.getNome());
        editTextDescricaoIdeia.setText(ideia.getDescricao());
        montarChipsAreas();
        suppressWatcher = false;

        // Read-only
        editTextNomeIdeia.setEnabled(!isReadOnly);
        editTextDescricaoIdeia.setEnabled(!isReadOnly);
        if (chipGroupAreas != null) chipGroupAreas.setEnabled(!isReadOnly);

        // Watchers (apenas se editável)
        if (!isReadOnly) addTextChangeListeners();
    }

    /** Retorna a lista de áreas selecionadas. */
    public java.util.List<String> getAreasSelecionadas() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < chipGroupAreas.getChildCount(); i++) {
            View child = chipGroupAreas.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) child;
                if (c.isChecked()) list.add(String.valueOf(c.getText()));
            }
        }
        return list;
    }

    /** Valida: exige pelo menos uma área marcada. Mostra Snackbar se inválido. */
    public boolean validateSelectionOrToast() {
        if (getAreasSelecionadas().isEmpty()) {
            com.google.android.material.snackbar.Snackbar
                    .make(chipGroupAreas, "Selecione pelo menos uma área de ajuda.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .show();
            chipGroupAreas.requestFocus();
            return false;
        }
        return true;
    }

    /** Persiste as áreas selecionadas dentro de postIts["AREAS_IDEIA"]. */
    public void persistAreasToIdeia() {
        if (ideia == null) return;

        java.util.List<String> selecionadas = getAreasSelecionadas();
        java.util.ArrayList<PostIt> postIts = new java.util.ArrayList<>();
        for (String a : selecionadas) {
            PostIt p = new PostIt();
            p.setTexto(a);
            p.setCor("#E0F7FA"); // cor suave p/ diferenciar tags de área
            p.setLastModified(new java.util.Date());
            postIts.add(p);
        }
        java.util.Map<String, java.util.List<PostIt>> mapa = ideia.getPostIts();
        if (mapa == null) {
            mapa = new java.util.HashMap<>();
            ideia.setPostIts(mapa);
        }
        mapa.put(KEY_POST_ITS_AREAS, postIts);
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

        // Chips: cada mudança agenda uma notificação (mesmo debounce)
        if (chipGroupAreas != null) {
            chipGroupAreas.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (suppressWatcher) return;
                handler.removeCallbacks(notifyRunnable);
                handler.postDelayed(notifyRunnable, DEBOUNCE_MS);
            });
        }
    }

    /** Compara e notifica a Activity somente quando houve mudança real. */
    private void notifyIfChanged() {
        if (listener == null || ideia == null) return;

        String novoNome = safe(editTextNomeIdeia.getText());
        String novaDesc = safe(editTextDescricaoIdeia.getText());
        java.util.List<String> novasAreas = coletarAreasSelecionadas();

        boolean mudou = notEquals(novoNome, ideia.getNome())
                || notEquals(novaDesc, ideia.getDescricao())
                || notEqualsListUnordered(novasAreas, ideia.getAreasNecessarias());
        if (mudou) {
            ideia.setNome(novoNome);
            ideia.setDescricao(novaDesc);
            ideia.setAreasNecessarias(novasAreas);
            listener.onFieldsChanged(ideia);
        }
    }

    private String safe(CharSequence cs) { return cs == null ? "" : cs.toString(); }
    private boolean notEquals(String a, String b) { return a == null ? b != null : !a.equals(b); }

    private boolean notEqualsListUnordered(java.util.List<String> a, java.util.List<String> b) {
        java.util.Set<String> sa = new java.util.HashSet<>(a == null ? java.util.Collections.emptyList() : a);
        java.util.Set<String> sb = new java.util.HashSet<>(b == null ? java.util.Collections.emptyList() : b);
        return !sa.equals(sb);
    }

    private void montarChipsAreas() {
        if (chipGroupAreas == null || getContext() == null) return;
        chipGroupAreas.removeAllViews();
        String[] areas = getResources().getStringArray(R.array.areas_predefinidas);
        java.util.Set<String> preSelecionadas = new java.util.HashSet<>();
        if (ideia.getAreasNecessarias() != null) preSelecionadas.addAll(ideia.getAreasNecessarias());

        for (String area : areas) {
            Chip chip = new Chip(getContext(), null, com.google.android.material.R.attr.chipStyle);
            chip.setText(area);
            chip.setCheckable(true);
            chip.setChecked(preSelecionadas.contains(area));
            chip.setChipIconResource(R.drawable.ic_tag);
            chip.setChipIconTintResource(androidx.appcompat.R.color.material_grey_600);
            chip.setIconStartPadding(6f);
            chip.setTextStartPadding(4f);
            chip.setEnabled(!isReadOnly);
            chipGroupAreas.addView(chip);
        }
    }
    private java.util.List<String> coletarAreasSelecionadas() {
        java.util.List<String> selecionadas = new java.util.ArrayList<>();
        if (chipGroupAreas == null) return selecionadas;
        for (int i = 0; i < chipGroupAreas.getChildCount(); i++) {
            View child = chipGroupAreas.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                selecionadas.add(String.valueOf(((Chip) child).getText()));
            }
        }
        return selecionadas;
    }

    /** Permite à Activity atualizar a ideia (ex.: ao receber do Firestore) sem loop. */
    public void bindIdeia(@NonNull Ideia atualizada) {
        this.ideia = atualizada;
        if (getView() == null) return;

        suppressWatcher = true;
        editTextNomeIdeia.setText(ideia.getNome());
        editTextDescricaoIdeia.setText(ideia.getDescricao());
        montarChipsAreas(); // reflete seleção vinda do Firestore/listener
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