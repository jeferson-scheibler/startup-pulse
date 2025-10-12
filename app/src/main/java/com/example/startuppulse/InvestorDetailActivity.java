package com.example.startuppulse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.startuppulse.data.Investor;
// Importe a classe de binding gerada a partir do seu layout XML
import com.example.startuppulse.databinding.FragmentInvestorDetailBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class InvestorDetailActivity extends AppCompatActivity {

    public static final String EXTRA_INVESTOR = "EXTRA_INVESTOR";
    // 1. Declare a variável para o binding
    private FragmentInvestorDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 2. Infla o layout usando o View Binding
        binding = FragmentInvestorDetailBinding.inflate(getLayoutInflater());
        // 3. Define o root do binding como o content view
        setContentView(binding.getRoot());

        Investor investor = (Investor) getIntent().getSerializableExtra(EXTRA_INVESTOR);

        if (investor == null) {
            Toast.makeText(this, "Erro ao carregar dados do investidor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        populateUI(investor);
    }

    private void populateUI(Investor investor) {
        // 4. Acesse as views diretamente através do objeto binding (sem findViewById)
        binding.investorNameTextView.setText(investor.getNome());
        binding.investorBioTextView.setText(investor.getBio());
        binding.investorTeseTextView.setText(investor.getTese());

        Glide.with(this)
                .load(investor.getFotoUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.investorPhotoImageView);

        // Oculta a seção de "Faixa de Investimento" que não está no seu modelo de dados
        binding.labelFaixaInvestimento.setVisibility(View.GONE);
        binding.investorCheckTextView.setVisibility(View.GONE);

        // Popula os chips de áreas e estágios
        addChipsToGroup(binding.chipGroupAreas, investor.getAreas());
        addChipsToGroup(binding.chipGroupEstagios, investor.getEstagios());

        // Configura o botão do LinkedIn
        if (investor.getLinkedinUrl() != null && !investor.getLinkedinUrl().isEmpty()) {
            binding.linkedinButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(investor.getLinkedinUrl()));
                startActivity(intent);
            });
        } else {
            binding.linkedinButton.setVisibility(View.GONE);
        }
    }

    private void addChipsToGroup(ChipGroup chipGroup, List<String> items) {
        if (items == null || items.isEmpty()) {
            chipGroup.setVisibility(View.GONE);
            return;
        }
        chipGroup.removeAllViews();
        for (String item : items) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chipGroup.addView(chip);
        }
    }
}