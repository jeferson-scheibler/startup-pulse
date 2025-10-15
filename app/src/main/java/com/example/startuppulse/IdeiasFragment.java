package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
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

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IdeiasFragment extends Fragment implements IdeiasAdapter.OnIdeiaClickListener {

    private FragmentIdeiasBinding binding;
    private IdeiasViewModel viewModel;
    private IdeiasAdapter ideiasAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingDeletes = new HashMap<>();
    private static final long DELETE_DELAY_MS = 3500L;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentIdeiasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            // Loga o erro caso o NavController ainda não seja encontrado, para facilitar o debug.
            Log.e("IdeiasFragment", "NavController não encontrado", e);
        }

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
        viewModel.publicIdeias.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.isOk()) {
                // Sucesso: extrai a lista de dados.
                List<Ideia> ideias = result.data;
                if (ideias == null) {
                    ideias = new ArrayList<>(); // Garante que a lista nunca seja nula.
                }

                ideiasAdapter.submitList(ideias);
                binding.viewEmptyStateIdeias.setVisibility(ideias.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerViewIdeias.setVisibility(ideias.isEmpty() ? View.GONE : View.VISIBLE);
                binding.errorState.setVisibility(View.GONE); // Esconde o estado de erro

            } else {
                // Erro: mostra uma mensagem de erro e esconde a lista.
                Log.e("IdeiasFragment", "Erro ao carregar ideias públicas", result.error);
                ideiasAdapter.submitList(new ArrayList<>()); // Limpa a lista
                binding.viewEmptyStateIdeias.setVisibility(View.GONE);
                binding.recyclerViewIdeias.setVisibility(View.GONE);
                binding.errorState.setVisibility(View.VISIBLE); // Mostra o estado de erro
                binding.errorText.setText("Não foi possível carregar as ideias. Verifique sua conexão.");
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && binding.swipeRefreshLayout.isRefreshing() != isLoading) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
    }

    @Override
    public void onIdeiaClick(Ideia ideia) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;

        boolean isOwner = uid != null && uid.equals(ideia.getOwnerId());
        boolean isMentor = uid != null && ideia.getMentorId() != null && uid.equals(ideia.getMentorId());

        if (isOwner || isMentor) {
            // MODO EDIÇÃO: Navega para o canvas passando o ID da ideia.
            Bundle args = new Bundle();
            args.putString("ideiaId", ideia.getId());
            navController.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
            return;
        }

        // MODO LEITURA: Verifica o limite de acesso diário.
        LimiteHelper.verificarAcessoIdeia(requireContext(), new LimiteHelper.LimiteCallback() {
            @Override
            public void onPermitido() {
                Bundle args = new Bundle();
                args.putString("ideiaId", ideia.getId());
                args.putBoolean("isReadOnly", true); // Passa o argumento de "apenas leitura"
                navController.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
            }

            @Override
            public void onNegado(String mensagem) {
                // A sua lógica original para mostrar o diálogo de limite de acesso continua aqui.
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
                    return;
                }
                String uid = user.getUid();
                LimiteHelper.getProximaDataAcessoFormatada(uid, dataFormatada -> {
                    if(isAdded()) {
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Limite Máximo de Acesso Diário")
                                .setMessage(
                                        "Você já acessou uma ideia hoje.\n\n" +
                                                "Limite Diário: 1\n\n" +
                                                "Próximo acesso liberado: " + dataFormatada + "\n\n" +
                                                "Deseja acessar ideias ilimitadas?\nTorne-se Premium."
                                )
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
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