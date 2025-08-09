package com.example.startuppulse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout; // Adicionado para o Empty State
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PerfilFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirestoreHelper firestoreHelper;

    // Componentes da UI
    private ShapeableImageView imageViewPerfil;
    private TextView nomeTextView, emailTextView;
    private TextView nomePlanoTextView, validadePlanoTextView;
    private Button sairButton;
    private RecyclerView recyclerViewRascunhos;
    private IdeiasAdapter rascunhosAdapter;
    private LinearLayout emptyStateRascunhos;

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

        nomePlanoTextView = view.findViewById(R.id.text_view_nome_plano);
        validadePlanoTextView = view.findViewById(R.id.text_view_validade_plano);
        verificarPlanoUsuario(nomePlanoTextView);

        Button btnGerenciarAssinatura = view.findViewById(R.id.btnGerenciarAssinatura);

        btnGerenciarAssinatura.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new AssinaturaFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Mapeia os componentes da UI com os IDs do novo layout
        imageViewPerfil = view.findViewById(R.id.image_view_perfil);
        nomeTextView = view.findViewById(R.id.text_view_nome_perfil);
        emailTextView = view.findViewById(R.id.text_view_email_perfil);
        sairButton = view.findViewById(R.id.btn_sair);
        recyclerViewRascunhos = view.findViewById(R.id.recycler_view_rascunhos);
        emptyStateRascunhos = view.findViewById(R.id.view_empty_state_rascunhos);

        setupRecyclerViewRascunhos();

        if (currentUser != null) {
            exibirDadosUsuario();
        } else {
            irParaLogin();
        }

        configurarListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            carregarRascunhos();
        }
    }

    private void setupRecyclerViewRascunhos() {
        recyclerViewRascunhos.setLayoutManager(new LinearLayoutManager(getContext()));

        rascunhosAdapter = new IdeiasAdapter(new ArrayList<>(), this::onRascunhoClicked, this);
        recyclerViewRascunhos.setAdapter(rascunhosAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(requireContext(), rascunhosAdapter));
        itemTouchHelper.attachToRecyclerView(recyclerViewRascunhos);
    }

    private void exibirDadosUsuario() {
        nomeTextView.setText(currentUser.getDisplayName());
        emailTextView.setText(currentUser.getEmail());

        Uri fotoUrl = currentUser.getPhotoUrl();
        if (fotoUrl != null && getContext() != null) {
            Glide.with(getContext())
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imageViewPerfil);
        } else {
            imageViewPerfil.setImageResource(R.drawable.ic_person);
        }

        firestoreHelper.buscarUsuario(currentUser.getUid(), new FirestoreHelper.UsuarioListener() {
            @Override
            public void onUsuarioCarregado(DocumentSnapshot snapshot) {
                if (!isAdded()) return;
            }
            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Log.e("PerfilFragment", "Erro ao carregar dados do usuário.", e);
            }
        });
    }

    private void configurarListeners() {
        sairButton.setOnClickListener(v -> {
            mAuth.signOut();
            irParaLogin();
        });
    }

    private void verificarPlanoUsuario(TextView textViewPlano) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.e("PerfilFragment", "Usuário não autenticado");
            textViewPlano.setText("Plano Básico");
            Toast.makeText(getContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        Log.d("PerfilFragment", "Verificando plano para UID: " + uid);

        FirebaseFirestore.getInstance().collection("premium")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("PerfilFragment", "Documento carregado: " + documentSnapshot.getData());

                    if (documentSnapshot.exists()) {
                        Timestamp dataFim = documentSnapshot.getTimestamp("data_fim");
                        if (dataFim != null && dataFim.toDate().after(new Date())) {
                            String dataFormatada = android.text.format.DateFormat.format("dd/MM/yyyy", dataFim.toDate()).toString();
                            nomePlanoTextView.setText("Plano Premium");
                            validadePlanoTextView.setText("Válido até "  + dataFormatada);
                        } else {
                            nomePlanoTextView.setText("Plano Básico");
                            validadePlanoTextView.setText("");
                        }
                    } else {
                        nomePlanoTextView.setText("Plano Básico");
                        validadePlanoTextView.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PerfilFragment", "Erro ao acessar Firestore:", e);
                    textViewPlano.setText("Erro ao verificar plano");
                    Toast.makeText(getContext(), "Erro ao acessar Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void carregarRascunhos() {
        firestoreHelper.getMeusRascunhos(currentUser.getUid(), new FirestoreHelper.IdeiasListener() {
            @Override
            public void onIdeiasCarregadas(List<Ideia> rascunhos) {
                if (!isAdded()) return;

                if (rascunhos == null || rascunhos.isEmpty()) {
                    recyclerViewRascunhos.setVisibility(View.GONE);
                    emptyStateRascunhos.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewRascunhos.setVisibility(View.VISIBLE);
                    emptyStateRascunhos.setVisibility(View.GONE);
                }
                rascunhosAdapter.setIdeias(rascunhos);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Erro ao carregar rascunhos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onRascunhoClicked(Ideia ideia) {
        Intent intent = new Intent(getActivity(), CanvasIdeiaActivity.class);
        intent.putExtra("ideia_id", ideia.getId());
        startActivity(intent);
    }

    private void irParaLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Limpa o back stack para que o usuário não possa voltar para a tela de perfil
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
