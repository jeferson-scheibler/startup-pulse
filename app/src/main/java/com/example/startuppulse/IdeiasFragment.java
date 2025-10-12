package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.FragmentIdeiasBinding;
import com.example.startuppulse.ui.ideias.IdeiasViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdeiasFragment extends Fragment implements IdeiasAdapter.OnIdeiaClickListener {

    private FragmentIdeiasBinding binding;
    private IdeiasViewModel viewModel;
    private IdeiasAdapter ideiasAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingDeletes = new HashMap<>();
    private static final long DELETE_DELAY_MS = 3500L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment()).get(IdeiasViewModel.class);

        setupRecyclerView();
        setupObservers();
        attachSwipeToDelete();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupRecyclerView() {
        // CORRIGIDO: O construtor do adapter agora só precisa do listener.
        ideiasAdapter = new IdeiasAdapter(this);
        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);
    }

    private void setupObservers() {
        viewModel.publicIdeias.observe(getViewLifecycleOwner(), ideias -> {
            if (ideias != null) {
                // CORRIGIDO: Usamos submitList() para atualizar a lista.
                // O ListAdapter cuidará das animações e da performance.
                ideiasAdapter.submitList(ideias);

                binding.viewEmptyStateIdeias.setVisibility(ideias.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerViewIdeias.setVisibility(ideias.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding.swipeRefreshLayout.isRefreshing() != isLoading) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
    }

    @Override
    public void onIdeiaClick(Ideia ideia) {
        // Sua lógica de clique está correta e permanece a mesma.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        boolean isOwner = uid != null && uid.equals(ideia.getOwnerId());
        boolean isMentor = uid != null && ideia.getMentorId() != null && uid.equals(ideia.getMentorId());

        if (isOwner || isMentor) {
            Intent intent = new Intent(getActivity(), CanvasIdeiaActivity.class);
            intent.putExtra("ideia_id", ideia.getId());
            startActivity(intent);
            return;
        }

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
                // ... (sua lógica de negação com dialog)
            }
        });
    }

    private void attachSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return 0;

                // CORRIGIDO: Usamos o método público getIdeiaAt para pegar o item.
                Ideia ideia = ideiasAdapter.getIdeiaAt(position);
                String uid = FirebaseAuth.getInstance().getUid();
                boolean isOwner = uid != null && uid.equals(ideia.getOwnerId());

                return isOwner ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                final Ideia ideiaParaExcluir = ideiasAdapter.getIdeiaAt(position);

                // O ListAdapter já cuida da remoção visual, não precisamos manipular a lista manualmente.
                // Apenas mostramos o Snackbar.

                Snackbar snackbar = Snackbar.make(binding.getRoot(), "Ideia excluída", Snackbar.LENGTH_LONG);
                snackbar.setAction("Desfazer", v -> {
                    // Cancela a exclusão agendada no ViewModel.
                    Runnable pendingDelete = pendingDeletes.remove(ideiaParaExcluir.getId());
                    if (pendingDelete != null) {
                        handler.removeCallbacks(pendingDelete);
                    }
                    // O listener do Firestore irá restaurar a UI automaticamente.
                });

                // Adicionamos um callback para saber quando o Snackbar é dispensado (sem ser pelo "Desfazer")
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            // Se o snackbar sumiu (sem clicar em desfazer), deleta a ideia.
                            viewModel.deleteIdeia(ideiaParaExcluir.getId());
                        }
                    }
                });

                snackbar.show();
            }
        }).attachToRecyclerView(binding.recyclerViewIdeias);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}