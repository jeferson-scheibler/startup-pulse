package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // NOVO IMPORT

import com.example.startuppulse.data.models.Ideia; // NOVO IMPORT
import com.example.startuppulse.databinding.FragmentInvestidoresBinding;
import com.example.startuppulse.ui.investor.InvestidoresViewModel;
import com.example.startuppulse.ui.investor.InvestorAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestidoresFragment extends Fragment {

    private FragmentInvestidoresBinding binding;
    private InvestidoresViewModel viewModel;
    private InvestorAdapter investorAdapter;
    private NavController navController;

    // Lista de ideias para o filtro
    private List<Ideia> userIdeias;
    // Texto padrão para "Todos"
    private static final String FILTRO_TODOS = "Todos os Investidores";

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

        setupRecyclerView();
        setupClickListeners(); // NOVO
        setupObservers();
    }

    private void setupClickListeners() {
        // Clique para ir ao cadastro de investidor (da tela de readiness)
        binding.btnCadastrarInvestidor.setOnClickListener(v -> {
            navController.navigate(R.id.action_investidoresFragment_to_investorTypeChoiceFragment);
        });

        // Listener para o dropdown de filtro
        binding.autocompleteIdeiaFilter.setOnItemClickListener((parent, v, position, id) -> {
            if (position == 0) {
                // Posição 0 é "Todos os Investidores"
                viewModel.setFilter(null);
            } else {
                // Posição 1 em diante é userIdeias.get(position - 1)
                Ideia selectedIdeia = userIdeias.get(position - 1);
                viewModel.setFilter(selectedIdeia);
            }
        });
    }

    private void setupRecyclerView() {
        investorAdapter = new InvestorAdapter(investor -> {
            Bundle args = new Bundle();
            args.putString("investorId", investor.getId());
            navController.navigate(R.id.action_investidoresFragment_to_investorDetailFragment, args);
        });

        binding.recyclerViewInvestors.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewInvestors.setAdapter(investorAdapter);

        // Listener de Rolagem Infinita
        binding.recyclerViewInvestors.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == investorAdapter.getItemCount() - 1) {
                    // Chegou ao fim da lista, carrega mais
                    viewModel.loadNextPage();
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.viewState.observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;

            // Gerenciador de visibilidade principal
            binding.progressBarLoadingInvestors.setVisibility(state == InvestidoresViewModel.ViewState.LOADING ? View.VISIBLE : View.GONE);
            binding.readinessContainer.setVisibility(state == InvestidoresViewModel.ViewState.SHOW_READINESS ? View.VISIBLE : View.GONE);

            // O container da lista é o PAI da lista e do "sem match"
            boolean showListContainer = (state == InvestidoresViewModel.ViewState.SHOW_INVESTORS ||
                    state == InvestidoresViewModel.ViewState.SHOW_NO_MATCHES);
            binding.containerInvestorList.setVisibility(showListContainer ? View.VISIBLE : View.GONE);

            // Gerenciador de visibilidade *dentro* do container da lista
            // Mostra a lista de investidores
            binding.recyclerViewInvestors.setVisibility(state == InvestidoresViewModel.ViewState.SHOW_INVESTORS ? View.VISIBLE : View.GONE);
            // Mostra o Lottie "Nenhum match"
            binding.containerNoMatches.setVisibility(state == InvestidoresViewModel.ViewState.SHOW_NO_MATCHES ? View.VISIBLE : View.GONE);
        });

        // (Seu observador de 'userReadyIdeias' para o dropdown não muda)
        viewModel.userReadyIdeias.observe(getViewLifecycleOwner(), ideias -> {
            this.userIdeias = ideias;
            if (ideias == null || ideias.isEmpty()) {
                binding.inputLayoutIdeiaFilter.setVisibility(View.GONE);
            } else {
                binding.inputLayoutIdeiaFilter.setVisibility(View.VISIBLE);

                ArrayList<String> ideiaNomes = new ArrayList<>();
                ideiaNomes.add(FILTRO_TODOS); // Opção padrão
                for (Ideia ideia : ideias) {
                    ideiaNomes.add(ideia.getNome());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, ideiaNomes);
                ((AutoCompleteTextView) binding.inputLayoutIdeiaFilter.getEditText()).setAdapter(adapter);
                binding.autocompleteIdeiaFilter.setText(FILTRO_TODOS, false);
            }
        });

        // --- OBSERVE A SIMPLIFICAÇÃO ---
        // O observador de 'investors' agora SÓ atualiza o adapter.
        // Ele não mexe mais na visibilidade (o viewState faz isso).
        viewModel.investors.observe(getViewLifecycleOwner(), investors -> {
            investorAdapter.submitList(investors);
        });
        // --- FIM DA SIMPLIFICAÇÃO ---

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