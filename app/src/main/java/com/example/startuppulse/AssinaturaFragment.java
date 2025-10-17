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

import com.example.startuppulse.databinding.FragmentAssinaturaBinding; // Importar a classe de binding
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

    // NOVO: Usar View Binding para acessar as views
    private FragmentAssinaturaBinding binding;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Infla o layout usando o binding
        binding = FragmentAssinaturaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicialização do Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Se o usuário não estiver logado, fecha o fragmento para evitar erros
        if (currentUser == null) {
            Toast.makeText(getContext(), "Você precisa estar logado para ver os planos.", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        configurarListeners();
        verificarAssinatura();
    }

    private void configurarListeners() {
        // Ação para o botão do plano PRO
        binding.buttonAssinarPro.setOnClickListener(v -> mostrarDialogInfoCobranca());

        // Ação para o botão do plano Gratuito (simplesmente fecha a tela)
        binding.buttonContinuarGratuito.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        // Ação para o botão de fechar na toolbar
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void mostrarDialogInfoCobranca() {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirmar Assinatura")
                .setMessage("💳 Valor: R$ 29,00/mês\n\nAo confirmar, sua conta será atualizada para o plano PRO com acesso a projetos ilimitados e match com mentores e investidores.")
                .setPositiveButton("Confirmar e Assinar", (dialog, which) -> simularCompra())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void verificarAssinatura() {
        db.collection("premium").document(currentUser.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Timestamp dataFim = document.getTimestamp("data_fim");
                        // Se o usuário já tem uma assinatura válida, ele não deveria nem ver os botões de compra.
                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            binding.buttonAssinarPro.setEnabled(false);
                            binding.buttonAssinarPro.setText("Você já é PRO");
                            binding.buttonContinuarGratuito.setVisibility(View.GONE); // Esconde o botão de continuar gratuito
                            Toast.makeText(getContext(), "Seu plano PRO já está ativo!", Toast.LENGTH_LONG).show();
                        }
                    }
                    // Se não existe documento ou a assinatura expirou, a tela funciona normalmente.
                })
                .addOnFailureListener(e -> {
                    // Em caso de falha na verificação, permite que o usuário tente assinar.
                    Toast.makeText(getContext(), "Não foi possível verificar seu plano atual.", Toast.LENGTH_SHORT).show();
                });
    }

    private void simularCompra() {
        Calendar cal = Calendar.getInstance();
        Timestamp dataInicio = new Timestamp(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 30); // Validade de 30 dias
        Timestamp dataFim = new Timestamp(cal.getTime());

        Map<String, Object> dados = new HashMap<>();
        dados.put("ativo", true); // Assinatura começa ativa (recorrência)
        dados.put("data_assinatura", dataInicio);
        dados.put("data_fim", dataFim);
        dados.put("plano", "PRO");

        db.collection("premium").document(currentUser.getUid())
                .set(dados)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Bem-vindo ao plano PRO!", Toast.LENGTH_SHORT).show();
                    // Atualiza a UI para refletir o novo status
                    verificarAssinatura();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Ocorreu um erro ao assinar. Tente novamente.", Toast.LENGTH_SHORT).show());
    }

    // É uma boa prática limpar o binding no onDestroyView para evitar memory leaks
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}