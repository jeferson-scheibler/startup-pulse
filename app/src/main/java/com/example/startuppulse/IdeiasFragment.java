package com.example.startuppulse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento que exibe a lista de rascunhos de ideias do usuário.
 */
public class IdeiasFragment extends Fragment implements IdeiasAdapter.OnIdeiaClickListener {

    private RecyclerView recyclerViewIdeias;
    private IdeiasAdapter ideiasAdapter;
    private FirestoreHelper firestoreHelper;
    private FirebaseUser currentUser;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ideias, container, false);

        // Inicialização dos componentes
        firestoreHelper = new FirestoreHelper();

        recyclerViewIdeias = view.findViewById(R.id.recycler_view_ideias);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyStateView = view.findViewById(R.id.view_empty_state_ideias);

        setupRecyclerView();

        // Configura o "puxar para atualizar"
        swipeRefreshLayout.setOnRefreshListener(this::carregarIdeiasPublicadas);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarIdeiasPublicadas();
    }

    private void setupRecyclerView() {
        recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(getContext()));
        ideiasAdapter = new IdeiasAdapter(new ArrayList<>(), this, this);
        recyclerViewIdeias.setAdapter(ideiasAdapter);
    }

    private void mostrarDialogLimite(Context context, String proximoAcesso) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Limite Máximo de Acesso Diário")
                .setMessage("Você já acessou uma ideia hoje.\n\nLimite Diário: 1\n\nPróximo acesso liberado: " + proximoAcesso +
                        "\n\nDeseja acessar ideias ilimitadas?\nTorne-se Premium.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void carregarIdeiasPublicadas() {
        swipeRefreshLayout.setRefreshing(true);

        firestoreHelper.getIdeiasPublicadas(new FirestoreHelper.IdeiasListener() {
            @Override
            public void onIdeiasCarregadas(List<Ideia> ideias) {
                swipeRefreshLayout.setRefreshing(false);
                if (ideias.isEmpty()) {
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerViewIdeias.setVisibility(View.GONE);
                } else {
                    emptyStateView.setVisibility(View.GONE);
                    recyclerViewIdeias.setVisibility(View.VISIBLE);
                    ideiasAdapter.setIdeias(ideias);
                }
            }

            @Override
            public void onError(Exception e) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Erro ao carregar ideias: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback da interface OnIdeiaClickListener, chamado quando um item da lista é clicado.
     */
    @Override
    public void onIdeiaClick(Ideia ideia) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getUid().equals(ideia.getOwnerId());
        boolean isMentor = currentUser.getUid().equals(ideia.getMentorId());

        if (isOwner || isMentor) {
            // Dono da ideia → abre normalmente
            Intent intent = new Intent(getActivity(), CanvasIdeiaActivity.class);
            intent.putExtra("ideia_id", ideia.getId());
            startActivity(intent);
        } else {
            // Não é dono → verifica limite diário
            LimiteHelper.verificarAcessoIdeia(getContext(), new LimiteHelper.LimiteCallback() {
                @Override
                public void onPermitido() {
                    Intent intent = new Intent(getActivity(), CanvasIdeiaActivity.class);
                    intent.putExtra("ideia_id", ideia.getId());
                    intent.putExtra("isReadOnly", true);
                    startActivity(intent);
                }

                @Override
                public void onNegado(String mensagem) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String uid = currentUser.getUid();

                        LimiteHelper.getProximaDataAcessoFormatada(uid, new LimiteHelper.LimiteDataCallback() {
                            @Override
                            public void onResult(String dataFormatada) {
                                mostrarDialogLimite(getContext(), dataFormatada);
                            }
                        });
                    }

                }
            });
        }
    }
}