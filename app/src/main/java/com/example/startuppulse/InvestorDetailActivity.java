package com.example.startuppulse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class InvestorDetailActivity extends AppCompatActivity {

    public static final String EXTRA_INVESTOR = "EXTRA_INVESTOR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_investor_detail);

        Investor investor = (Investor) getIntent().getSerializableExtra(EXTRA_INVESTOR);

        if (investor == null) {
            Toast.makeText(this, "Erro ao carregar dados do investidor.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        populateUI(investor);
    }

    private void populateUI(Investor investor) {
        ShapeableImageView photo = findViewById(R.id.image_view_investor_photo);
        TextView name = findViewById(R.id.text_view_investor_name);
        TextView bio = findViewById(R.id.text_view_investor_bio);
        TextView tese = findViewById(R.id.text_view_investor_tese);
        TextView check = findViewById(R.id.text_view_investor_check);
        ChipGroup areasGroup = findViewById(R.id.chip_group_areas);
        ChipGroup estagiosGroup = findViewById(R.id.chip_group_estagios);
        Button linkedinButton = findViewById(R.id.button_linkedin);

        name.setText(investor.getNome());
        bio.setText(investor.getBio());
        tese.setText(investor.getTese());

        Glide.with(this)
                .load(investor.getFotoUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(photo);

        // Formata a faixa de investimento como moeda
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        // Long checkMin = investor.getCheckMin(); // Assumindo que os campos são Long no Firestore
        // Long checkMax = investor.getCheckMax();
        // String checkText = String.format("%s - %s", currencyFormat.format(checkMin), currencyFormat.format(checkMax));
        // check.setText(checkText);
        check.setText("Faixa de Investimento (Exemplo)");


        // Popula os chips de áreas e estágios
        addChipsToGroup(areasGroup, investor.getAreas());
        addChipsToGroup(estagiosGroup, investor.getEstagios());

        // Configura o botão do LinkedIn
        if (investor.getLinkedinUrl() != null && !investor.getLinkedinUrl().isEmpty()) {
            linkedinButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(investor.getLinkedinUrl()));
                startActivity(intent);
            });
        } else {
            linkedinButton.setVisibility(View.GONE);
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