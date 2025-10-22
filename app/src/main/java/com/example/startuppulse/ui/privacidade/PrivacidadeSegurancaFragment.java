package com.example.startuppulse.ui.privacidade;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentPrivacidadeSegurancaBinding;
import dagger.hilt.android.AndroidEntryPoint;
import com.example.startuppulse.util.ConfirmationDialogFragment;
import com.example.startuppulse.util.ConfirmationDialogListener;

@AndroidEntryPoint
public class PrivacidadeSegurancaFragment extends Fragment implements ConfirmationDialogListener{

    private FragmentPrivacidadeSegurancaBinding binding;
    private PrivacidadeSegurancaViewModel viewModel;
    private static final String DELETE_CONFIRM_TAG = "delete_confirm";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPrivacidadeSegurancaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PrivacidadeSegurancaViewModel.class);

        setupToolbar();
        setupClickListeners();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupClickListeners() {
        binding.btnAlterarSenha.setOnClickListener(v -> viewModel.sendPasswordResetEmail());

        // Atualiza o ViewModel quando o switch muda, mas apenas se o estado realmente mudar
        binding.switchVisibilidadePerfil.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Evita chamadas repetidas se o estado for definido programaticamente
            if (buttonView.isPressed()) {
                viewModel.updateProfileVisibility(isChecked);
            }
        });

        binding.btnExcluirConta.setOnClickListener(v -> {
            Log.d("PrivFrag", "Botão Excluir/Desativar clicado. Preparando dados do diálogo...");
            // Chama o ViewModel para iniciar a verificação dos critérios
            viewModel.prepareDeletionDialogData();
            // O diálogo será mostrado pelo observer de deactivationCriteriaReady
        });
    }

    private void setupObservers() {
        viewModel.isGoogleSignIn.observe(getViewLifecycleOwner(), isGoogle -> {
            if (binding == null) return;
            binding.btnAlterarSenha.setEnabled(!isGoogle);
            binding.textInfoGoogleLogin.setVisibility(isGoogle ? View.VISIBLE : View.GONE);
        });

        viewModel.isProfilePublic.observe(getViewLifecycleOwner(), isPublic -> {
            if (binding == null) return;
            // Define o estado do switch sem acionar o listener
            binding.switchVisibilidadePerfil.setChecked(isPublic);
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // Opcional: Desabilitar botões durante o carregamento
            binding.btnAlterarSenha.setEnabled(!isLoading && !viewModel.isGoogleSignIn.getValue());
            binding.switchVisibilidadePerfil.setEnabled(!isLoading);
            binding.btnExcluirConta.setEnabled(!isLoading);
        });

        viewModel.toastMessage.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.navigateToLogin.observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                // Navega para o início (Splash ou Login), limpando a pilha
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_global_return_to_login); // Use a ação global que criamos
            }
        });
        viewModel.getDeactivationCriteriaReady().observe(getViewLifecycleOwner(), criteriaPair -> {
            // Check if the Pair and its contents are not null
            if (criteriaPair != null && criteriaPair.first != null && criteriaPair.second != null) {
                Boolean isMentor = criteriaPair.first;
                Boolean hasRatedIdeas = criteriaPair.second;
                Log.d("PrivFrag", "Critérios recebidos: isMentor=" + isMentor + ", hasRatedIdeas=" + hasRatedIdeas + ". Mostrando diálogo.");
                showDeleteConfirmationDialog(isMentor, hasRatedIdeas);
            } else {
                Log.d("PrivFrag", "Criteria pair é nulo ou incompleto no observer, diálogo não mostrado.");
                // Optional: Show a generic error Toast if needed
                // Toast.makeText(getContext(), "Erro ao verificar critérios.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog(boolean isMentor, boolean hasRatedIdeas) {
        boolean shouldDeactivate = isMentor || hasRatedIdeas;

        String title;
        String message; // A mensagem agora é String (pode conter HTML)
        String positiveButtonText;
        int iconResId = shouldDeactivate ? R.drawable.ic_info : R.drawable.ic_delete; // Ícone diferente

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Verificação de critérios:<br>");
        messageBuilder.append(isMentor ? "❌" : "✓");
        messageBuilder.append(" É mentor<br>");
        messageBuilder.append(hasRatedIdeas ? "❌" : "✓");
        messageBuilder.append(" Possui ideias avaliadas<br><br>");

        if (shouldDeactivate) {
            title = "Desativar Conta";
            messageBuilder.append("Sua conta será <b>desativada</b>. Seus dados serão mantidos, mas a conta ficará inacessível. Você poderá reativá-la no futuro (após 24h). Tem certeza?");
            positiveButtonText = "Desativar";
        } else {
            title = getString(R.string.confirmar_exclusao_titulo);
            messageBuilder.append(getString(R.string.confirmar_exclusao_mensagem));
            positiveButtonText = getString(R.string.excluir);
        }
        message = messageBuilder.toString();

        // Cria e mostra o DialogFragment customizado
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                title,
                message,
                positiveButtonText,
                getString(android.R.string.cancel), // Texto padrão para cancelar
                iconResId,
                !shouldDeactivate, // Botão positivo é destrutivo APENAS se for exclusão
                DELETE_CONFIRM_TAG // Passa a tag
        );
        // Mostra o diálogo usando o FragmentManager do Fragment filho (importante!)
        dialog.show(getChildFragmentManager(), DELETE_CONFIRM_TAG);
    }

    @Override
    public void onConfirm(String dialogTag) {
        // Verifica se a confirmação veio do diálogo correto
        if (DELETE_CONFIRM_TAG.equals(dialogTag)) {
            Log.d("PrivFrag", "Confirmação recebida para desativar/excluir conta.");
            // Chama a função do ViewModel para executar a ação
            viewModel.requestAccountDeletionOrDeactivation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}