// MentorDetailActivity.java
package com.example.startuppulse;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class MentorDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_mentor_detail);

        Mentor mentor = (Mentor) getIntent().getSerializableExtra("mentor");
        if (mentor == null) { finish(); return; }

        TextView nome = findViewById(R.id.textNome);
        TextView cidade = findViewById(R.id.textCidade);
        com.google.android.material.chip.Chip chipProfissao = findViewById(R.id.chipProfissao);
        ImageView foto = findViewById(R.id.imageAvatar);
        ChipGroup chips = findViewById(R.id.chipsAreas);

        nome.setText(mentor.getNome());
        cidade.setText(mentor.getCidade() + ", " + mentor.getEstado());
        chipProfissao.setText(mentor.getProfissao());

        if (mentor.getImagem() != null && !mentor.getImagem().isEmpty()) {
            Glide.with(this).load(mentor.getImagem()).circleCrop().into(foto);
        }

        chips.removeAllViews();
        if (mentor.getAreas() != null) {
            for (String a : mentor.getAreas()) {
                Chip c = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
                c.setText(a);
                c.setClickable(false);
                c.setCheckable(false);
                c.setChipIconResource(R.drawable.ic_tag);
                c.setChipIconTintResource(androidx.appcompat.R.color.material_grey_600);
                chips.addView(c);
            }
        }
    }
}