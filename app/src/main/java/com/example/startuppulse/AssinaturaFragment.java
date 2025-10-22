package com.example.startuppulse;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.startuppulse.databinding.FragmentAssinaturaBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AssinaturaFragment extends Fragment {

    private FragmentAssinaturaBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAssinaturaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Você precisa estar logado.", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        configurarListeners();
        verificarStatusAssinatura();
    }

    private void configurarListeners() {
        // Usa o ID do seu layout mais recente
        binding.buttonAssinarPro.setOnClickListener(v -> mostrarDialogoDeConfirmacao());
        binding.buttonContinuarGratuito.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.buttonCancelarSimulacao.setOnClickListener(v -> mostrarDialogoDeConfirmacaoCancelamento());
    }

    private void mostrarDialogoDeConfirmacao() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Ativar Simulação PRO")
                .setMessage("Deseja simular a compra e ativar o plano PRO por 30 dias para este usuário? (Apenas para teste, sem cobrança)")
                .setPositiveButton("Sim, Ativar", (dialog, which) -> simularCompra())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Verifica na coleção 'premium' se o usuário já tem uma assinatura ativa.
     */
    private void verificarStatusAssinatura() {
        binding.buttonAssinarPro.setEnabled(false);
        binding.buttonCancelarSimulacao.setVisibility(View.GONE); // Esconde por padrão
        binding.textValidadeAssinatura.setVisibility(View.GONE); // Esconde por padrão

        db.collection("premium").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (!isAdded() || binding == null) return; // Verifica se o fragment ainda está ativo

                    if (document.exists()) {
                        Timestamp dataFimTs = document.getTimestamp("data_fim");
                        if (dataFimTs != null && dataFimTs.toDate().after(new Date())) {
                            // Usuário é PRO
                            binding.buttonAssinarPro.setText("Plano PRO Ativo (Simulação)");
                            binding.buttonAssinarPro.setEnabled(false); // Não pode assinar de novo
                            binding.buttonContinuarGratuito.setVisibility(View.GONE);

                            // --- MOSTRAR VALIDADE E BOTÃO CANCELAR ---
                            String dataFormatada = dateFormat.format(dataFimTs.toDate());
                            binding.textValidadeAssinatura.setText("Válido até: " + dataFormatada);
                            binding.textValidadeAssinatura.setVisibility(View.VISIBLE);
                            binding.buttonCancelarSimulacao.setVisibility(View.VISIBLE);
                            // ----------------------------------------

                        } else {
                            // Assinatura expirou ou inválida
                            configurarParaNaoAssinante();
                        }
                    } else {
                        // Usuário não é PRO (documento não existe)
                        configurarParaNaoAssinante();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(getContext(), "Erro ao verificar assinatura.", Toast.LENGTH_SHORT).show();
                    configurarParaNaoAssinante(); // Assume não assinante em caso de erro
                });
    }

    /**
     * Este é o seu método original. Ele é a forma mais simples de simular.
     * Escreve diretamente no Firestore para conceder o status PRO.
     */
    private void simularCompra() {
        Calendar cal = Calendar.getInstance();
        Timestamp dataInicio = new Timestamp(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 30); // Validade de 30 dias
        Timestamp dataFim = new Timestamp(cal.getTime());

        Map<String, Object> dados = new HashMap<>();
        dados.put("ativo", true);
        dados.put("data_assinatura", dataInicio);
        dados.put("data_fim", dataFim);
        dados.put("plano", "PRO");

        // Usa a sua coleção "premium" original
        db.collection("premium").document(currentUser.getUid())
                .set(dados)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Simulação bem-sucedida! Plano PRO ativado.", Toast.LENGTH_SHORT).show();
                    // Atualiza a UI para refletir o novo status
                    verificarStatusAssinatura();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro na simulação. Tente novamente.", Toast.LENGTH_SHORT).show());
    }

    private void configurarParaNaoAssinante() {
        if (!isAdded() || binding == null) return;
        binding.buttonAssinarPro.setText("Assinar Agora");
        binding.buttonAssinarPro.setEnabled(true);
        binding.buttonContinuarGratuito.setVisibility(View.VISIBLE);
        binding.textValidadeAssinatura.setVisibility(View.GONE);
        binding.buttonCancelarSimulacao.setVisibility(View.GONE);
    }

    private void cancelarSimulacao() {
        db.collection("premium").document(currentUser.getUid())
                .delete() // Simplesmente deleta o documento da assinatura simulada
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Simulação cancelada.", Toast.LENGTH_SHORT).show();
                    // Atualiza a UI para refletir o novo status (não assinante)
                    verificarStatusAssinatura();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao cancelar simulação. Tente novamente.", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoDeConfirmacaoCancelamento() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Simulação PRO")
                .setMessage("Tem certeza que deseja cancelar a simulação do plano PRO? Você voltará para o plano gratuito.")
                .setPositiveButton("Sim, Cancelar", (dialog, which) -> cancelarSimulacao())
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}