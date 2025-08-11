package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.databinding.FragmentIdeiasBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.ItemTouchHelper;
import com.google.android.material.snackbar.Snackbar;
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;


/**
 * Fragmento que exibe a lista de ideias publicadas.
 * (Agora usando ViewBinding e FirestoreHelper com Callback<Result<T>>)
 */
public class IdeiasFragment extends Fragment implements IdeiasAdapter.OnIdeiaClickListener {

    private FragmentIdeiasBinding binding;
    private IdeiasAdapter ideiasAdapter;
    private FirestoreHelper firestoreHelper;
    private com.google.firebase.firestore.ListenerRegistration ideiasRegistration;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingDeletes = new HashMap<>();
    private static final long DELETE_DELAY_MS = 3500L; // 3.5s

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreHelper = new FirestoreHelper();

        // RecyclerView
        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        ideiasAdapter = new IdeiasAdapter(new ArrayList<>(), this, this);
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);

        attachSwipeToDelete();

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // não movemos itens
            }

            // Permite swipe só para o DONO da ideia (evita exclusão indevida)
            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                int position = vh.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return 0;

                Ideia ideia = ideiasAdapter.getCurrentList().get(position);
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                boolean souDono = uid != null && uid.equals(ideia.getOwnerId());

                return souDono ? super.getSwipeDirs(rv, vh) : 0; // 0 = bloqueia swipe
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                // chama o fluxo de confirmação+exclusão que está no adapter
                ideiasAdapter.iniciarExclusao(position);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewIdeias);

        // pull-to-refresh e carga inicial
        binding.swipeRefreshLayout.setOnRefreshListener(this::startRealtimeIdeias);
        startRealtimeIdeias();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // evita vazamento
    }

    private void mostrarDialogLimite(String proximoAcesso) {
        if (getContext() == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Limite Máximo de Acesso Diário")
                .setMessage(
                        "Você já acessou uma ideia hoje.\n\n" +
                                "Limite Diário: 1\n\n" +
                                "Próximo acesso liberado: " + proximoAcesso + "\n\n" +
                                "Deseja acessar ideias ilimitadas?\nTorne-se Premium."
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void startRealtimeIdeias() {
        if (ideiasRegistration != null) return; // já ativo
        binding.swipeRefreshLayout.setRefreshing(true);

        ideiasRegistration = firestoreHelper.listenToIdeiasPublicadas(r -> {
            if (binding == null) return;
            binding.swipeRefreshLayout.setRefreshing(false);

            if (!r.isOk()) {
                com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(),
                        "Erro ao atualizar ideias: " + r.error.getMessage(),
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show();
                return;
            }

            java.util.List<Ideia> ideias = r.data != null ? r.data : new java.util.ArrayList<>();
            if (ideias.isEmpty()) {
                binding.viewEmptyStateIdeias.setVisibility(View.VISIBLE);
                binding.recyclerViewIdeias.setVisibility(View.GONE);
            } else {
                binding.viewEmptyStateIdeias.setVisibility(View.GONE);
                binding.recyclerViewIdeias.setVisibility(View.VISIBLE);
                ideiasAdapter.submitList(ideias);
            }
        });
    }

    private void stopRealtimeIdeias() {
        if (ideiasRegistration != null) {
            ideiasRegistration.remove();
            ideiasRegistration = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startRealtimeIdeias(); // garante ativo ao voltar pra tela
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRealtimeIdeias(); // evita vazamento/listener ativo fora da tela
    }

    /**
     * Clique em uma ideia.
     * - Dono ou Mentor: abre modo de edição
     * - Outros: checa limite diário e abre em read-only se permitido
     */
    @Override
    public void onIdeiaClick(Ideia ideia) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        boolean isOwner = uid != null && uid.equals(ideia.getOwnerId());
        boolean isMentor = uid != null && uid.equals(ideia.getMentorId()); // correção de NPE

        if (isOwner || isMentor) {
            Intent intent = new Intent(getActivity(), CanvasIdeiaActivity.class);
            intent.putExtra("ideia_id", ideia.getId());
            startActivity(intent);
            return;
        }

        // Não é dono/mentor → verificar limite diário
        LimiteHelper.verificarAcessoIdeia(requireContext(), new LimiteHelper.LimiteCallback() {
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
                if (currentUser == null) {
                    Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
                    return;
                }
                String uid = currentUser.getUid();
                LimiteHelper.getProximaDataAcessoFormatada(uid, dataFormatada ->
                        mostrarDialogLimite(dataFormatada)
                );
            }
        });
    }
    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                Ideia ideia = ideiasAdapter.getItemAt(pos);
                ideiasAdapter.removeAt(pos); // remove visualmente

                Snackbar sb = Snackbar.make(binding.getRoot(),
                        "Ideia excluída", Snackbar.LENGTH_LONG);

                // A11y: anuncia o Snackbar
                sb.addCallback(new Snackbar.Callback() {
                    @Override public void onShown(Snackbar transientBottomBar) {
                        transientBottomBar.getView().announceForAccessibility("Ideia excluída. Toque em desfazer para cancelar.");
                    }
                });

                sb.setAction("Desfazer", v -> {
                    // Cancela o delete pendente e restaura na posição
                    Runnable r = pendingDeletes.remove(ideia.getId());
                    if (r != null) handler.removeCallbacks(r);
                    ideiasAdapter.restore(ideia, pos);
                });

                sb.show();

                // Agenda exclusão real após o timeout do Snackbar
                Runnable r = () -> {
                    pendingDeletes.remove(ideia.getId());
                    new FirestoreHelper().excluirIdeia(ideia.getId(), res -> {
                        if (!res.isOk() && binding != null) {
                            Snackbar err = Snackbar.make(binding.getRoot(),
                                    "Falha ao excluir. Tente novamente.",
                                    Snackbar.LENGTH_LONG);
                            err.show();
                            // Restaura visualmente se o delete falhar
                            ideiasAdapter.restore(ideia, Math.min(pos, ideiasAdapter.getItemCount()));
                        }
                    });
                };
                pendingDeletes.put(ideia.getId(), r);
                handler.postDelayed(r, DELETE_DELAY_MS);
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(binding.recyclerViewIdeias);
    }
}