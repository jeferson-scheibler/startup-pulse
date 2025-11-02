package com.example.startuppulse.ui.investor;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.databinding.FragmentInvestorRegisterDetailsBinding;
import com.google.android.material.textfield.TextInputLayout;
import com.redmadrobot.inputmask.MaskedTextChangedListener;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestorRegisterDetailsFragment extends Fragment {

    private FragmentInvestorRegisterDetailsBinding binding;
    private InvestorVerificationViewModel viewModel; // ViewModel alterado
    private String investorType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvestorRegisterDetailsBinding.inflate(inflater, container, false);
        // ViewModel alterado para o novo
        viewModel = new ViewModelProvider(this).get(InvestorVerificationViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        investorType = getArguments() != null ? getArguments().getString("investorType") : "INDIVIDUAL";

        setupUI();
        setupClickListeners();
        setupObservers();

        // Carrega os dados do usuário assim que a tela é criada
        viewModel.loadCurrentUser();
    }

    private void setupUI() {
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        // Ajusta a constraint do botão "Verificar"
        int topTargetId = -1;

        if ("INDIVIDUAL".equals(investorType)) {
            binding.tvTitle.setText("Verificação de Investidor Anjo");
            binding.inputLayoutCpf.setVisibility(View.VISIBLE);
            binding.inputLayoutCnpj.setVisibility(View.GONE);
            topTargetId = binding.inputLayoutCpf.getId();

            MaskedTextChangedListener.Companion.installOn(
                    binding.editTextCpf,
                    "[000].[000].[000]-[00]",
                    (maskFilled, extractedValue, formattedValue) -> {}
            );
        } else { // "FIRM"
            binding.tvTitle.setText("Verificação de Fundo/Empresa");
            binding.inputLayoutCpf.setVisibility(View.GONE);
            binding.inputLayoutCnpj.setVisibility(View.VISIBLE);
            topTargetId = binding.inputLayoutCnpj.getId();

            MaskedTextChangedListener.Companion.installOn(
                    binding.editTextCnpj,
                    "[00].[000].[000]/[0000]-[00]",
                    (maskFilled, extractedValue, formattedValue) -> {}
            );
        }

        // Ajusta a constraint do botão dinamicamente
        ((androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.btnVerificar.getLayoutParams())
                .topToBottom = topTargetId;
    }

    private void setupObservers() {
        // Observa os dados do usuário (Nome, Email)
        viewModel.user.observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.editTextNome.setText(user.getNome());
                binding.editTextEmail.setText(user.getEmail());
            }
        });

        // Observa o estado da verificação
        viewModel.state.observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case LOADING:
                    setLoading(true);
                    break;
                case VERIFYING:
                    // A verificação foi INICIADA. Navega para a tela "Aguarde..."
                    setLoading(false);
                    if (NavHostFragment.findNavController(this).getCurrentDestination() != null &&
                            NavHostFragment.findNavController(this).getCurrentDestination().getId() == R.id.investorRegisterDetailsFragment)
                    {
                        NavHostFragment.findNavController(this)
                                .navigate(R.id.action_investorRegisterDetailsFragment_to_investorVerifyingFragment);
                    }
                    break;
                case ERROR:
                    setLoading(false);
                    break;
                case IDLE:
                    setLoading(false);
                    break;
            }
        });

        viewModel.errorEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(getContext(), "Erro: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnVerificar.setOnClickListener(v -> attemptVerification());
    }

    private void attemptVerification() {
        binding.inputLayoutCpf.setError(null);
        binding.inputLayoutCnpj.setError(null);

        String documento;
        TextInputLayout layoutDocumento;
        if ("INDIVIDUAL".equals(investorType)) {
            documento = binding.editTextCpf.getText().toString().trim();
            layoutDocumento = binding.inputLayoutCpf;
        } else {
            documento = binding.editTextCnpj.getText().toString().trim();
            layoutDocumento = binding.inputLayoutCnpj;
        }

        // Validação (muito mais simples)
        if (TextUtils.isEmpty(documento)) {
            layoutDocumento.setError("Documento é obrigatório.");
            return;
        }

        // Inicia o processo de verificação
        viewModel.startVerification(investorType, documento);
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnVerificar.setEnabled(!isLoading);
        binding.btnVerificar.setText(isLoading ? null : "Iniciar Verificação");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}