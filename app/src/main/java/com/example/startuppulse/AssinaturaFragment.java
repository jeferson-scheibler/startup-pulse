package com.example.startuppulse;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AssinaturaFragment extends Fragment {

    private TextView tvNomePlano, tvValidadePlano;

    private Button btnAssinar, btnAtivarRecorrencia, btnCancelar;

    private FirebaseFirestore db;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assinatura, container, false);

        tvNomePlano = view.findViewById(R.id.tvNomePlano);
        tvValidadePlano = view.findViewById(R.id.tvValidadePlano);
        btnAssinar = view.findViewById(R.id.btnSimularCompra);
        btnAtivarRecorrencia = view.findViewById(R.id.btnAtivarRecorrencia);
        btnCancelar = view.findViewById(R.id.btnCancelarAssinatura);


        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        verificarAssinatura();

        btnAssinar.setOnClickListener(v -> mostrarDialogInfoCobranca());
        btnAtivarRecorrencia.setOnClickListener(v -> mostrarDialogAtivarRecorrencia());
        btnCancelar.setOnClickListener(v -> mostrarDialogCancelarRecorrencia());


        return view;
    }

    private void mostrarDialogInfoCobranca() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Informações da Cobrança");

        String mensagem = "💳 Valor: R$ 39,90/mês\n\n" +
                "Ao confirmar, sua conta será atualizada para o plano Premium com acesso ilimitado a ideias e publicações.";

        builder.setMessage(mensagem);

        builder.setPositiveButton("Confirmar", (dialog, which) -> {
            simularCompra(); // metodo que você já usa para ativar o plano
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void mostrarDialogCancelarRecorrencia() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Cancelar Assinatura Recorrente");
        builder.setMessage("Você deseja mesmo cancelar a assinatura recorrente?");

        builder.setPositiveButton("Sim", (dialog, which) -> {
            cancelarAssinatura(); // chama sua função real de cancelamento
        });

        builder.setNegativeButton("Não", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void mostrarDialogAtivarRecorrencia() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ativar Assinatura Recorrente");

        builder.setMessage("Deseja ativar a assinatura recorrente?\n\n" +
                "Você será cobrado automaticamente todo mês para manter seu plano Premium ativo.");

        builder.setPositiveButton("Sim", (dialog, which) -> {
            ativarRecorrencia(); // chama sua função que ativa a recorrência
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void verificarAssinatura() {
        db.collection("premium").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Timestamp dataFim = document.getTimestamp("data_fim");
                        Boolean recorrente = document.getBoolean("ativo");

                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            // Assinatura ainda válida
                            String dataFormatada = android.text.format.DateFormat.format("dd/MM/yyyy", dataFim.toDate()).toString();
                            tvNomePlano.setText("Plano Premium");
                            tvValidadePlano.setText("Válido até " + dataFormatada);


                            btnAssinar.setVisibility(View.GONE);

                            if (recorrente != null && recorrente) {
                                // Recorrência ATIVADA → mostrar botão para cancelar
                                btnCancelar.setVisibility(View.VISIBLE);
                                btnAtivarRecorrencia.setVisibility(View.GONE);
                            } else {
                                // Recorrência DESATIVADA → mostrar botão para ativar
                                btnAtivarRecorrencia.setVisibility(View.VISIBLE);
                                btnCancelar.setVisibility(View.GONE);
                            }

                        } else {
                            // Assinatura vencida ou inexistente
                            tvNomePlano.setText("Seu Plano: Plano Básico");
                            tvValidadePlano.setText("");

                            btnAssinar.setVisibility(View.VISIBLE);
                            btnAtivarRecorrencia.setVisibility(View.GONE);
                            btnCancelar.setVisibility(View.GONE);
                        }

                    } else {
                        // Documento não existe (nunca assinou)
                        tvNomePlano.setText("Seu Plano: Plano Básico");
                        tvValidadePlano.setText("");
                        btnAssinar.setVisibility(View.VISIBLE);
                        btnAtivarRecorrencia.setVisibility(View.GONE);
                        btnCancelar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvNomePlano.setText("Erro ao Verificar Plano");
                    tvValidadePlano.setText("");

                    btnAssinar.setVisibility(View.GONE);
                    btnAtivarRecorrencia.setVisibility(View.GONE);
                    btnCancelar.setVisibility(View.GONE);
                });
    }


    private void simularCompra() {
        Calendar cal = Calendar.getInstance();
        Timestamp dataInicio = new Timestamp(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 30); // validade de 30 dias
        Timestamp dataFim = new Timestamp(cal.getTime());

        Map<String, Object> dados = new HashMap<>();
        dados.put("ativo", true); // ativação recorrente será feita manualmente
        dados.put("data_assinatura", dataInicio);
        dados.put("data_fim", dataFim);

        db.collection("premium").document(uid)
                .set(dados)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Assinatura ativada!", Toast.LENGTH_SHORT).show();
                    verificarAssinatura();
                });
    }

    private void ativarRecorrencia() {
        db.collection("premium").document(uid)
                .update("ativo", true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Assinatura recorrente ativada!", Toast.LENGTH_SHORT).show();
                    verificarAssinatura(); // Atualiza interface
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao ativar recorrência.", Toast.LENGTH_SHORT).show();
                });
    }


    private void cancelarAssinatura() {
        db.collection("premium").document(uid)
                .update("ativo", false)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Assinatura recorrente cancelada.", Toast.LENGTH_SHORT).show();
                    verificarAssinatura(); // atualizar interface
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao cancelar recorrência.", Toast.LENGTH_SHORT).show();
                });
    }

}
