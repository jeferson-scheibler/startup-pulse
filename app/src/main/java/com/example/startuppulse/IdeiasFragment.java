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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreHelper = new FirestoreHelper();

        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Corrigido para a nova assinatura do construtor
        ideiasAdapter = new IdeiasAdapter(this, this);
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);

        attachSwipeToDelete();

        binding.swipeRefreshLayout.setOnRefreshListener(this::startRealtimeIdeias);
    }

    @Override
    public void onStart() {
        super.onStart();
        startRealtimeIdeias();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRealtimeIdeias();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startRealtimeIdeias() {
        if (ideiasRegistration != null) return;
        binding.swipeRefreshLayout.setRefreshing(true);

        ideiasRegistration = firestoreHelper.listenToIdeiasPublicadas(r -> {
            if (binding == null) return;
            binding.swipeRefreshLayout.setRefreshing(false);

            if (!r.isOk()) {
                Snackbar.make(binding.getRoot(), "Erro ao atualizar ideias: " + r.error.getMessage(), Snackbar.LENGTH_LONG).show();
                return;
            }

            List<Ideia> ideias = r.data != null ? r.data : new ArrayList<>();
            ideiasAdapter.submitList(ideias); // Usa submitList para atualizar a UI

            // Atualiza o estado vazio
            binding.viewEmptyStateIdeias.setVisibility(ideias.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerViewIdeias.setVisibility(ideias.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void stopRealtimeIdeias() {
        if (ideiasRegistration != null) {
            ideiasRegistration.remove();
            ideiasRegistration = null;
        }
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
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            // Verifica se o utilizador é o dono da ideia antes de permitir o swipe
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return 0;

                Ideia ideia = ideiasAdapter.getCurrentList().get(position);
                String uid = FirebaseAuth.getInstance().getUid();
                boolean isOwner = uid != null && uid.equals(ideia.getOwnerId());

                return isOwner ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                // O ListAdapter já remove o item da lista visualmente quando o submitList é chamado.
                // A nossa estratégia será submeter uma lista sem o item e depois restaurá-la se for desfeito.
                final Ideia ideiaParaExcluir = ideiasAdapter.getCurrentList().get(position);
                final List<Ideia> listaAtual = new ArrayList<>(ideiasAdapter.getCurrentList());
                listaAtual.remove(ideiaParaExcluir);
                ideiasAdapter.submitList(listaAtual);

                Snackbar snackbar = Snackbar.make(binding.getRoot(), "Ideia excluída", Snackbar.LENGTH_LONG);
                snackbar.setAction("Desfazer", v -> {
                    // Cancela a exclusão pendente
                    Runnable pendingDelete = pendingDeletes.remove(ideiaParaExcluir.getId());
                    if (pendingDelete != null) {
                        handler.removeCallbacks(pendingDelete);
                    }
                    // Restaura a lista original
                    ideiasAdapter.submitList(new ArrayList<>(ideiasAdapter.getCurrentList()));
                });
                snackbar.show();

                // Agenda a exclusão real do Firestore
                Runnable deleteRunnable = () -> {
                    pendingDeletes.remove(ideiaParaExcluir.getId());
                    firestoreHelper.excluirIdeia(ideiaParaExcluir.getId(), res -> {
                        if (!res.isOk() && binding != null) {
                            Snackbar.make(binding.getRoot(), "Falha ao excluir. Tente novamente.", Snackbar.LENGTH_LONG).show();
                            // Se a exclusão falhar, a UI será corrigida na próxima atualização do listener do Firestore
                        }
                    });
                };
                pendingDeletes.put(ideiaParaExcluir.getId(), deleteRunnable);
                handler.postDelayed(deleteRunnable, DELETE_DELAY_MS);
            }
        }).attachToRecyclerView(binding.recyclerViewIdeias);
    }
}