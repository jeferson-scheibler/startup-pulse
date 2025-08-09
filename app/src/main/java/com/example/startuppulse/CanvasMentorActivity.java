package com.example.startuppulse;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity para o utilizador se registar ou editar o seu perfil de Mentor.
 */
public class CanvasMentorActivity extends AppCompatActivity {

    private EditText editTextNome, editTextProfissao, editTextCidade, editTextEstado;
    private Button buttonSalvar, buttonCancelar;

    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas_mentor);

        // 1. Inicializar componentes
        firestoreHelper = new FirestoreHelper();

        // 2. Mapear as Views do layout
        editTextNome = findViewById(R.id.edit_text_nome);
        editTextProfissao = findViewById(R.id.edit_text_profissao);
        editTextCidade = findViewById(R.id.edit_text_cidade);
        editTextEstado = findViewById(R.id.edit_text_estado);
        buttonSalvar = findViewById(R.id.button_salvar);
        buttonCancelar = findViewById(R.id.button_cancelar);

        // 3. Preenche os dados iniciais
        preencherDadosIniciais();

        // 4. Configura os listeners dos botões
        buttonSalvar.setOnClickListener(v -> validarEProcessarMentor());
        buttonCancelar.setOnClickListener(v -> finish());
    }

    private void preencherDadosIniciais() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            editTextNome.setText(user.getDisplayName());
            editTextNome.setEnabled(false); // O nome não pode ser alterado
        }

        // No futuro, se você implementar a edição, pode carregar os dados do mentor aqui
        // Mentor mentor = (Mentor) getIntent().getSerializableExtra("mentor");
        // if (mentor != null) { ... }
    }

    private void validarEProcessarMentor() {
        String nome = editTextNome.getText().toString().trim();
        String profissao = editTextProfissao.getText().toString().trim();
        String cidade = editTextCidade.getText().toString().trim();
        String estado = editTextEstado.getText().toString().trim();

        if (nome.isEmpty() || profissao.isEmpty() || cidade.isEmpty() || estado.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSalvar.setEnabled(false);
        buttonSalvar.setText("A verificar...");

        // A geocodificação é uma operação de rede e deve ser feita numa thread de fundo
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(cidade + ", " + estado, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double latitude = address.getLatitude();
                    double longitude = address.getLongitude();

                    // Sucesso! Volta para a thread principal para salvar no Firestore
                    handler.post(() -> salvarMentor(nome, profissao, cidade, estado, latitude, longitude));
                } else {
                    handler.post(() -> {
                        Toast.makeText(this, "Localização não encontrada. Verifique a cidade e o estado.", Toast.LENGTH_LONG).show();
                        buttonSalvar.setEnabled(true);
                        buttonSalvar.setText("Salvar Cadastro");
                    });
                }
            } catch (IOException e) {
                handler.post(() -> {
                    Toast.makeText(this, "Erro de rede. Tente novamente.", Toast.LENGTH_LONG).show();
                    buttonSalvar.setEnabled(true);
                    buttonSalvar.setText("Salvar Cadastro");
                });
            }
        });
    }

    private void salvarMentor(String nome, String profissao, String cidade, String estado, double latitude, double longitude) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Erro: Utilizador não autenticado.", Toast.LENGTH_SHORT).show();
            buttonSalvar.setEnabled(true);
            buttonSalvar.setText("Salvar Cadastro");
            return;
        }

        Mentor mentor = new Mentor();
        mentor.setNome(nome);
        mentor.setProfissao(profissao);
        mentor.setCidade(cidade);
        mentor.setEstado(estado);
        mentor.setLatitude(latitude);
        mentor.setLongitude(longitude);
        if (currentUser.getPhotoUrl() != null) {
            mentor.setImagem(currentUser.getPhotoUrl().toString());
        }

        // Usa o ID do utilizador como o ID do documento do mentor
        String mentorId = currentUser.getUid();

        firestoreHelper.addMentorWithId(mentorId, mentor, new FirestoreHelper.AddMentorListener() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(CanvasMentorActivity.this, "Perfil de mentor salvo com sucesso!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CanvasMentorActivity.this, "Erro ao salvar o perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                buttonSalvar.setEnabled(true);
                buttonSalvar.setText("Salvar Cadastro");
            }
        });
    }
}