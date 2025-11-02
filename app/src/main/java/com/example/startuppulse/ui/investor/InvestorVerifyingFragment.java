package com.example.startuppulse.ui.investor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentInvestorVerifyingBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestorVerifyingFragment extends Fragment {

    private FragmentInvestorVerifyingBinding binding;
    private InvestorRegistrationViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvestorVerifyingBinding.inflate(inflater, container, false);
        // Scopo do ViewModel deve ser o mesmo do fragmento anterior
        // Usamos o ViewModelProvider do Fragmento
        viewModel = new ViewModelProvider(this).get(InvestorRegistrationViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupObservers();

        // Inicia a escuta assim que a tela é exibida
        viewModel.beginListening();

        // Impede o usuário de voltar para o formulário
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Não faz nada, bloqueia o botão "voltar"
                Toast.makeText(getContext(), "Verificação em andamento...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupObservers() {
        viewModel.state.observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case SUCCESS:
                    // Verificado com sucesso!
                    // Navega para a Tela 4: Completar Perfil
                    Toast.makeText(getContext(), "Perfil verificado com sucesso!", Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_investorVerifyingFragment_to_investorProfileSetupFragment);
                    break;
                case ERROR:
                    // O erro já é tratado pelo errorEvent
                    break;
                case VERIFYING:
                case IDLE:
                default:
                    // Continua mostrando a tela de "Verificando..."
                    break;
            }
        });

        viewModel.errorEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                // Exibe o motivo da rejeição e navega de volta
                showRejectionDialog(message);
            }
        });
    }

    /**
     * Exibe um diálogo de erro/rejeição.
     */
    private void showRejectionDialog(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Falha na Verificação")
                .setMessage(message)
                .setPositiveButton("Entendi", (dialog, which) -> {
                    // Navega de volta para a tela de escolha
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_investorVerifyingFragment_to_investorTypeChoiceFragment);
                })
                .setCancelable(false) // Impede que o usuário feche o diálogo
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}