package com.example.startuppulse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.startuppulse.common.Result;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PerfilFragment extends Fragment {

    private static final String TAG = "PerfilFragment";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirestoreHelper firestoreHelper;

    // UI existentes
    private ShapeableImageView imageViewPerfil;
    private TextView nomeTextView, emailTextView;
    private TextView nomePlanoTextView, validadePlanoTextView;
    private Button sairButton, btnGerenciarAssinatura;

    // Novos (do layout remodelado)
    private TextView chipPremium, chipMemberSince;
    private TextView statPublicadas, statSeguindo, statDias;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestoreHelper = new FirestoreHelper();

        // Plano
        nomePlanoTextView = view.findViewById(R.id.text_view_nome_plano);
        validadePlanoTextView = view.findViewById(R.id.text_view_validade_plano);
        btnGerenciarAssinatura = view.findViewById(R.id.btnGerenciarAssinatura);
        btnGerenciarAssinatura.setOnClickListener(v ->
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AssinaturaFragment())
                        .addToBackStack(null)
                        .commit()
        );

        // Perfil
        imageViewPerfil = view.findViewById(R.id.image_view_perfil);
        nomeTextView = view.findViewById(R.id.text_view_nome_perfil);
        emailTextView = view.findViewById(R.id.text_view_email_perfil);
        sairButton = view.findViewById(R.id.btn_sair);

        // Novos componentes
        chipPremium = view.findViewById(R.id.chip_premium);
        chipMemberSince = view.findViewById(R.id.chip_member_since);
        statPublicadas = view.findViewById(R.id.stat_publicadas);
        statSeguindo = view.findViewById(R.id.stat_seguindo);
        statDias = view.findViewById(R.id.stat_dias);

        configurarListeners();

        if (currentUser != null) {
            exibirDadosUsuario();
            verificarPlanoUsuario(nomePlanoTextView); // também controla chip premium
            preencherDiasDeConta();
            carregarContagemPublicadas();
            carregarContagemSeguindo();
        } else {
            irParaLogin();
        }
    }

    private void exibirDadosUsuario() {
        String nome = (currentUser != null) ? currentUser.getDisplayName() : null;
        String email = (currentUser != null) ? currentUser.getEmail() : null;

        nomeTextView.setText(TextUtils.isEmpty(nome) ? "Sem nome" : nome);
        emailTextView.setText(TextUtils.isEmpty(email) ? "—" : email);

        Uri fotoUrl = (currentUser != null) ? currentUser.getPhotoUrl() : null;
        if (isAdded()) {
            Glide.with(this)
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageViewPerfil);
        }

        if (currentUser != null) {
            firestoreHelper.buscarUsuario(currentUser.getUid(), (Result<DocumentSnapshot> r) -> {
                if (!isAdded()) return;
                if (!r.isOk()) {
                    Log.e(TAG, "Erro ao carregar usuário", r.error);
                }
            });
        }
    }

    private void configurarListeners() {
        sairButton.setOnClickListener(v -> {
            mAuth.signOut();
            irParaLogin();
        });
    }

    /**
     * Atualiza card do plano e também a visibilidade do chip "Premium".
     */
    private void verificarPlanoUsuario(TextView textViewPlano) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            textViewPlano.setText("Plano Básico");
            validadePlanoTextView.setText("");
            setPremiumChipVisible(false);
            return;
        }

        FirebaseFirestore.getInstance().collection("premium")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    boolean ativo = false;
                    if (documentSnapshot.exists()) {
                        Timestamp dataFim = documentSnapshot.getTimestamp("data_fim");
                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            String dataFormatada = android.text.format.DateFormat
                                    .format("dd/MM/yyyy", dataFim.toDate()).toString();
                            nomePlanoTextView.setText("Plano Premium");
                            validadePlanoTextView.setText("Válido até " + dataFormatada);
                            ativo = true;
                        }
                    }

                    if (!ativo) {
                        nomePlanoTextView.setText("Plano Básico");
                        validadePlanoTextView.setText("");
                    }
                    setPremiumChipVisible(ativo);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Erro ao acessar Firestore (premium):", e);
                    textViewPlano.setText("Erro ao verificar plano");
                    setPremiumChipVisible(false);
                    Toast.makeText(requireContext(), "Erro ao acessar Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setPremiumChipVisible(boolean visible) {
        if (chipPremium != null) chipPremium.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Preenche "Membro há X dias" e o card "Dias".
     */
    private void preencherDiasDeConta() {
        if (currentUser == null || currentUser.getMetadata() == null) {
            if (chipMemberSince != null) chipMemberSince.setText("");
            if (statDias != null) statDias.setText("—");
            return;
        }
        long created = currentUser.getMetadata().getCreationTimestamp();
        long dias = Math.max(1, TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - created));

        if (statDias != null) statDias.setText(String.valueOf(dias));

        // Texto compacto para o chip
        String textoChip;
        if (dias < 30) {
            textoChip = String.format(Locale.getDefault(), "Membro há %d dias", dias);
        } else {
            long meses = Math.max(1, dias / 30);
            textoChip = String.format(Locale.getDefault(), "Membro há %d %s",
                    meses, (meses == 1 ? "mês" : "meses"));
        }
        if (chipMemberSince != null) chipMemberSince.setText(textoChip);
    }

    /**
     * Contabiliza ideias publicadas do usuário.
     * Ajuste a coleção/campo conforme seu modelo.
     */
    private void carregarContagemPublicadas() {
        if (currentUser == null) {
            if (statPublicadas != null) statPublicadas.setText("—");
            return;
        }
        // Exemplo de modelo:
        // Collection "ideias" com campos: autorId (string) e status (ex: "PUBLICADA")
        FirebaseFirestore.getInstance()
                .collection("ideias")
                .whereEqualTo("ownerId", currentUser.getUid())
                .whereIn("status", java.util.Arrays.asList("EM_AVALIACAO", "AVALIADA_APROVADA", "AVALIADA_REPROVADA"))
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    int count = (snap != null) ? snap.size() : 0;
                    if (count == 0) {
                        // fallback: talvez seu modelo use "visibilidade"=="publica"
                        FirebaseFirestore.getInstance()
                                .collection("ideias")
                                .whereEqualTo("autorId", currentUser.getUid())
                                .whereEqualTo("visibilidade", "publica")
                                .get()
                                .addOnSuccessListener(snap2 -> {
                                    int c2 = (snap2 != null) ? snap2.size() : 0;
                                    if (statPublicadas != null) statPublicadas.setText(String.valueOf(c2));
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "contagem publicadas fallback falhou", e);
                                    if (statPublicadas != null) statPublicadas.setText("0");
                                });
                    } else {
                        if (statPublicadas != null) statPublicadas.setText(String.valueOf(count));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.w(TAG, "contagem publicadas falhou", e);
                    if (statPublicadas != null) statPublicadas.setText("0");
                });
    }

    /**
     * Contabiliza mentores seguidos.
     * Ajuste a coleção/campo conforme seu modelo (ex.: /users/{uid}/following_mentors).
     */
    private void carregarContagemSeguindo() {
        if (currentUser == null) {
            if (statSeguindo != null) statSeguindo.setText("—");
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .collection("following_mentors")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    int count = (snap != null) ? snap.size() : 0;
                    if (statSeguindo != null) statSeguindo.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.w(TAG, "contagem seguindo falhou", e);
                    if (statSeguindo != null) statSeguindo.setText("0");
                });
    }

    private void irParaLogin() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}