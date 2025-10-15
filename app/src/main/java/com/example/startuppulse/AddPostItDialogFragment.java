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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider; // Import necessário

import com.example.startuppulse.data.PostIt;
import com.example.startuppulse.databinding.DialogAddPostitBinding; // Import para View Binding
import com.example.startuppulse.ui.canvas.CanvasIdeiaViewModel; // Import do ViewModel
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddPostItDialogFragment extends DialogFragment {

    private static final String ARG_ETAPA_CHAVE    = "etapa_chave";
    private static final String ARG_POSTIT_ANTIGO  = "postit_antigo";
    private static final int    MAX_LEN_TEXT       = 280;

    // MODIFICAÇÃO 1: Usar View Binding para segurança e conveniência
    private DialogAddPostitBinding binding;
    private CanvasIdeiaViewModel sharedViewModel;

    private String etapaChave;
    private PostIt postitParaEditar;
    private boolean isEditMode = false;
    private String corSelecionada = "#F9F871"; // Cor padrão

    // A interface do listener foi completamente removida.

    // Fábrica: Adicionar
    public static AddPostItDialogFragment newInstanceForAdd(String etapaChave) {
        AddPostItDialogFragment f = new AddPostItDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ETAPA_CHAVE, etapaChave);
        f.setArguments(b);
        return f;
    }

    // Fábrica: Editar
    public static AddPostItDialogFragment newInstanceForEdit(String etapaChave, PostIt postit) {
        AddPostItDialogFragment f = new AddPostItDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ETAPA_CHAVE, etapaChave);
        b.putSerializable(ARG_POSTIT_ANTIGO, postit);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // O FirestoreHelper foi removido.
        Bundle args = getArguments();
        if (args != null) {
            etapaChave  = args.getString(ARG_ETAPA_CHAVE);
            postitParaEditar = (PostIt) args.getSerializable(ARG_POSTIT_ANTIGO);
            isEditMode  = (postitParaEditar != null);
        }
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout usando View Binding
        binding = DialogAddPostitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // MODIFICAÇÃO 2: Obter a instância do ViewModel compartilhado
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(CanvasIdeiaViewModel.class);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        binding.editTextPostit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_LEN_TEXT)});

        // Configura a UI inicial
        if (isEditMode && postitParaEditar != null) {
            binding.textDialogTitle.setText("Editar Ponto-Chave");
            binding.editTextPostit.setText(postitParaEditar.getTexto());
            setSelectedColor(postitParaEditar.getCor() != null ? postitParaEditar.getCor() : corSelecionada);
        } else {
            binding.textDialogTitle.setText("Adicionar Ponto-Chave");
            setSelectedColor(corSelecionada);
        }

        // Listeners
        binding.radioGroupColors.setOnCheckedChangeListener((group, checkedId) -> {
            corSelecionada = colorForRadioId(checkedId);
        });

        binding.btnCancelarPostit.setOnClickListener(vw -> dismissAllowingStateLoss());
        binding.btnSalvarPostit.setOnClickListener(vw -> salvar());
    }

    private void salvar() {
        String texto = binding.editTextPostit.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) {
            binding.editTextPostit.setError("O post-it não pode estar vazio.");
            return;
        }

        setButtonsEnabled(false);

        // MODIFICAÇÃO 3: Chamar o ViewModel em vez do listener ou FirestoreHelper
        if (isEditMode) {
            sharedViewModel.updatePostIt(etapaChave, postitParaEditar, texto, corSelecionada);
        } else {
            sharedViewModel.addPostIt(etapaChave, texto, corSelecionada);
        }

        closeWithKeyboardHide();
    }

    // --- Lógica de UI e Utilitários (permanecem os mesmos) ---

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Previne memory leaks com View Binding em Dialogs/Fragments
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnSalvarPostit.setEnabled(enabled);
        binding.btnCancelarPostit.setEnabled(enabled);
        binding.btnSalvarPostit.setAlpha(enabled ? 1f : .6f);
        binding.btnCancelarPostit.setAlpha(enabled ? 1f : .6f);
    }

    private void closeWithKeyboardHide() {
        View current = getDialog() != null ? getDialog().getCurrentFocus() : null;
        if (current != null && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
        }
        dismissAllowingStateLoss();
    }

    private static final Map<Integer, String> ID_TO_COLOR = new HashMap<Integer, String>() {{
        put(R.id.radio_yellow, "#F9F871");
        put(R.id.radio_orange, "#FFC75F");
        put(R.id.radio_pink, "#FF96AD");
        put(R.id.radio_blue, "#84D2F6");
        put(R.id.radio_green, "#A9F0D1");
        put(R.id.radio_white, "#FFFFFF");
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
        binding.radioGroupColors.check(id != null ? id : R.id.radio_yellow);
    }
}