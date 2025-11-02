package com.example.startuppulse.ui.investor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.startuppulse.R;
import com.example.startuppulse.databinding.FragmentInvestorTypeChoiceBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InvestorTypeChoiceFragment extends Fragment {

    private FragmentInvestorTypeChoiceBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvestorTypeChoiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ação do botão "Voltar" na toolbar
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        // Ação do Card "Investidor Físico"
        binding.cardInvestidorIndividual.setOnClickListener(v -> {
            navigateToDetails("INDIVIDUAL");
        });

        // Ação do Card "Fundo ou Empresa"
        binding.cardInvestidorEmpresa.setOnClickListener(v -> {
            navigateToDetails("FIRM");
        });
    }

    /**
     * Navega para a próxima tela de detalhes, passando o tipo de investidor como argumento.
     */
    private void navigateToDetails(String investorType) {
        Bundle args = new Bundle();
        args.putString("investorType", investorType);

        // Navega para a próxima ação (que criaremos no nav_graph)
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_investorTypeChoiceFragment_to_investorRegisterDetailsFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}