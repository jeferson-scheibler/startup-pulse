// app/src/main/java/com/example/startuppulse/InvestidoresFragment.java

package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.databinding.FragmentInvestidoresBinding;
import com.example.startuppulse.ui.investor.InvestidoresViewModel;
import com.example.startuppulse.ui.investor.InvestorAdapter;

// Imports removidos:
// import java.util.ArrayList;
// import java.util.Locale;
import java.util.List; // Este é mantido, provavelmente usado pelo investors.observe

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestidoresFragment extends Fragment {

    private FragmentInvestidoresBinding binding;
    private InvestidoresViewModel viewModel;
    private InvestorAdapter investorAdapter;
    // Removido: private ReadinessTaskAdapter readinessAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvestidoresBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(InvestidoresViewModel.class);
        navController = NavHostFragment.findNavController(this);

        binding.btnCadastrarInvestidor.setOnClickListener(v -> {
            navController.navigate(R.id.action_investidoresFragment_to_investorTypeChoiceFragment);
        });

        setupRecyclerViews();
        setupObservers();
    }

    private void setupRecyclerViews() {
        // O click listener foi movido para o ViewHolder do adapter
        investorAdapter = new InvestorAdapter(investor -> {
            Bundle args = new Bundle();
            args.putString("investorId", investor.getId());
            navController.navigate(R.id.action_investidoresFragment_to_investorDetailFragment, args);
        });

        // Removido: binding.recyclerViewTasks.setLayoutManager(...);
        binding.recyclerViewInvestors.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewInvestors.setAdapter(investorAdapter);
    }

    private void setupObservers() {
        viewModel.viewState.observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;
            binding.progressBarLoadingInvestors.setVisibility(state == InvestidoresViewModel.ViewState.LOADING ? View.VISIBLE : View.GONE);
            binding.readinessContainer.setVisibility(state == InvestidoresViewModel.ViewState.SHOW_READINESS ? View.VISIBLE : View.GONE);
            binding.containerInvestorList.setVisibility(state == InvestidoresViewModel.ViewState.SHOW_INVESTORS ? View.VISIBLE : View.GONE);
            binding.containerCadastroInvestidor.setVisibility(state == InvestidoresViewModel.ViewState.SHOW_READINESS ? View.VISIBLE : View.GONE);
        });

        viewModel.investors.observe(getViewLifecycleOwner(), investors -> {
            investorAdapter.submitList(investors); // Assumindo que o InvestorAdapter usa ListAdapter
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}