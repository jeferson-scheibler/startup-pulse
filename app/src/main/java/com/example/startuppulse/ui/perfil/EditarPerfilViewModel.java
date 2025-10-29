package com.example.startuppulse.ui.perfil;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.Cidade;
import com.example.startuppulse.data.Estado;
import com.example.startuppulse.data.ResultCallback;
import com.example.startuppulse.data.models.Mentor;
import com.example.startuppulse.data.models.User;
import com.example.startuppulse.data.repositories.IAuthRepository;
import com.example.startuppulse.data.repositories.IMentorRepository;
import com.example.startuppulse.data.repositories.IStorageRepository;
import com.example.startuppulse.data.repositories.IUserRepository;
import com.example.startuppulse.util.IBGEService;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class EditarPerfilViewModel extends ViewModel {

    private final IAuthRepository authRepository;
    private final IUserRepository userRepository;
    private final IStorageRepository storageRepository;
    private final IMentorRepository mentorRepository;
    private final IBGEService ibgeService; // ADICIONADO

    // LiveDatas para os perfis
    private final MutableLiveData<Result<User>> _userProfile = new MutableLiveData<>();
    public final LiveData<Result<User>> userProfile = _userProfile;

    // ADICIONADO: LiveData para o perfil de mentor
    private final MutableLiveData<Result<Mentor>> _mentorProfile = new MutableLiveData<>();
    public final LiveData<Result<Mentor>> mentorProfile = _mentorProfile;

    // ADICIONADO: LiveDatas para API IBGE
    private final MutableLiveData<Result<List<Estado>>> _estados = new MutableLiveData<>();
    public LiveData<Result<List<Estado>>> estados = _estados;
    private final MutableLiveData<Result<List<Cidade>>> _cidades = new MutableLiveData<>();
    public LiveData<Result<List<Cidade>>> cidades = _cidades;

    private final MutableLiveData<Result<Void>> _updateResult = new MutableLiveData<>();
    public final LiveData<Result<Void>> updateResult = _updateResult;

    private String currentUserId;
    private User currentUserData; // Cache dos dados do usuário

    @Inject
    public EditarPerfilViewModel(IAuthRepository authRepository, IUserRepository userRepository, IStorageRepository storageRepository, IMentorRepository mentorRepository, IBGEService ibgeService) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.storageRepository = storageRepository;
        this.mentorRepository = mentorRepository;
        this.ibgeService = ibgeService; // ADICIONADO
        loadAllProfiles();
    }

    private void loadAllProfiles() {
        currentUserId = authRepository.getCurrentUserId();
        if (currentUserId != null) {
            _userProfile.setValue(new Result.Loading<>());
            userRepository.getUserProfile(currentUserId, userResult -> {
                _userProfile.postValue(userResult);

                if (userResult instanceof Result.Success) {
                    this.currentUserData = ((Result.Success<User>) userResult).data;
                    // Se o usuário for mentor, carrega o perfil de mentor
                    if (this.currentUserData.isMentor()) {
                        loadMentorProfile(currentUserId);
                        // Carrega os estados para os spinners
                        loadEstados();
                    }
                }
            });
        } else {
            _userProfile.setValue(new Result.Error<>(new Exception("Usuário não autenticado.")));
        }
    }

    private void loadMentorProfile(String mentorId) {
        _mentorProfile.setValue(new Result.Loading<>());
        mentorRepository.getMentorById(mentorId, _mentorProfile::postValue);
    }

    // --- LÓGICA DO IBGE (REUTILIZADA DA ETAPA 2) ---

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

    // --- LÓGICA DE SALVAMENTO UNIFICADA (MUDANÇA PRINCIPAL) ---

    /**
     * Salva o perfil unificado.
     * Dados do usuário são salvos em /users/{uid}
     * Dados do mentor (se for mentor) são salvos em /mentores/{uid}
     */
    public void saveUnifiedProfile(String newName, String userBio, Uri newPhotoUri,
                                   String newProfession, String newLinkedinUrl, List<String> newAreas,
                                   String mentorBio, String estado, String cidade) {

        if (currentUserId == null || currentUserData == null) {
            _updateResult.setValue(new Result.Error<>(new Exception("Usuário não encontrado.")));
            return;
        }

        _updateResult.setValue(new Result.Loading<>());

        if (newPhotoUri != null) {
            // 1. Se há nova foto, faz o upload primeiro
            String fileName = currentUserId + ".jpg";
            String folderPath = "profile_images/" + currentUserId;
            storageRepository.uploadImage(newPhotoUri, folderPath, fileName, result -> {
                if (result instanceof Result.Success) {
                    String photoUrl = ((Result.Success<String>) result).data;
                    // 2. Continua para salvar no banco com a nova URL da foto
                    performDatabaseUpdates(newName, userBio, photoUrl, newProfession, newLinkedinUrl, newAreas,
                            mentorBio, estado, cidade);
                } else {
                    _updateResult.setValue(new Result.Error<>(((Result.Error<String>) result).error));
                }
            });
        } else {
            // 2. Se não há nova foto, salva direto no banco com a foto antiga (null)
            performDatabaseUpdates(newName, userBio, null, newProfession, newLinkedinUrl, newAreas,
                    mentorBio, estado, cidade);
        }
    }

    /**
     * Atualiza os documentos no Firestore.
     * @param newPhotoUrl null se a foto não foi alterada, ou a nova URL se foi.
     */
    private void performDatabaseUpdates(String name, String userBio, String newPhotoUrl,
                                        String profession, String linkedinUrl, List<String> areas,
                                        String mentorBio, String estado, String cidade) {

        // --- ATUALIZAÇÃO DO DOCUMENTO USER ---
        // Atualiza o objeto User local
        currentUserData.setNome(name);
        currentUserData.setBio(userBio);
        currentUserData.setProfissao(profession);
        currentUserData.setLinkedinUrl(linkedinUrl);
        currentUserData.setAreasDeInteresse(areas);
        if (newPhotoUrl != null) {
            currentUserData.setFotoUrl(newPhotoUrl);
        }

        // Salva o objeto User inteiro
        // userResult é Result<Void>
        userRepository.updateUser(currentUserId, currentUserData, userResult -> {
            if (userResult instanceof Result.Success) {

                // Se não for mentor, o trabalho acaba aqui
                if (!currentUserData.isMentor()) {
                    _updateResult.postValue(new Result.Success<>(null));
                    return;
                }

                // --- ATUALIZAÇÃO DO DOCUMENTO MENTOR ---
                // Se for mentor, continua para atualizar o mentor
                Result<Mentor> mentorResult = _mentorProfile.getValue();
                Mentor mentorData;

                if (mentorResult instanceof Result.Success) {
                    mentorData = ((Result.Success<Mentor>) mentorResult).data;
                } else {
                    mentorData = new Mentor();
                }

                mentorData.setBio(mentorBio);
                mentorData.setState(estado);
                mentorData.setCity(cidade);

                // mentorSaveResult é Result<String>
                mentorRepository.saveMentorProfile(mentorData, mentorSaveResult -> {

                    // --- INÍCIO DA CORREÇÃO ---
                    if (mentorSaveResult instanceof Result.Success) {
                        // Se o mentor salvar com sucesso (Result<String>),
                        // postamos um sucesso genérico (Result<Void>) para a UI.
                        _updateResult.postValue(new Result.Success<>(null));

                    } else if (mentorSaveResult instanceof Result.Error) {
                        // Se o mentor falhar (Result.Error<String>),
                        // extraímos o erro e o repassamos como (Result.Error<Void>).
                        Exception error = ((Result.Error<String>) mentorSaveResult).error;
                        _updateResult.postValue(new Result.Error<>(error));
                    }
                    // --- FIM DA CORREÇÃO ---

                });

            } else if (userResult instanceof Result.Error) {
                // Se o Passo 1 (Salvar User) falhar, relate o erro e pare.
                _updateResult.postValue(userResult);
            }
        });
    }
}