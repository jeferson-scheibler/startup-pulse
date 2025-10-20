// app/src/main/java/com/example/startuppulse/IdeiasFragment.java
package com.example.startuppulse;

import android.os.Bundle;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.databinding.FragmentIdeiasBinding;
import com.example.startuppulse.ui.ideias.IdeiasViewModel;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IdeiasFragment extends Fragment {

    private FragmentIdeiasBinding binding;
    private IdeiasViewModel viewModel;
    private IdeiasAdapter ideiasAdapter;
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
        navController = NavHostFragment.findNavController(this);
        viewModel = new ViewModelProvider(this).get(IdeiasViewModel.class); // Obtém o ViewModel

        setupRecyclerView();
        setupObservers();
        attachSwipeToDelete();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupRecyclerView() {
        // A navegação agora é tratada pelo ViewModel, o adapter apenas reporta o clique
        ideiasAdapter = new IdeiasAdapter(ideia -> viewModel.onIdeiaClicked(ideia, requireContext()));
        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);
    }

    private void setupObservers() {
        viewModel.publicIdeias.observe(getViewLifecycleOwner(), result -> {
            if (binding == null) return;

            if (result instanceof Result.Success) {
                List<Ideia> ideias = ((Result.Success<List<Ideia>>) result).data;
                ideias = (ideias == null) ? new ArrayList<>() : ideias;

                ideiasAdapter.submitList(ideias);
                binding.viewEmptyStateIdeias.setVisibility(ideias.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerViewIdeias.setVisibility(ideias.isEmpty() ? View.GONE : View.VISIBLE);
                binding.errorState.setVisibility(View.GONE);

            } else if (result instanceof Result.Error) {
                Log.e("IdeiasFragment", "Erro ao carregar ideias", ((Result.Error<List<Ideia>>) result).error);
                ideiasAdapter.submitList(new ArrayList<>());
                binding.viewEmptyStateIdeias.setVisibility(View.GONE);
                binding.recyclerViewIdeias.setVisibility(View.GONE);
                binding.errorState.setVisibility(View.VISIBLE);
                binding.errorText.setText("Não foi possível carregar as ideias. Verifique sua conexão.");
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && binding.swipeRefreshLayout.isRefreshing() != isLoading) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
            }
        });

        viewModel.navigateToCanvas.observe(getViewLifecycleOwner(), event -> {
            IdeiasViewModel.NavigationEvent navEvent = event.getContentIfNotHandled();
            if (navEvent != null) {
                Bundle args = new Bundle();
                args.putString("ideiaId", navEvent.ideiaId);
                args.putBoolean("isReadOnly", navEvent.isReadOnly);
                navController.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
            }
        });

        viewModel.showLimitDialog.observe(getViewLifecycleOwner(), event -> {
            String dataFormatada = event.getContentIfNotHandled();
            if (dataFormatada != null && isAdded()) {
                new AlertDialog.Builder(requireContext())
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

        viewModel.toastEvent.observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void attachSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return 0;

                Ideia ideia = ideiasAdapter.getIdeiaAt(position);
                // A lógica de permissão agora está no ViewModel
                return viewModel.canDeleteIdeia(ideia) ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                final Ideia ideiaParaExcluir = ideiasAdapter.getIdeiaAt(position);

                // Mostra um Snackbar para confirmar a exclusão.
                Snackbar.make(binding.getRoot(), "Ideia movida para a lixeira", Snackbar.LENGTH_LONG)
                        .setAction("Desfazer", v -> {
                            // Se desfeito, o listener do Firestore irá restaurar a UI, não precisamos fazer nada aqui.
                            // Esta é apenas uma ilusão de ótica para o usuário. A exclusão real é adiada.
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                // Se o snackbar sumiu sem clicar em "Desfazer", a exclusão é confirmada.
                                if (event != DISMISS_EVENT_ACTION) {
                                    viewModel.deleteIdeia(ideiaParaExcluir.getId());
                                }
                            }
                        }).show();
            }
        }).attachToRecyclerView(binding.recyclerViewIdeias);
    }
}