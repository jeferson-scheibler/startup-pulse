package com.example.startuppulse;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.lifecycle.Observer;

public class MainActivity extends AppCompatActivity {

    LinearLayout navButtonIdeias, navButtonMentores, navButtonPerfil;
    ImageView navIconIdeias, navIconMentores, navIconPerfil;
    TextView navTextIdeias, navTextMentores, navTextPerfil;
    FloatingActionButton fabAddIdea, fabAddMentor;
    private boolean isMentor = false;
    private FirebaseUser currentUser;
    private TextView offlineIndicatorBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        offlineIndicatorBar = findViewById(R.id.offline_indicator_bar);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        NetworkManager networkManager = NetworkManager.getInstance(getApplicationContext());
        networkManager.isNetworkAvailable().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isAvailable) {
                offlineIndicatorBar.setVisibility(isAvailable ? View.GONE : View.VISIBLE);
            }
        });

        // Referências para os componentes
        fabAddIdea = findViewById(R.id.fab_add_idea);
        fabAddMentor = findViewById(R.id.fab_add_mentor);
        navButtonIdeias = findViewById(R.id.nav_button_ideias);
        navButtonMentores = findViewById(R.id.nav_button_mentores);
        navButtonPerfil = findViewById(R.id.nav_button_perfil);

        navIconIdeias = findViewById(R.id.nav_icon_ideias);
        navIconMentores = findViewById(R.id.nav_icon_mentores);
        navIconPerfil = findViewById(R.id.nav_icon_perfil);

        navTextIdeias = findViewById(R.id.nav_text_ideias);
        navTextMentores = findViewById(R.id.nav_text_mentores);
        navTextPerfil = findViewById(R.id.nav_text_perfil);

        verificarSeUsuarioEhMentor();
        // Configura os cliques dos botões de navegação
        setupButtonClickListeners();

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

        // Define a tela inicial padrão
        if (savedInstanceState == null) {
            navigateTo(new IdeiasFragment());
            updateButtonState(navButtonIdeias);
        }
    }

    private void verificarSeUsuarioEhMentor() {
        if (currentUser == null) {
            isMentor = false;
            fabAddMentor.setVisibility(View.GONE);
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
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (fragment instanceof MentoresFragment && !isMentor) {
            fabAddMentor.setVisibility(View.VISIBLE);
        } else {
            fabAddMentor.setVisibility(View.GONE);
        }
    }

    private void setupButtonClickListeners() {
        navButtonIdeias.setOnClickListener(v -> {
            navigateTo(new IdeiasFragment());
            updateButtonState(v);
        });

        navButtonMentores.setOnClickListener(v -> {
            navigateTo(new MentoresFragment());
            updateButtonState(v);
        });

        navButtonPerfil.setOnClickListener(v -> {
            navigateTo(new PerfilFragment());
            updateButtonState(v);
        });
    }

    private void navigateTo(Fragment fragment) {
        if (fragment instanceof MentoresFragment) {
            fabAddIdea.setVisibility(View.GONE);
            verificarSeUsuarioEhMentor();
        } else {
            fabAddIdea.setVisibility(View.VISIBLE);
            fabAddMentor.setVisibility(View.GONE);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        // Atualiza visibilidade do botão mentor com base na flag isMentor
        fabAddMentor.postDelayed(this::atualizarVisibilidadeFabMentor, 100);
    }

    private void updateButtonState(View selectedButton) {
        // Primeiro, diz a todos os botões que eles NÃO estão selecionados
        navButtonIdeias.setSelected(false);
        navButtonMentores.setSelected(false);
        navButtonPerfil.setSelected(false);

        // Esconde todos os textos
        navTextIdeias.setVisibility(View.GONE);
        navTextMentores.setVisibility(View.GONE);
        navTextPerfil.setVisibility(View.GONE);

        // Agora, diz apenas ao botão clicado que ele ESTÁ selecionado
        selectedButton.setSelected(true);

        // E mostra apenas o texto do botão selecionado
        if (selectedButton.getId() == R.id.nav_button_ideias) {
            navTextIdeias.setVisibility(View.VISIBLE);
        } else if (selectedButton.getId() == R.id.nav_button_mentores) {
            navTextMentores.setVisibility(View.VISIBLE);
        } else if (selectedButton.getId() == R.id.nav_button_perfil) {
            navTextPerfil.setVisibility(View.VISIBLE);
        }
    }

    private void resetButtonState(ImageView icon, TextView text, int color) {
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setVisibility(View.GONE);
        text.setTextColor(color);
    }

    private void setButtonSelected(ImageView icon, TextView text, int color) {
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setVisibility(View.VISIBLE);
        text.setTextColor(color);
    }
}
