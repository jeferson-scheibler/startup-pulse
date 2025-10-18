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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AssinaturaFragment extends Fragment {

    private FragmentAssinaturaBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

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
        binding.buttonAssinarPro.setEnabled(false); // Desabilita o botão enquanto verifica

        db.collection("premium").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Timestamp dataFim = document.getTimestamp("data_fim");
                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            // Usuário é PRO
                            binding.buttonAssinarPro.setText("Plano PRO Ativo (Simulação)");
                            binding.buttonAssinarPro.setEnabled(false);
                            binding.buttonContinuarGratuito.setVisibility(View.GONE);
                        } else {
                            // Assinatura expirou
                            binding.buttonAssinarPro.setText("Assinar Agora");
                            binding.buttonAssinarPro.setEnabled(true);
                        }
                    } else {
                        // Usuário não é PRO
                        binding.buttonAssinarPro.setText("Assinar Agora");
                        binding.buttonAssinarPro.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao verificar assinatura.", Toast.LENGTH_SHORT).show();
                    binding.buttonAssinarPro.setEnabled(true); // Permite tentar de novo
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}