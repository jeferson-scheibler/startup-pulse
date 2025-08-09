package com.example.startuppulse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

/**
 * VERSÃO FINAL E CORRIGIDA.
 * Esta versão utiliza o objeto PostIt para adição e edição,
 * garantindo segurança de tipos e resolvendo os erros de compilação.
 */
public class AddPostItDialogFragment extends DialogFragment {

    private static final String ARG_IDEIA_ID = "ideia_id";
    private static final String ARG_ETAPA_CHAVE = "etapa_chave";
    private static final String ARG_POSTIT_ANTIGO = "postit_antigo"; // Agora armazena um objeto PostIt

    private EditText editTextPostIt;
    private String ideiaId;
    private String etapaChave;
    private PostIt postitParaEditar; // Variável agora é do tipo PostIt
    private boolean isEditMode = false;
    private FirestoreHelper firestoreHelper;
    private AddPostItListener listener;
    private String corSelecionada = "#F9F871"; // Cor padrão

    // 1. A interface foi corrigida para usar o objeto PostIt
    public interface AddPostItListener {
        void onPostItAdded();
        void onPostItEdited(PostIt postitAntigo, String novoTexto, String novaCor);
    }

    // Método para ADICIONAR (continua igual)
    public static AddPostItDialogFragment newInstanceForAdd(String ideiaId, String etapaChave) {
        AddPostItDialogFragment fragment = new AddPostItDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IDEIA_ID, ideiaId);
        args.putString(ARG_ETAPA_CHAVE, etapaChave);
        fragment.setArguments(args);
        return fragment;
    }

    // 2. Método para EDITAR foi corrigido para aceitar um objeto PostIt
    public static AddPostItDialogFragment newInstanceForEdit(String ideiaId, String etapaChave, PostIt postit) {
        AddPostItDialogFragment fragment = new AddPostItDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IDEIA_ID, ideiaId);
        args.putString(ARG_ETAPA_CHAVE, etapaChave);
        args.putSerializable(ARG_POSTIT_ANTIGO, postit); // Passa o objeto PostIt
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // O listener agora é o fragmento que o chamou (CanvasBlockFragment)
        if (getTargetFragment() instanceof AddPostItListener) {
            listener = (AddPostItListener) getTargetFragment();
        } else {
            throw new ClassCastException("O Fragmento alvo deve implementar AddPostItListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreHelper = new FirestoreHelper();
        if (getArguments() != null) {
            ideiaId = getArguments().getString(ARG_IDEIA_ID);
            etapaChave = getArguments().getString(ARG_ETAPA_CHAVE);

            // 3. Obtém o objeto PostIt para edição
            if (getArguments().containsKey(ARG_POSTIT_ANTIGO)) {
                postitParaEditar = (PostIt) getArguments().getSerializable(ARG_POSTIT_ANTIGO);
                isEditMode = (postitParaEditar != null);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_postit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tituloDialog = view.findViewById(R.id.text_dialog_title);
        editTextPostIt = view.findViewById(R.id.edit_text_postit);
        MaterialButton btnSalvar = view.findViewById(R.id.btn_salvar_postit);
        MaterialButton btnCancelar = view.findViewById(R.id.btn_cancelar_postit);
        RadioGroup radioGroupColors = view.findViewById(R.id.radio_group_colors);

        // Configura o seletor de cores
        radioGroupColors.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_yellow) corSelecionada = "#F9F871";
            else if (checkedId == R.id.radio_orange) corSelecionada = "#FFC75F";
            else if (checkedId == R.id.radio_pink) corSelecionada = "#FF96AD";
            else if (checkedId == R.id.radio_blue) corSelecionada = "#84D2F6";
            else if (checkedId == R.id.radio_green) corSelecionada = "#A9F0D1";
            else if (checkedId == R.id.radio_white) corSelecionada = "#FFFFFF";
        });

        btnCancelar.setOnClickListener(v -> dismiss());
        btnSalvar.setOnClickListener(v -> salvar());

        // 4. Lógica de edição corrigida para usar os getters do objeto PostIt
        if (isEditMode) {
            tituloDialog.setText("Editar Ponto-Chave");
            editTextPostIt.setText(postitParaEditar.getTexto());
            String corExistente = postitParaEditar.getCor();
            if (corExistente != null) {
                corSelecionada = corExistente;
                switch (corExistente.toUpperCase()) { // Usa toUpperCase para segurança
                    case "#F9F871": radioGroupColors.check(R.id.radio_yellow); break;
                    case "#FFC75F": radioGroupColors.check(R.id.radio_orange); break;
                    case "#FF96AD": radioGroupColors.check(R.id.radio_pink); break;
                    case "#84D2F6": radioGroupColors.check(R.id.radio_blue); break;
                    case "#A9F0D1": radioGroupColors.check(R.id.radio_green); break;
                    case "#FFFFFF": radioGroupColors.check(R.id.radio_white); break;
                    default: radioGroupColors.check(R.id.radio_yellow); break;
                }
            } else {
                radioGroupColors.check(R.id.radio_yellow);
            }
        } else {
            tituloDialog.setText("Adicionar Ponto-Chave");
            radioGroupColors.check(R.id.radio_yellow);
        }
    }

    private void salvar() {
        String postItText = editTextPostIt.getText().toString().trim();
        if (postItText.isEmpty()) {
            editTextPostIt.setError("O post-it não pode estar vazio.");
            return;
        }

        // 5. A chamada para o listener foi corrigida
        if (isEditMode) {
            listener.onPostItEdited(postitParaEditar, postItText, corSelecionada);
        } else {
            firestoreHelper.addPostitToIdeia(ideiaId, etapaChave, postItText, corSelecionada, new FirestoreHelper.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    if (listener != null) {
                        listener.onPostItAdded();
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Erro ao salvar.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        dismiss();
    }
}