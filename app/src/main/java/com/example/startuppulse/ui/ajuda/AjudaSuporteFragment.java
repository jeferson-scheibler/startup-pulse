package com.example.startuppulse.ui.ajuda;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentAjudaSuporteBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AjudaSuporteFragment extends Fragment {

    private FragmentAjudaSuporteBinding binding;
    private AjudaSuporteViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAjudaSuporteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AjudaSuporteViewModel.class);

        setupToolbar();
        setupClickListeners();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupClickListeners() {
        binding.btnFaleConosco.setOnClickListener(v -> viewModel.requestOpenEmailClient());
    }

    private void setupObservers() {
        viewModel.openEmailClient.observe(getViewLifecycleOwner(), event -> {
            String emailAddress = event.getContentIfNotHandled();
            if (emailAddress != null) {
                openEmailClient(emailAddress);
            }
        });
    }

    private void openEmailClient(String emailAddress) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        // Configura para abrir apenas apps de e-mail
        emailIntent.setData(Uri.parse("mailto:"));
        // Define o destinatário
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
        // Define um assunto padrão
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.assunto_email_suporte));

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.enviar_email_suporte)));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(getContext(), R.string.nenhum_app_email, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}