package com.example.startuppulse.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.text.HtmlCompat; // Usar HtmlCompat para compatibilidade
import androidx.fragment.app.DialogFragment;
import com.example.startuppulse.R;
import com.example.startuppulse.databinding.DialogConfirmationBinding;

public class ConfirmationDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_POSITIVE_TEXT = "arg_positive_text";
    private static final String ARG_NEGATIVE_TEXT = "arg_negative_text";
    private static final String ARG_ICON_RES_ID = "arg_icon_res_id";
    private static final String ARG_TAG = "arg_tag";
    private static final String ARG_POSITIVE_IS_DESTRUCTIVE = "arg_positive_is_destructive"; // Para estilizar botão de exclusão

    private DialogConfirmationBinding binding;
    private ConfirmationDialogListener listener;
    private String dialogTag;

    /**
     * Cria uma nova instância do ConfirmationDialogFragment.
     *
     * @param title Título do diálogo.
     * @param message Mensagem (pode conter HTML básico como <br>, <b>).
     * @param positiveButtonText Texto do botão de confirmação.
     * @param negativeButtonText Texto do botão de cancelar.
     * @param iconResId Resource ID do ícone (opcional, 0 para nenhum).
     * @param positiveIsDestructive True se o botão positivo for uma ação destrutiva (ex: excluir), para aplicar estilo vermelho.
     * @param tag Tag para identificar o diálogo no listener.
     * @return Uma nova instância do fragmento.
     */
    public static ConfirmationDialogFragment newInstance(
            String title, String message, String positiveButtonText, String negativeButtonText,
            int iconResId, boolean positiveIsDestructive, String tag) {

        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE_TEXT, positiveButtonText);
        args.putString(ARG_NEGATIVE_TEXT, negativeButtonText);
        args.putInt(ARG_ICON_RES_ID, iconResId);
        args.putBoolean(ARG_POSITIVE_IS_DESTRUCTIVE, positiveIsDestructive);
        args.putString(ARG_TAG, tag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Garante que o Fragment pai ou a Activity implemente o listener
        if (getParentFragment() instanceof ConfirmationDialogListener) {
            listener = (ConfirmationDialogListener) getParentFragment();
        } else if (context instanceof ConfirmationDialogListener) {
            listener = (ConfirmationDialogListener) context;
        } else {
            throw new RuntimeException("O chamador deve implementar ConfirmationDialogListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogConfirmationBinding.inflate(inflater, container, false);

        // Remove o fundo padrão para usar o MaterialCardView
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Define a largura e altura da janela do diálogo
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // --- ADICIONAR PADDING ---
            // 1. Defina o padding desejado em dp
            int paddingDp = 16;

            // 2. Converta dp para pixels
            DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
            int paddingPx = Math.round(paddingDp * displayMetrics.density);

            // 3. Aplique o padding (esquerda, cima, direita, baixo)
            //    Usamos 0 para cima e baixo, pois o WRAP_CONTENT já cuida da altura.
            dialog.getWindow().getDecorView().setPadding(paddingPx, 0, paddingPx, 0);
            // --- FIM DO PADDING ---
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            dialogTag = args.getString(ARG_TAG, ""); // Pega a tag
            binding.dialogTitle.setText(args.getString(ARG_TITLE));

            // Converte mensagem (possivelmente HTML) para Spanned
            String messageHtml = args.getString(ARG_MESSAGE);
            Spanned messageSpanned = HtmlCompat.fromHtml(messageHtml != null ? messageHtml : "", HtmlCompat.FROM_HTML_MODE_LEGACY);
            binding.dialogMessage.setText(messageSpanned);

            binding.dialogButtonPositive.setText(args.getString(ARG_POSITIVE_TEXT));
            binding.dialogButtonNegative.setText(args.getString(ARG_NEGATIVE_TEXT));

            int iconRes = args.getInt(ARG_ICON_RES_ID);
            if (iconRes != 0) {
                binding.dialogIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), iconRes));
                binding.dialogIcon.setVisibility(View.VISIBLE);
            } else {
                binding.dialogIcon.setVisibility(View.GONE);
            }

            if (args.getBoolean(ARG_POSITIVE_IS_DESTRUCTIVE, false)) {
                // Removemos as linhas que setavam background e text color manualmente.
                // Aplicamos os atributos chave do estilo App.Button.Destructive
                // Nota: Aplicar um 'style' completo programaticamente a um MaterialButton
                // após inflado é complexo. Setar os atributos principais é a abordagem mais direta.

                // Aplica a cor de fundo (Tint)
                binding.dialogButtonPositive.setBackgroundTintList(
                        AppCompatResources.getColorStateList(requireContext(), R.color.colorError)
                );

                // Aplica a cor do texto
                binding.dialogButtonPositive.setTextColor(
                        AppCompatResources.getColorStateList(requireContext(), R.color.colorOnPrimary)
                );

                // Aplica a cor do ripple (se definida no estilo)
                // Assumindo que você tenha @color/rippleDestructive ou similar,
                // caso contrário, pode usar ripplePrimary ou outra cor.
                // Se o estilo usa @color/ripplePrimary:
                binding.dialogButtonPositive.setRippleColor(
                        AppCompatResources.getColorStateList(requireContext(), R.color.ripplePrimary)
                );

                // Outros atributos como shapeAppearanceOverlay e elevation
                // são mais difíceis de aplicar programaticamente aqui e geralmente
                // são herdados do estilo base do botão no layout XML.
                // O estilo base no XML é App.Button.Primary, então a forma será a mesma.

            } else {
                // Garante que o botão use o estilo padrão (App.Button.Primary) se não for destrutivo
                // Isso pode não ser estritamente necessário se o XML já define App.Button.Primary,
                // mas garante a reversão caso o DialogFragment seja reutilizado.
                binding.dialogButtonPositive.setBackgroundTintList(
                        AppCompatResources.getColorStateList(requireContext(), R.color.colorPrimary)
                );
                binding.dialogButtonPositive.setTextColor(
                        AppCompatResources.getColorStateList(requireContext(), R.color.colorOnPrimary)
                );
                binding.dialogButtonPositive.setRippleColor(
                        AppCompatResources.getColorStateList(requireContext(), R.color.ripplePrimary)
                );
            }

            binding.dialogButtonPositive.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfirm(dialogTag);
                }
                dismiss();
            });

            binding.dialogButtonNegative.setOnClickListener(v -> {
                // if (listener != null) listener.onCancel(dialogTag); // Chamar se tiver onCancel
                dismiss();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}