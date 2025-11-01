package com.example.startuppulse.ui.mentor;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Cidade;
import com.example.startuppulse.data.models.Estado;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IMentorRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.util.Event;
import com.example.startuppulse.util.GeocodeUtils;
import com.example.startuppulse.util.IBGEService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class CanvasMentorViewModel extends ViewModel {

    private final IUserRepository userRepository;
    private final IMentorRepository mentorRepository;
    private final IAuthRepository authRepository;
    private final IBGEService ibgeService;

    // LiveData para o usuário atual
    private final MutableLiveData<User> _user = new MutableLiveData<>();
    public LiveData<User> user = _user;

    // LiveData para Estados (API IBGE)
    private final MutableLiveData<Result<List<Estado>>> _estados = new MutableLiveData<>();
    public LiveData<Result<List<Estado>>> estados = _estados;

    // LiveData para Cidades (API IBGE)
    private final MutableLiveData<Result<List<Cidade>>> _cidades = new MutableLiveData<>();
    public LiveData<Result<List<Cidade>>> cidades = _cidades;

    // LiveData para o resultado do salvamento
    private final MutableLiveData<Result<Void>> _saveResult = new MutableLiveData<>();
    public LiveData<Result<Void>> saveResult = _saveResult;

    // Evento de navegação
    private final MutableLiveData<Event<Boolean>> _navigateToProfile = new MutableLiveData<>();
    public LiveData<Event<Boolean>> navigateToProfile = _navigateToProfile;

    private String currentUid;

    @Inject
    public CanvasMentorViewModel(IUserRepository userRepository, IMentorRepository mentorRepository, IAuthRepository authRepository, IBGEService ibgeService) {
        this.userRepository = userRepository;
        this.mentorRepository = mentorRepository;
        this.authRepository = authRepository;
        this.ibgeService = ibgeService;
        loadInitialData();
    }

    /**
     * Carrega os dados iniciais: o usuário logado e a lista de estados.
     */
    public void loadInitialData() {
        currentUid = authRepository.getCurrentUserId();
        if (currentUid != null) {
            userRepository.getUserProfile(currentUid, result -> {
                if (result instanceof Result.Success) {
                    _user.postValue(((Result.Success<User>) result).data);
                }
                // Opcional: tratar falha no carregamento do usuário
            });
        }
        loadEstados();
    }

    /**
     * Busca a lista de estados na API do IBGE.
     */
    private void loadEstados() {
        _estados.setValue(new Result.Loading<>());
        ibgeService.getEstados().enqueue(new Callback<List<Estado>>() {
            @Override
            public void onResponse(Call<List<Estado>> call, Response<List<Estado>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _estados.setValue(new Result.Success<>(response.body()));
                } else {
                    _estados.setValue(new Result.Error<>(new Exception("Falha ao buscar estados")));
                }
            }

            @Override
            public void onFailure(Call<List<Estado>> call, Throwable t) {
                _estados.setValue(new Result.Error<>(new Exception(t)));
            }
        });
    }

    /**
     * Busca as cidades de um estado específico (pela sigla/UF).
     */
    public void onEstadoSelected(String siglaUF) {
        _cidades.setValue(new Result.Loading<>());
        ibgeService.getCidadesPorEstado(siglaUF).enqueue(new Callback<List<Cidade>>() {
            @Override
            public void onResponse(Call<List<Cidade>> call, Response<List<Cidade>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cidades.setValue(new Result.Success<>(response.body()));
                } else {
                    _cidades.setValue(new Result.Error<>(new Exception("Falha ao buscar cidades")));
                }
            }

            @Override
            public void onFailure(Call<List<Cidade>> call, Throwable t) {
                _cidades.setValue(new Result.Error<>(new Exception(t)));
            }
        });
    }

    /**
     * Orquestra o salvamento:
     * 1. Atualiza o documento do Usuário (isMentor=true, profissao, areas)
     * 2. Cria o documento do Mentor (estado, cidade)
     */
    public void saveMentorProfile(String profissao, List<String> areas, String estado, String cidade, String bio) {
        _saveResult.setValue(new Result.Loading<>());

        User currentUser = _user.getValue();
        if (currentUser == null) {
            _saveResult.setValue(new Result.Error<>(new Exception("Usuário não carregado.")));
            return;
        }

        String currentUid = currentUser.getId();
        if (currentUid == null) {
            _saveResult.setValue(new Result.Error<>(new Exception("Usuário sem ID válido.")));
            return;
        }

        // Atualiza atributos do usuário
        currentUser.setMentor(true);
        currentUser.setProfissao(profissao);
        currentUser.setAreasDeInteresse(areas);

        // 🔹 Executa todo o processo em background
        new Thread(() -> {
            try {
                // 1️⃣ Busca coordenadas antes de salvar
                double[] coords = GeocodeUtils.obterCoordenadasPorCidade(cidade, estado);
                double latitude = 0.0;
                double longitude = 0.0;

                if (coords != null) {
                    latitude = coords[0];
                    longitude = coords[1];
                    Log.d("MentorProfile", "Coordenadas obtidas: " + latitude + ", " + longitude);
                } else {
                    Log.w("MentorProfile", "Não foi possível obter coordenadas para " + cidade + ", " + estado);
                }

                double finalLatitude = latitude;
                double finalLongitude = longitude;

                // 2️⃣ Atualiza o User no Firestore
                userRepository.updateUser(currentUid, currentUser, userResult -> {
                    if (userResult instanceof Result.Success) {

                        // 3️⃣ Cria o Mentor após atualizar o usuário
                        Mentor mentor = new Mentor();
                        mentor.setState(estado);
                        mentor.setCity(cidade);
                        mentor.setBio(bio);
                        mentor.setLatitude(finalLatitude);
                        mentor.setLongitude(finalLongitude);
                        mentor.setActivePublic(true); // opcional, mas importante pro match

                        mentorRepository.saveMentorProfile(mentor, mentorResult -> {
                            if (mentorResult instanceof Result.Success) {
                                _saveResult.postValue(new Result.Success<>(null));
                                _navigateToProfile.postValue(new Event<>(true));
                            } else if (mentorResult instanceof Result.Error) {
                                Exception error = ((Result.Error<String>) mentorResult).error;
                                _saveResult.postValue(new Result.Error<>(error));
                            }
                        });
                    } else if (userResult instanceof Result.Error) {
                        Exception error = ((Result.Error<Void>) userResult).error;
                        _saveResult.postValue(new Result.Error<>(error));
                    }
                });

            } catch (Exception e) {
                Log.e("MentorProfile", "Erro inesperado ao salvar mentor: " + e.getMessage(), e);
                _saveResult.postValue(new Result.Error<>(e));
            }
        }).start();
    }

}