package com.example.startuppulse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.startuppulse.common.Result;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog para adicionar/editar Post-it.
 * Mantém os IDs do layout dialog_add_postit.xml.
 * Compatível com FirestoreHelper baseado em Result<T>.
 */
public class AddPostItDialogFragment extends DialogFragment {

    private static final String ARG_IDEIA_ID      = "ideia_id";
    private static final String ARG_ETAPA_CHAVE    = "etapa_chave";
    private static final String ARG_POSTIT_ANTIGO  = "postit_antigo";

    private static final String STATE_TEXT         = "state_text";
    private static final String STATE_COLOR        = "state_color";
    private static final int    MAX_LEN_TEXT       = 280;

    private EditText editTextPostIt;
    private RadioGroup radioGroupColors;
    private MaterialButton btnSalvar, btnCancelar;
    private TextView tituloDialog;

    private String ideiaId;
    private String etapaChave;
    private PostIt postitParaEditar;   // null quando é adição
    private boolean isEditMode = false;

    private String corSelecionada = "#F9F871"; // default
    private FirestoreHelper firestoreHelper;

    public interface AddPostItListener {
        void onPostItAdded();
        void onPostItEdited(PostIt postitAntigo, String novoTexto, String novaCor);
    }

    // Fábrica: Adicionar
    public static AddPostItDialogFragment newInstanceForAdd(String ideiaId, String etapaChave) {
        AddPostItDialogFragment f = new AddPostItDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_IDEIA_ID, ideiaId);
        b.putString(ARG_ETAPA_CHAVE, etapaChave);
        f.setArguments(b);
        return f;
    }

    // Fábrica: Editar
    public static AddPostItDialogFragment newInstanceForEdit(String ideiaId, String etapaChave, PostIt postit) {
        AddPostItDialogFragment f = new AddPostItDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_IDEIA_ID, ideiaId);
        b.putString(ARG_ETAPA_CHAVE, etapaChave);
        b.putSerializable(ARG_POSTIT_ANTIGO, postit);
        f.setArguments(b);
        return f;
    }

    // Busca listener de forma segura (targetFragment é deprecated em versões recentes)
    @Nullable
    private AddPostItListener resolveListener() {
        Fragment tgt = getTargetFragment();
        if (tgt instanceof AddPostItListener) return (AddPostItListener) tgt;
        Fragment parent = getParentFragment();
        if (parent instanceof AddPostItListener) return (AddPostItListener) parent;
        if (getActivity() instanceof AddPostItListener) return (AddPostItListener) getActivity();
        return null;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreHelper = new FirestoreHelper();

        Bundle args = getArguments();
        if (args != null) {
            ideiaId     = args.getString(ARG_IDEIA_ID);
            etapaChave  = args.getString(ARG_ETAPA_CHAVE);
            postitParaEditar = (PostIt) args.getSerializable(ARG_POSTIT_ANTIGO);
            isEditMode  = (postitParaEditar != null);
        }

        setStyle(STYLE_NO_TITLE, 0);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_postit, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tituloDialog     = v.findViewById(R.id.text_dialog_title);
        editTextPostIt   = v.findViewById(R.id.edit_text_postit);
        radioGroupColors = v.findViewById(R.id.radio_group_colors);
        btnSalvar        = v.findViewById(R.id.btn_salvar_postit);
        btnCancelar      = v.findViewById(R.id.btn_cancelar_postit);

        // Limite de caracteres no campo de texto (coerente com counterMaxLength=280)
        editTextPostIt.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(MAX_LEN_TEXT) });

        // Restaura estado (rotações, etc.)
        if (savedInstanceState != null) {
            String restoredText  = savedInstanceState.getString(STATE_TEXT, "");
            String restoredColor = savedInstanceState.getString(STATE_COLOR, corSelecionada);
            editTextPostIt.setText(restoredText);
            setSelectedColor(restoredColor);
        } else if (isEditMode) {
            tituloDialog.setText("Editar Ponto-Chave");
            editTextPostIt.setText(safe(postitParaEditar.getTexto()));
            setSelectedColor(safeColor(postitParaEditar.getCor(), "#F9F871"));
        } else {
            tituloDialog.setText("Adicionar Ponto-Chave");
            setSelectedColor("#F9F871");
        }

        // Troca de cor
        radioGroupColors.setOnCheckedChangeListener((group, checkedId) -> {
            corSelecionada = colorForRadioId(checkedId);
        });

        btnCancelar.setOnClickListener(vw -> dismissAllowingStateLoss());
        btnSalvar.setOnClickListener(vw -> salvar());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TEXT, safe(editTextPostIt.getText()));
        outState.putString(STATE_COLOR, corSelecionada);
    }

    // ---------- Lógica ----------

    private void salvar() {
        String texto = safe(editTextPostIt.getText()).trim();
        if (TextUtils.isEmpty(texto)) {
            editTextPostIt.setError("O post-it não pode estar vazio.");
            return;
        }

        // evita duplo clique
        setButtonsEnabled(false);

        AddPostItListener callback = resolveListener();

        if (isEditMode) {
            // Somente notifica o host; quem edita no Firestore é o Fragment pai (CanvasBlockFragment)
            if (callback != null) callback.onPostItEdited(postitParaEditar, texto, corSelecionada);
            closeWithKeyboardHide();
        } else {
            // Adição -> salva direto no Firestore
            firestoreHelper.addPostitToIdeia(ideiaId, etapaChave, texto, corSelecionada, (Result<Void> r) -> {
                if (!isAdded()) return;
                if (r.isOk()) {
                    if (callback != null) callback.onPostItAdded();
                    closeWithKeyboardHide();
                } else {
                    Toast.makeText(requireContext(),
                            "Erro ao salvar: " + r.error.getMessage(),
                            Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                }
            });
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btnSalvar.setEnabled(enabled);
        btnCancelar.setEnabled(enabled);
        btnSalvar.setAlpha(enabled ? 1f : .6f);
        btnCancelar.setAlpha(enabled ? 1f : .6f);
    }

    private void closeWithKeyboardHide() {
        View current = getDialog() != null ? getDialog().getCurrentFocus() : null;
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
        dismissAllowingStateLoss();
    }

    // ---------- Utilitários de cor/seleção ----------

    private static final Map<Integer, String> ID_TO_COLOR = new HashMap<Integer, String>() {{
        put(R.id.radio_yellow, "#F9F871");
        put(R.id.radio_orange, "#FFC75F");
        put(R.id.radio_pink,   "#FF96AD");
        put(R.id.radio_blue,   "#84D2F6");
        put(R.id.radio_green,  "#A9F0D1");
        put(R.id.radio_white,  "#FFFFFF");
    }};
    private static final Map<String, Integer> COLOR_TO_ID = new HashMap<String, Integer>() {{
        put("#F9F871", R.id.radio_yellow);
        put("#FFC75F", R.id.radio_orange);
        put("#FF96AD", R.id.radio_pink);
        put("#84D2F6", R.id.radio_blue);
        put("#A9F0D1", R.id.radio_green);
        put("#FFFFFF", R.id.radio_white);
    }};

    private String colorForRadioId(int id) {
        String c = ID_TO_COLOR.get(id);
        return c != null ? c : "#F9F871";
    }

    private void setSelectedColor(@NonNull String color) {
        corSelecionada = color.toUpperCase();
        Integer id = COLOR_TO_ID.get(corSelecionada);
        radioGroupColors.check(id != null ? id : R.id.radio_yellow);
    }

    private String safe(CharSequence cs) { return cs == null ? "" : cs.toString(); }
    private String safeColor(String c, String def) { return TextUtils.isEmpty(c) ? def : c; }
}