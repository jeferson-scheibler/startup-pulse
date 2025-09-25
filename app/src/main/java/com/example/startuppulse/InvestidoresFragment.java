package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.startuppulse.databinding.FragmentInvestidoresBinding; // Importação correta
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvestidoresFragment extends Fragment implements InvestorAdapter.OnInvestorClickListener {

    private FragmentInvestidoresBinding binding; // Usando View Binding

    // Adapters
    private ReadinessTaskAdapter readinessAdapter;
    private InvestorAdapter investorAdapter;

    // Firebase e Dados
    private FirestoreHelper firestoreHelper;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInvestidoresBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeFirebase();
        setupRecyclerViews();
        loadUserReadinessData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevenir memory leaks
    }

    private void initializeFirebase() {
        firestoreHelper = new FirestoreHelper();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupRecyclerViews() {
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewInvestors.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    /**
     * Ponto de entrada: Carrega a ideia principal do usuário e decide qual tela mostrar.
     */
    private void loadUserReadinessData() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            // Mostra o painel de prontidão com dados nulos (score 0%)
            updateReadinessUI(ReadinessCalculator.calculate(null));
            return;
        }

        firestoreHelper.getIdeiasForOwner(currentUser.getUid(), result -> {
            if (!isAdded() || binding == null) return;

            Ideia ideiaPrincipal = (result.isOk() && result.data != null && !result.data.isEmpty())
                    ? result.data.get(0) : null;

            // **NOVA LÓGICA INTEGRADA**
            // Se a ideia já estiver marcada como pronta, vai direto para a lista de investidores.
            if (ideiaPrincipal != null && ideiaPrincipal.isProntaParaInvestidores()) {
                showInvestorListView();
                loadInvestors();
            } else {
                // Caso contrário, mostra o painel de prontidão com o score atual.
                ReadinessData readinessData = ReadinessCalculator.calculate(ideiaPrincipal);
                showReadinessView();
                updateReadinessUI(readinessData);
            }
        });
    }

    /**
     * Carrega os dados dos investidores do Firestore.
     */
    private void loadInvestors() {
        firestoreHelper.getInvestidores(result -> {
            if (!isAdded() || binding == null) return;

            binding.progressBarLoadingInvestors.setVisibility(View.GONE);

            if (result.isOk() && result.data != null) {
                investorAdapter = new InvestorAdapter(result.data, this);
                binding.recyclerViewInvestors.setAdapter(investorAdapter);
            } else {
                Toast.makeText(getContext(), "Falha ao carregar investidores.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Atualiza a UI do Painel de Prontidão com os dados calculados.
     */
    private void updateReadinessUI(ReadinessData data) {
        binding.progressBarScore.setProgress(data.getScore());
        binding.textViewScore.setText(String.format(Locale.getDefault(), "%d%%", data.getScore()));

        List<ReadinessTask> tasks = new ArrayList<>();
        tasks.add(new ReadinessTask("Preencha todos os blocos do seu Canvas da Ideia.", data.isCanvasCompleto()));
        tasks.add(new ReadinessTask("Cadastre os membros da sua equipe.", data.isEquipeDefinida()));
        tasks.add(new ReadinessTask("Documente suas métricas e tração inicial.", data.isMetricasIniciais()));
        tasks.add(new ReadinessTask("Receba uma avaliação de um mentor.", data.isValidadoPorMentor()));
        tasks.add(new ReadinessTask("Anexe seu Pitch Deck.", data.hasPitchDeck()));

        readinessAdapter = new ReadinessTaskAdapter(tasks, requireContext());
        binding.recyclerViewTasks.setAdapter(readinessAdapter);
    }

    /**
     * Configura a visibilidade para mostrar o Painel de Prontidão.
     */
    private void showReadinessView() {
        binding.readinessContainer.setVisibility(View.VISIBLE);
        binding.containerInvestorList.setVisibility(View.GONE);
        binding.progressBarLoadingInvestors.setVisibility(View.GONE);
    }

    /**
     * Configura a visibilidade para mostrar a Lista de Investidores.
     */
    private void showInvestorListView() {
        binding.readinessContainer.setVisibility(View.GONE);
        binding.containerInvestorList.setVisibility(View.VISIBLE);
        binding.progressBarLoadingInvestors.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInvestorClick(Investor investor) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), InvestorDetailActivity.class);
        intent.putExtra(InvestorDetailActivity.EXTRA_INVESTOR, investor);
        startActivity(intent);
    }
}