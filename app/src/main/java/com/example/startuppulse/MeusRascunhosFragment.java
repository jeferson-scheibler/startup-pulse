package com.example.startuppulse;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Ideia;
import com.example.startuppulse.databinding.FragmentMeusRascunhosBinding;
import com.example.startuppulse.ui.ideias.MeusRascunhosViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MeusRascunhosFragment extends Fragment {

    private FragmentMeusRascunhosBinding binding;
    private MeusRascunhosViewModel viewModel;
    private IdeiasAdapter ideiasAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMeusRascunhosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MeusRascunhosViewModel.class);
        navController = NavHostFragment.findNavController(this);

        setupRecyclerView();
        setupObservers();
        attachSwipeToDelete();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupRecyclerView() {
        ideiasAdapter = new IdeiasAdapter(ideia -> {
            // Rascunhos sempre abrem em modo de edição
            Bundle args = new Bundle();
            args.putString("ideiaId", ideia.getId());
            navController.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
        });
        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);
    }

    private void setupObservers() {
        viewModel.draftIdeiasResult.observe(getViewLifecycleOwner(), result -> {
            if (binding == null) return;

            // Esconde todos os estados de UI por padrão
            binding.viewEmptyStateIdeias.setVisibility(View.GONE);
            binding.recyclerViewIdeias.setVisibility(View.GONE);
            binding.errorState.setVisibility(View.GONE);

            if (result instanceof Result.Success) {
                // Se for sucesso, extrai a lista de forma segura
                List<Ideia> rascunhos = ((Result.Success<List<Ideia>>) result).data;
                rascunhos = (rascunhos == null) ? new ArrayList<>() : rascunhos;

                ideiasAdapter.submitList(rascunhos);

                // Verifica se a lista está vazia
                if (rascunhos.isEmpty()) {
                    binding.viewEmptyStateIdeias.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewIdeias.setVisibility(View.VISIBLE);
                }
            } else if (result instanceof Result.Error) {
                // Se for erro, mostra a UI de erro
                binding.errorState.setVisibility(View.VISIBLE);
                String errorMsg = ((Result.Error<List<Ideia>>) result).error.getMessage();
                binding.errorText.setText("Não foi possível carregar seus rascunhos: " + errorMsg);
                Log.e("MeusRascunhosFragment", "Erro ao carregar rascunhos", ((Result.Error<List<Ideia>>) result).error);
            }
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && binding.swipeRefreshLayout.isRefreshing() != isLoading) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
            }
        });
    }

    private void attachSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Não suportamos reordenar
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Pega a ideia que foi arrastada
                final Ideia ideiaParaExcluir = ideiasAdapter.getIdeiaAt(position);

                // Mostra um Snackbar com a opção de "Desfazer"
                Snackbar.make(binding.getRoot(), "Rascunho excluído", Snackbar.LENGTH_LONG)
                        .setAction("Desfazer", v -> {
                            // Se o utilizador clica em "Desfazer", não fazemos nada. A exclusão é cancelada.
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                // Se o Snackbar sumiu sem que o "Desfazer" fosse clicado,
                                // a exclusão é confirmada e enviada para o ViewModel.
                                if (event != DISMISS_EVENT_ACTION) {
                                    viewModel.deleteDraft(ideiaParaExcluir.getId());
                                }
                            }
                        }).show();
            }
        }).attachToRecyclerView(binding.recyclerViewIdeias);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}