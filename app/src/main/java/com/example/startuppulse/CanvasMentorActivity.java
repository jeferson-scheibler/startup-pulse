package com.example.startuppulse;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.databinding.ActivityCanvasMentorBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Callback;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Activity para o utilizador se registar ou editar o seu perfil de Mentor.
 */
public class CanvasMentorActivity extends AppCompatActivity {

    private ActivityCanvasMentorBinding binding;
    private FirestoreHelper firestoreHelper;
    private IBGEService ibgeService;

    private List<Estado> listaDeEstados = new ArrayList<>();
    private Estado estadoSelecionado;

    // Lista simples de áreas para seleção (pode mover para strings.xml depois)
    private static final List<String> AREAS_PREDEFINIDAS = Arrays.asList(
            "Marketing", "Vendas", "Finanças", "Estratégia", "Produto",
            "Tecnologia", "Operações", "Jurídico", "RH", "UX/UI"
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCanvasMentorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestoreHelper = new FirestoreHelper();
        setupRetrofit();
        // Preenche dados iniciais
        preencherDadosIniciais();

        // Monta chips de áreas
        montarChipsAreas();
        buscarEstados();
        setupDropdownListeners();

        // Listeners
        binding.buttonSalvar.setOnClickListener(v -> validarEProcessarMentor());
        binding.buttonCancelar.setOnClickListener(v -> finish());
    }
    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://servicodados.ibge.gov.br/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ibgeService = retrofit.create(IBGEService.class);
    }

    private void buscarEstados() {
        ibgeService.getEstados().enqueue(new Callback<List<Estado>>() {
            @Override
            public void onResponse(Call<List<Estado>> call, Response<List<Estado>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaDeEstados = response.body();
                    ArrayAdapter<Estado> adapter = new ArrayAdapter<>(CanvasMentorActivity.this, android.R.layout.simple_dropdown_item_1line, listaDeEstados);
                    binding.autoCompleteEstado.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Estado>> call, Throwable t) { /* Lidar com erro de rede */ }
        });
    }

    private void setupDropdownListeners() {
        binding.autoCompleteEstado.setOnItemClickListener((parent, view, position, id) -> {
            estadoSelecionado = (Estado) parent.getItemAtPosition(position);
            binding.autoCompleteCidade.setText(""); // Limpa a cidade anterior
            buscarCidades(estadoSelecionado.getSigla());
        });
    }

    private void buscarCidades(String siglaEstado) {
        binding.layoutCidade.setEnabled(true);
        ibgeService.getCidadesPorEstado(siglaEstado).enqueue(new Callback<List<Cidade>>() {
            @Override
            public void onResponse(Call<List<Cidade>> call, Response<List<Cidade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ArrayAdapter<Cidade> adapter = new ArrayAdapter<>(CanvasMentorActivity.this, android.R.layout.simple_dropdown_item_1line, response.body());
                    binding.autoCompleteCidade.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Cidade>> call, Throwable t) { /* Lidar com erro de rede */ }
        });
    }

    private void preencherDadosIniciais() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            binding.editTextNome.setText(user.getDisplayName());
            binding.editTextNome.setEnabled(false); // nome fixo (vem da conta)
        }
        // Se futuramente editar mentor, pré-carregue aqui (intent / Firestore)
    }

    private void montarChipsAreas() {
        binding.chipGroupAreasSelect.removeAllViews();
        for (String area : AREAS_PREDEFINIDAS) {
            com.google.android.material.chip.Chip chip =
                    new com.google.android.material.chip.Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setText(area);
            chip.setCheckable(true);
            chip.setChecked(false);
            chip.setChipIconResource(R.drawable.ic_tag);
            chip.setChipIconTintResource(androidx.appcompat.R.color.material_grey_600);
            chip.setIconStartPadding(6f);
            chip.setTextStartPadding(4f);
            binding.chipGroupAreasSelect.addView(chip);
        }
    }

    private List<String> coletarAreasSelecionadas() {
        List<String> selecionadas = new ArrayList<>();
        for (int i = 0; i < binding.chipGroupAreasSelect.getChildCount(); i++) {
            if (binding.chipGroupAreasSelect.getChildAt(i) instanceof com.google.android.material.chip.Chip) {
                com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) binding.chipGroupAreasSelect.getChildAt(i);
                if (c.isChecked()) selecionadas.add(String.valueOf(c.getText()));
            }
        }
        return selecionadas;
    }

    private void validarEProcessarMentor() {
        String nome = binding.editTextNome.getText().toString().trim();
        String profissao = binding.editTextProfissao.getText().toString().trim();
        String cidade = binding.autoCompleteCidade.getText().toString().trim();
        String estado = (estadoSelecionado != null) ? estadoSelecionado.getNome() : "";

        if (nome.isEmpty() || profissao.isEmpty() || cidade.isEmpty() || estado.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        bloquearSalvar(true, "A verificar...");

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

                    handler.post(() -> salvarMentor(nome, profissao, cidade, estado, latitude, longitude));
                } else {
                    handler.post(() -> {
                        Toast.makeText(this, "Localização não encontrada. Verifique a cidade e o estado.", Toast.LENGTH_LONG).show();
                        bloquearSalvar(false, "Salvar Cadastro");
                    });
                }
            } catch (IOException e) {
                handler.post(() -> {
                    Toast.makeText(this, "Erro de rede. Tente novamente.", Toast.LENGTH_LONG).show();
                    bloquearSalvar(false, "Salvar Cadastro");
                });
            } finally {
                executor.shutdown();
            }
        });
    }

    private void bloquearSalvar(boolean bloqueado, String texto) {
        binding.buttonSalvar.setEnabled(!bloqueado);
        binding.buttonSalvar.setText(texto);
    }

    private void salvarMentor(String nome, String profissao, String cidade, String estado, double latitude, double longitude) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Erro: Utilizador não autenticado.", Toast.LENGTH_SHORT).show();
            bloquearSalvar(false, "Salvar Cadastro");
            return;
        }

        Mentor mentor = new Mentor();
        mentor.setId(currentUser.getUid()); // boa prática: manter id consistente
        mentor.setNome(nome);
        mentor.setProfissao(profissao);
        mentor.setCidade(cidade);
        mentor.setEstado(estado);
        mentor.setLatitude(latitude);
        mentor.setLongitude(longitude);
        if (currentUser.getPhotoUrl() != null) {
            mentor.setImagem(currentUser.getPhotoUrl().toString());
        }
        mentor.setVerificado(false); // badge controlado por admin/moderação
        mentor.setAreas(coletarAreasSelecionadas());

        String mentorId = currentUser.getUid();

        // NOVO: usa Callback<Result<String>>
        firestoreHelper.addMentorWithId(mentorId, mentor, r -> {
            if (r.isOk()) {
                Toast.makeText(this, "Perfil de mentor salvo com sucesso!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Erro ao salvar o perfil: " + r.error.getMessage(), Toast.LENGTH_LONG).show();
                bloquearSalvar(false, "Salvar Cadastro");
            }
        });
    }
}