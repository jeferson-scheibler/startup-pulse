package com.example.startuppulse;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    LinearLayout navButtonIdeias, navButtonMentores, navButtonPerfil;
    ImageView navIconIdeias, navIconMentores, navIconPerfil;
    TextView navTextIdeias, navTextMentores, navTextPerfil;
    FloatingActionButton fabAddIdea, fabAddMentor;
    LinearLayout navButtonInvestidores;
    ImageView navIconInvestidores;
    TextView navTextInvestidores;
    private boolean isMentor = false;
    private FirebaseUser currentUser;
    private TextView offlineIndicatorBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main deve ter um FrameLayout com id fragment_container
        offlineIndicatorBar = findViewById(R.id.offline_indicator_bar);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Indicador de offline/online
        NetworkManager networkManager = NetworkManager.getInstance(getApplicationContext());
        networkManager.isNetworkAvailable().observe(this, isAvailable ->
                offlineIndicatorBar.setVisibility(Boolean.TRUE.equals(isAvailable) ? View.GONE : View.VISIBLE)
        );

        // Referências UI
        fabAddIdea = findViewById(R.id.fab_add_idea);
        fabAddMentor = findViewById(R.id.fab_add_mentor);

        navButtonIdeias = findViewById(R.id.nav_button_ideias);
        navButtonMentores = findViewById(R.id.nav_button_mentores);
        navButtonInvestidores = findViewById(R.id.nav_button_investidores);
        navButtonPerfil  = findViewById(R.id.nav_button_perfil);

        navIconIdeias = findViewById(R.id.nav_icon_ideias);
        navIconMentores = findViewById(R.id.nav_icon_mentores);
        navIconInvestidores = findViewById(R.id.nav_icon_investidores);
        navIconPerfil = findViewById(R.id.nav_icon_perfil);

        navTextIdeias = findViewById(R.id.nav_text_ideias);
        navTextMentores = findViewById(R.id.nav_text_mentores);
        navTextInvestidores = findViewById(R.id.nav_text_investidores);
        navTextPerfil = findViewById(R.id.nav_text_perfil);

        verificarSeUsuarioEhMentor();
        setupButtonClickListeners();

        // FABs
        fabAddIdea.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CanvasIdeiaActivity.class);
            Ideia novaIdeia = new Ideia();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                novaIdeia.setOwnerId(user.getUid());
                novaIdeia.setAutorNome(user.getDisplayName());
            }
            intent.putExtra("ideia", novaIdeia);
            startActivity(intent);
        });

        fabAddMentor.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CanvasMentorActivity.class);
            Mentor novoMentor = new Mentor();
            if (currentUser != null) {
                novoMentor.setId(currentUser.getUid());
                novoMentor.setNome(currentUser.getDisplayName());
            }
            intent.putExtra("mentor", novoMentor);
            startActivity(intent);
        });

        // Tela inicial: IdeiasHostFragment (tabs: Publicadas / Meus rascunhos)
        if (savedInstanceState == null) {
            loadFragment(new IdeiasHostFragment());
            updateButtonState(navButtonIdeias);
        }
    }

    private void verificarSeUsuarioEhMentor() {
        if (currentUser == null) {
            isMentor = false;
            atualizarVisibilidadeFabMentor();
            return;
        }
        String uid = currentUser.getUid();
        FirebaseFirestore.getInstance()
                .collection("mentores")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isMentor = documentSnapshot.exists();
                    atualizarVisibilidadeFabMentor();
                })
                .addOnFailureListener(e -> {
                    isMentor = false;
                    atualizarVisibilidadeFabMentor();
                });
    }

    private void atualizarVisibilidadeFabMentor() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // Exibe o FAB de "Adicionar Mentor" somente quando estamos na aba de mentores
        // e o usuário AINDA não tem cadastro de mentor (isMentor == false).
        if (fragment instanceof MentoresFragment && !isMentor) {
            fabAddMentor.setVisibility(View.VISIBLE);
        } else {
            fabAddMentor.setVisibility(View.GONE);
        }
    }

    private void setupButtonClickListeners() {
        navButtonIdeias.setOnClickListener(v -> {
            loadFragment(new IdeiasHostFragment()); // host com tabs
            updateButtonState(v);
            // Na aba de ideias, mostrar FAB de ideias e esconder o de mentor
            fabAddIdea.setVisibility(View.VISIBLE);
            fabAddMentor.setVisibility(View.GONE);
        });

        navButtonMentores.setOnClickListener(v -> {
            loadFragment(new MentoresFragment());
            updateButtonState(v);
            // Na aba de mentores, esconder FAB de ideias e decidir FAB de mentor
            fabAddIdea.setVisibility(View.GONE);
            verificarSeUsuarioEhMentor(); // decide visibilidade do fabAddMentor
        });

        navButtonInvestidores.setOnClickListener(v -> {
            loadFragment(new InvestidoresFragment());
            updateButtonState(v);
            // Esconder ambos os FABs na tela de investidores
            fabAddIdea.setVisibility(View.GONE);
            fabAddMentor.setVisibility(View.GONE);
        });

        navButtonPerfil.setOnClickListener(v -> {
            loadFragment(new PerfilFragment());
            updateButtonState(v);
            // Na aba de perfil, esconder ambos
            fabAddIdea.setVisibility(View.GONE);
            fabAddMentor.setVisibility(View.GONE);
        });
    }

    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        fabAddMentor.postDelayed(this::atualizarVisibilidadeFabMentor, 100);
    }

    private void updateButtonState(View selectedButton) {
        navButtonIdeias.setSelected(false);
        navButtonMentores.setSelected(false);
        navButtonInvestidores.setSelected(false);
        navButtonPerfil.setSelected(false);

        navTextIdeias.setVisibility(View.GONE);
        navTextMentores.setVisibility(View.GONE);
        navTextInvestidores.setVisibility(View.GONE);
        navTextPerfil.setVisibility(View.GONE);

        selectedButton.setSelected(true);

        if (selectedButton.getId() == R.id.nav_button_ideias) {
            navTextIdeias.setVisibility(View.VISIBLE);
        } else if (selectedButton.getId() == R.id.nav_button_mentores) {
            navTextMentores.setVisibility(View.VISIBLE);
        } else if (selectedButton.getId() == R.id.nav_button_investidores){
            navTextInvestidores.setVisibility(View.VISIBLE);
        } else if (selectedButton.getId() == R.id.nav_button_perfil) {
            navTextPerfil.setVisibility(View.VISIBLE);
        }

        tintNavIcons();
    }

    private void tintNavIcons() {
        int selected = ContextCompat.getColor(this, R.color.primary_color);
        int normal   = ContextCompat.getColor(this, R.color.strokeSoft);

        boolean ideiasSel   = navButtonIdeias.isSelected();
        boolean mentoresSel = navButtonMentores.isSelected();
        boolean investidoresSel = navButtonInvestidores.isSelected();
        boolean perfilSel   = navButtonPerfil.isSelected();

        navIconIdeias.setImageTintList(ColorStateList.valueOf(ideiasSel ? selected : normal));
        navIconMentores.setImageTintList(ColorStateList.valueOf(mentoresSel ? selected : normal));
        navIconInvestidores.setImageTintList(ColorStateList.valueOf(investidoresSel ? selected : normal));
        navIconPerfil.setImageTintList(ColorStateList.valueOf(perfilSel ? selected : normal));
    }
}