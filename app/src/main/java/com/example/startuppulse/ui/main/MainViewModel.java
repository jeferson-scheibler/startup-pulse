package com.example.startuppulse.ui.main;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.data.repositories.IAuthRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private static final String TAG = "MainViewModel";

    public enum AuthenticationState {
        AUTHENTICATED,
        UNAUTHENTICATED
    }

    private final MutableLiveData<AuthenticationState> _authenticationState = new MutableLiveData<>();
    public final LiveData<AuthenticationState> authenticationState = _authenticationState;

    private final FirebaseAuth firebaseAuth; // Injetar FirebaseAuth
    private final FirebaseFirestore firestore; // Injetar Firestore
    private final IAuthRepository authRepository; // Injetar IAuthRepository

    private final FirebaseAuth.AuthStateListener authStateListener;
    private boolean acessoDiarioRegistradoNestaSessao = false;

    @Inject // Adicionar anotação Inject ao construtor
    public MainViewModel(FirebaseAuth firebaseAuth, FirebaseFirestore firestore, IAuthRepository authRepository) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.authRepository = authRepository;

        Log.d(TAG, "MainViewModel Initialized");

        authStateListener = auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                Log.d(TAG, "AuthStateListener: User Authenticated: " + user.getUid());
                _authenticationState.setValue(AuthenticationState.AUTHENTICATED);
                // Tenta registrar o acesso diário APENAS se ainda não foi feito nesta sessão
                if (!acessoDiarioRegistradoNestaSessao) {
                    registrarAcessoDiario(user.getUid());
                }
            } else {
                Log.d(TAG, "AuthStateListener: User Unauthenticated");
                _authenticationState.setValue(AuthenticationState.UNAUTHENTICATED);
                acessoDiarioRegistradoNestaSessao = false; // Reseta a flag ao deslogar
            }
        };

        // Começa a ouvir as mudanças assim que o ViewModel é criado
        this.firebaseAuth.addAuthStateListener(authStateListener);
    }

    private void registrarAcessoDiario(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.d(TAG, "registrarAcessoDiario: userId inválido.");
            return;
        }

        DocumentReference userDocRef = firestore.collection("usuarios").document(userId);
        Log.d(TAG, "registrarAcessoDiario: Verificando acesso para userId: " + userId);


        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.w(TAG, "registrarAcessoDiario: Documento do usuário não encontrado para ID: " + userId);
                return;
            }

            Timestamp ultimoAcessoTs = documentSnapshot.getTimestamp("ultimoAcesso");
            Date agora = new Date();
            boolean registrarHoje = true;

            if (ultimoAcessoTs != null) {
                Date ultimoAcessoDate = ultimoAcessoTs.toDate();
                if (saoMesmoDia(ultimoAcessoDate, agora)) {
                    registrarHoje = false;
                    Log.d(TAG, "registrarAcessoDiario: Acesso de hoje já registrado.");
                }
            } else {
                Log.d(TAG, "registrarAcessoDiario: Primeiro acesso registrado.");
            }

            if (registrarHoje) {
                Log.d(TAG, "registrarAcessoDiario: Necessário registrar acesso de hoje.");
                Map<String, Object> updates = new HashMap<>();
                updates.put("ultimoAcesso", FieldValue.serverTimestamp());
                updates.put("diasAcessoTotal", FieldValue.increment(1));

                userDocRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Log.i(TAG, "registrarAcessoDiario: Acesso de hoje registrado com SUCESSO.");
                            acessoDiarioRegistradoNestaSessao = true; // Marca como registrado nesta sessão
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "registrarAcessoDiario: Falha ao registrar acesso.", e));
            } else {
                // Se já registrou hoje, marca como feito nesta sessão também para não tentar de novo
                acessoDiarioRegistradoNestaSessao = true;
            }

        }).addOnFailureListener(e -> {
            Log.e(TAG, "registrarAcessoDiario: Falha ao buscar documento do usuário.", e);
        });
    }

    // Função auxiliar para comparar se duas datas são no mesmo dia
    private boolean saoMesmoDia(Date data1, Date data2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(data1);
        cal2.setTime(data2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: Removing AuthStateListener");
        // Para de ouvir para evitar memory leaks
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}