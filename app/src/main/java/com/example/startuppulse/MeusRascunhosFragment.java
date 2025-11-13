package com.example.startuppulse;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.example.startuppulse.databinding.DialogConfirmDeleteBinding;
import com.example.startuppulse.databinding.FragmentMeusRascunhosBinding;
import com.example.startuppulse.ui.ideias.MeusRascunhosViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MeusRascunhosFragment extends Fragment {

    private FragmentMeusRascunhosBinding binding;
    private MeusRascunhosViewModel viewModel;
    private IdeiasAdapter ideiasAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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

        setupRecyclerView();
        setupObservers();
        attachSwipeToDelete();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());
    }

    public static MeusRascunhosFragment newInstance(NavController navController) {
        MeusRascunhosFragment fragment = new MeusRascunhosFragment();
        return new MeusRascunhosFragment();
    }

    private void setupRecyclerView() {
        ideiasAdapter = new IdeiasAdapter(ideia -> {
            Bundle args = new Bundle();
            args.putString("ideiaId", ideia.getId());
            try {
                NavController navControllerCorreto = NavHostFragment.findNavController(getParentFragment());
                navControllerCorreto.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
            } catch (Exception e) {
                // Se isto falhar, o problema é mais profundo, mas pelo menos não cracha
                Log.e("MeusRascunhosFragment", "Falha CRÍTICA ao obter NavController do pai", e);
            }
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

                // --- LÓGICA DO SNACKBAR REMOVIDA ---
                // Mostra o diálogo de confirmação
                showConfirmDeleteDialog(ideiaParaExcluir, position);
            }
        }).attachToRecyclerView(binding.recyclerViewIdeias);
    }

    /**
     * Mostra um diálogo customizado para confirmar a exclusão de um rascunho.
     * @param ideia A ideia a ser excluída.
     * @param position A posição do item no adapter, para reverter a animação se o usuário cancelar.
     */
    private void showConfirmDeleteDialog(final Ideia ideia, final int position) {
        if (getContext() == null) return;

        // Infla o layout customizado
        DialogConfirmDeleteBinding dialogBinding = DialogConfirmDeleteBinding.inflate(LayoutInflater.from(getContext()));

        // Cria o MaterialAlertDialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogBinding.getRoot());
        builder.setCancelable(false); // Mantém a lógica de não fechar ao clicar fora

        // Cria o diálogo
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            // 1. Mantém o fundo transparente para o seu "Glass" CardView
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // --- ADIÇÃO: Configura o "DIM" (escurecimento) manualmente ---
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.dimAmount = 0.7f; // Define a força do escurecimento (0.0 = 0%, 1.0 = 100%)
            lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND; // Adiciona a flag que ativa o dim
            dialog.getWindow().setAttributes(lp);
            // --- Fim da Adição ---
        }

        // Configura o botão "Cancelar"
        dialogBinding.dialogButtonNegative.setOnClickListener(v -> {
            dialog.dismiss();
            if (ideiasAdapter != null) {
                ideiasAdapter.notifyItemChanged(position);
            }
        });

        // Configura o botão "Confirmar" (Excluir)
        dialogBinding.dialogButtonPositive.setOnClickListener(v -> {
            viewModel.deleteDraft(ideia.getId());
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}