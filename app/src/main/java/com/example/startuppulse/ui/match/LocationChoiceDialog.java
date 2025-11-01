package com.example.startuppulse.ui.match;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.startuppulse.R;
import com.google.android.material.button.MaterialButton;

public class LocationChoiceDialog extends DialogFragment {

    private final boolean hasIdeaLocation;
    private final boolean hasUserLocation;
    private final OnChoiceListener listener;

    public interface OnChoiceListener {
        void onChoice(String choice);
    }

    public LocationChoiceDialog(boolean hasIdeaLocation, boolean hasUserLocation, OnChoiceListener listener) {
        this.hasIdeaLocation = hasIdeaLocation;
        this.hasUserLocation = hasUserLocation;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_location_choice, null);
        builder.setView(view);

        MaterialButton btnUseIdeaLocation = view.findViewById(R.id.btnUseIdeaLocation);
        MaterialButton btnUseUserLocation = view.findViewById(R.id.btnUseUserLocation);
        MaterialButton btnContinueWithoutLocation = view.findViewById(R.id.btnContinueWithoutLocation);

        TextView title = view.findViewById(R.id.tvTitle);
        TextView description = view.findViewById(R.id.tvDescription);
        LottieAnimationView animationView = view.findViewById(R.id.lottieLoading);

        // Configura visibilidade dos botões
        btnUseIdeaLocation.setVisibility(hasIdeaLocation ? View.VISIBLE : View.GONE);
        btnUseUserLocation.setVisibility(hasUserLocation ? View.VISIBLE : View.GONE);

        title.setText("Onde buscar mentores?");
        description.setText("Escolha qual localização deseja usar para encontrar o mentor ideal:");

        btnUseIdeaLocation.setOnClickListener(v -> {
            dismiss();
            listener.onChoice("IDEA");
        });

        btnUseUserLocation.setOnClickListener(v -> {
            dismiss();
            listener.onChoice("USER");
        });

        btnContinueWithoutLocation.setOnClickListener(v -> {
            dismiss();
            listener.onChoice("NONE");
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

}
