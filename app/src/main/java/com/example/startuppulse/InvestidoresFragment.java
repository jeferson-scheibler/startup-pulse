package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvestidoresFragment extends Fragment implements InvestorAdapter.OnInvestorClickListener {

    private static final int SCORE_MINIMO_PARA_INVESTIDORES = 75;

    // UI Components
    private ConstraintLayout readinessContainer;
    private LinearLayout investorListContainer;
    private ProgressBar progressBarScore, progressBarLoadingInvestors;
    private TextView textViewScore;
    private RecyclerView recyclerViewTasks, recyclerViewInvestors;

    // Adapters
    private ReadinessTaskAdapter readinessAdapter;
    private InvestorAdapter investorAdapter;

    // Firebase
    private FirestoreHelper firestoreHelper;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_investidores, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeFirebase();
        initializeViews(view);
        setupRecyclerViews();
        loadUserReadinessData();
    }

    private void initializeFirebase() {
        firestoreHelper = new FirestoreHelper();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void initializeViews(View view) {
        readinessContainer = view.findViewById(R.id.readiness_container);
        investorListContainer = view.findViewById(R.id.container_investor_list);

        // O resto das views está dentro do readinessContainer
        progressBarScore = readinessContainer.findViewById(R.id.progress_bar_score);
        textViewScore = readinessContainer.findViewById(R.id.text_view_score);
        progressBarLoadingInvestors = readinessContainer.findViewById(R.id.progress_bar_loading_investors);
    }

    private void setupRecyclerViews() {
        recyclerViewTasks = readinessContainer.findViewById(R.id.recycler_view_tasks);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(requireContext()));

        recyclerViewInvestors = investorListContainer.findViewById(R.id.recycler_view_investors);
        recyclerViewInvestors.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void loadUserReadinessData() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            updateReadinessUI(ReadinessCalculator.calculate(null));
            return;
        }

        firestoreHelper.getIdeiasForOwner(currentUser.getUid(), result -> {
            if (!isAdded()) return;

            Ideia ideiaPrincipal = (result.isOk() && result.data != null && !result.data.isEmpty())
                    ? result.data.get(0) : null;

            ReadinessData readinessData = ReadinessCalculator.calculate(ideiaPrincipal);

            if (readinessData.getScore() >= SCORE_MINIMO_PARA_INVESTIDORES) {
                showInvestorListView();
                loadInvestors();
            } else {
                showReadinessView();
                updateReadinessUI(readinessData);
            }
        });
    }

    private void loadInvestors() {
        firestoreHelper.getInvestidores(result -> {
            if (!isAdded()) return;

            progressBarLoadingInvestors.setVisibility(View.GONE);

            if (result.isOk() && result.data != null) {
                investorAdapter = new InvestorAdapter(result.data, this);
                recyclerViewInvestors.setAdapter(investorAdapter);
            } else {
                Toast.makeText(getContext(), "Falha ao carregar investidores.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateReadinessUI(ReadinessData data) {
        progressBarScore.setProgress(data.getScore());
        textViewScore.setText(String.format(Locale.getDefault(), "%d%%", data.getScore()));

        List<ReadinessTask> tasks = new ArrayList<>();
        tasks.add(new ReadinessTask("Preencha todos os blocos do seu Canvas da Ideia.", data.isCanvasCompleto()));
        tasks.add(new ReadinessTask("Cadastre os membros da sua equipe.", data.isEquipeDefinida()));
        tasks.add(new ReadinessTask("Documente suas métricas e tração inicial.", data.isMetricasIniciais()));
        tasks.add(new ReadinessTask("Receba uma avaliação de um mentor.", data.isValidadoPorMentor()));
        tasks.add(new ReadinessTask("Anexe seu Pitch Deck.", data.hasPitchDeck()));

        readinessAdapter = new ReadinessTaskAdapter(tasks, requireContext());
        recyclerViewTasks.setAdapter(readinessAdapter);
    }

    private void showReadinessView() {
        readinessContainer.findViewById(R.id.text_view_titulo).setVisibility(View.VISIBLE);
        // ... mostrar todas as views do readiness
        investorListContainer.setVisibility(View.GONE);
        progressBarLoadingInvestors.setVisibility(View.GONE);
    }

    private void showInvestorListView() {
        // Esconde todas as views do painel de prontidão
        readinessContainer.findViewById(R.id.text_view_titulo).setVisibility(View.GONE);
        readinessContainer.findViewById(R.id.text_view_subtitulo).setVisibility(View.GONE);
        readinessContainer.findViewById(R.id.progress_bar_score).setVisibility(View.GONE);
        readinessContainer.findViewById(R.id.text_view_score).setVisibility(View.GONE);
        readinessContainer.findViewById(R.id.text_view_proximos_passos_titulo).setVisibility(View.GONE);
        readinessContainer.findViewById(R.id.recycler_view_tasks).setVisibility(View.GONE);

        // Mostra o contêiner da lista de investidores e o progresso
        investorListContainer.setVisibility(View.VISIBLE);
        progressBarLoadingInvestors.setVisibility(View.VISIBLE);
    }
    @Override
    public void onInvestorClick(Investor investor) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), InvestorDetailActivity.class);
        intent.putExtra(InvestorDetailActivity.EXTRA_INVESTOR, investor);
        startActivity(intent);
    }
}