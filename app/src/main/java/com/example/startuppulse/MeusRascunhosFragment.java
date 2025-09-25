package com.example.startuppulse;

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
import com.example.startuppulse.databinding.FragmentMeusRascunhosBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MeusRascunhosFragment extends Fragment implements IdeiasAdapter.OnIdeiaClickListener {

    private FragmentMeusRascunhosBinding binding;
    private IdeiasAdapter ideiasAdapter;
    private FirestoreHelper firestoreHelper;
    private com.google.firebase.firestore.ListenerRegistration rascunhosRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMeusRascunhosBinding.inflate(inflater, container, false);
        firestoreHelper = new FirestoreHelper();

        // Recycler
        binding.recyclerViewIdeias.setLayoutManager(new LinearLayoutManager(requireContext()));
        ideiasAdapter = new IdeiasAdapter(this, this);
        binding.recyclerViewIdeias.setAdapter(ideiasAdapter);

        // Swipe-to-delete apenas para o dono (essa aba já é do dono por definição)
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int position = vh.getBindingAdapterPosition();
                ideiasAdapter.iniciarExclusao(position);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerViewIdeias);

        // Pull-to-refresh reata o listener
        binding.swipeRefreshLayout.setOnRefreshListener(this::startRealtime);

        return binding.getRoot();
    }

    @Override public void onStart() { super.onStart(); startRealtime(); }
    @Override public void onStop() { super.onStop(); stopRealtime(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRealtime();
        binding = null;
    }

    private void startRealtime() {
        if (binding == null) return;
        if (rascunhosRegistration != null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String ownerId = user != null ? user.getUid() : "";
        binding.swipeRefreshLayout.setRefreshing(true);

        rascunhosRegistration = firestoreHelper.listenToMeusRascunhos(ownerId, (Result<List<Ideia>> r) -> {
            if (binding == null) return;
            binding.swipeRefreshLayout.setRefreshing(false);

            if (!r.isOk()) {
                Snackbar.make(binding.getRoot(), "Erro: " + r.error.getMessage(), Snackbar.LENGTH_LONG).show();
                return;
            }
            List<Ideia> ideias = r.data != null ? r.data : new ArrayList<>();
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

    private void stopRealtime() {
        if (rascunhosRegistration != null) {
            rascunhosRegistration.remove();
            rascunhosRegistration = null;
        }
    }

    // Clique abre em modo edição (é do dono)
    @Override
    public void onIdeiaClick(Ideia ideia) {
        android.content.Intent intent = new android.content.Intent(getActivity(), CanvasIdeiaActivity.class);
        intent.putExtra("ideia_id", ideia.getId());
        startActivity(intent);
    }
}