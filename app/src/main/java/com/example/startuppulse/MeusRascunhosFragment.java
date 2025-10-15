package com.example.startuppulse;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.databinding.FragmentMeusRascunhosBinding;
import com.example.startuppulse.ui.ideias.IdeiasViewModel;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MeusRascunhosFragment extends Fragment implements IdeiasAdapter.OnIdeiaClickListener {

    private FragmentMeusRascunhosBinding binding;
    private IdeiasViewModel viewModel;
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

        viewModel = new ViewModelProvider(requireParentFragment()).get(IdeiasViewModel.class);
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (IllegalStateException e) {
            Log.e("MeusRascunhosFragment", "NavController não encontrado", e);
        }

        setupRecyclerView();
        setupObservers();
        attachSwipeToDelete();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupRecyclerView() {
        ideiasAdapter = new IdeiasAdapter(this);
        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);
    }

    private void setupObservers() {
        viewModel.draftIdeias.observe(getViewLifecycleOwner(), rascunhos -> {
            if (rascunhos != null) {
                ideiasAdapter.submitList(rascunhos);
                binding.viewEmptyStateIdeias.setVisibility(rascunhos.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerViewIdeias.setVisibility(rascunhos.isEmpty() ? View.GONE : View.VISIBLE);
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
        if (navController != null) {
            Bundle args = new Bundle();
            args.putString("ideiaId", ideia.getId());
            // A flag 'isReadOnly' é false por padrão, o que está correto para um rascunho.
            navController.navigate(R.id.action_global_to_canvasIdeiaFragment, args);
        }
    }

    private void attachSwipeToDelete() {
        // Prepara os drawables para o visual do swipe
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
        ColorDrawable background = new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.colorError));

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                final Ideia ideiaParaExcluir = ideiasAdapter.getIdeiaAt(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Confirmar Exclusão")
                        .setMessage("Tem certeza que quer excluir o rascunho '" + ideiaParaExcluir.getNome() + "'?")
                        .setPositiveButton("Excluir", (dialog, which) -> {
                            viewModel.deleteIdeia(ideiaParaExcluir.getId());
                            if (binding != null) {
                                Snackbar.make(binding.getRoot(), "Rascunho excluído", Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> ideiasAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> ideiasAdapter.notifyItemChanged(position))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (icon != null) {
                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();

                    if (dX < 0) { // Deslizando para a esquerda
                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                        background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                                itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    } else {
                        background.setBounds(0, 0, 0, 0);
                    }
                    background.draw(c);
                    icon.draw(c);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.recyclerViewIdeias);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}