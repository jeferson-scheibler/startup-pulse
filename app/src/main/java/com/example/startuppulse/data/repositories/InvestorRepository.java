package com.example.startuppulse.data.repositories;

import androidx.annotation.Nullable;
import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.common.ResultCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório para todas as operações de dados relacionadas a Investidores.
 * Gerenciado pelo Hilt como um Singleton para toda a aplicação.
 */
@Singleton
public class InvestorRepository extends BaseRepository implements IInvestorRepository {

    private static final String INVESTORS_COLLECTION = "investors";
    private final Map<String, ListenerRegistration> activeListeners = new HashMap<>();

    @Inject
    public InvestorRepository() {
        super();
    }

    /**
     * Busca os detalhes de um único investidor pelo seu ID.
     */
    @Override // CORRIGIDO: Adicionado Override
    public void getInvestorDetails(String investorId, ResultCallback<Investor> callback) {
        db.collection(INVESTORS_COLLECTION).document(investorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Investor investor = documentSnapshot.toObject(Investor.class);
                        callback.onResult(new Result.Success<>(investor));
                    } else {
                        callback.onResult(new Result.Error<>(new Exception("Investidor não encontrado.")));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Busca a lista completa de todos os investidores.
     */
    @Override // CORRIGIDO: Adicionado Override
    public void getInvestidores(ResultCallback<List<Investor>> callback) {
        db.collection(INVESTORS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    try {
                        List<Investor> investidores = querySnapshot.toObjects(Investor.class);
                        callback.onResult(new Result.Success<>(investidores));
                    } catch (Exception e) {
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * CORRIGIDO: Método de paginação agora está totalmente implementado.
     */
    @Override
    public void getInvestidoresPaginados(int pageSize, @Nullable DocumentSnapshot lastVisible, @Nullable List<String> filterAreas, ResultCallback<InvestorPagingResult> callback) {
        // Constrói a query base
        Query query = db.collection(INVESTORS_COLLECTION)
                .whereEqualTo("status", "ACTIVE") // Garante que apenas investidores ativos sejam listados
                .orderBy("nome")
                .limit(pageSize);

        if (filterAreas != null && !filterAreas.isEmpty()) {
            // "whereArrayContainsAny" encontra investidores que tenham
            // PELO MENOS UMA das áreas da ideia.
            query = query.whereArrayContainsAny("areas", filterAreas);
        }

        // Adiciona o cursor "startAfter" se não for a primeira página
        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        // Executa a query
        query.get().addOnSuccessListener(querySnapshot -> {
            try {
                // Converte os documentos para a lista de investidores
                List<Investor> investidores = querySnapshot.toObjects(Investor.class);

                // Pega o snapshot do último documento da lista para ser o próximo cursor
                DocumentSnapshot newLastVisible = null;
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    newLastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                }

                // Retorna o objeto de resultado da paginação
                InvestorPagingResult result = new InvestorPagingResult(investidores, newLastVisible);
                callback.onResult(new Result.Success<>(result));

            } catch (Exception e) {
                callback.onResult(new Result.Error<>(e));
            }
        }).addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Cria o documento do investidor no Firestore.
     */
    @Override // CORRIGIDO: Adicionado Override
    public void createInvestorDocument(Investor investor, ResultCallback<Void> callback) {
        db.collection(INVESTORS_COLLECTION).document(investor.getId())
                .set( investor)
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }

    /**
     * Ouve as mudanças em tempo real em um documento de investidor.
     */
    @Override // CORRIGIDO: Adicionado Override
    public void listenForInvestorVerification(String uid, EventListener<DocumentSnapshot> listener) {
        stopListening(uid); // Remove listener antigo, se houver
        ListenerRegistration registration = db.collection(INVESTORS_COLLECTION).document(uid)
                .addSnapshotListener(listener);
        activeListeners.put(uid, registration);
    }

    /**
     * Para de ouvir as mudanças em um documento de investidor.
     */
    @Override // CORRIGIDO: Adicionado Override
    public void stopListening(String uid) {
        if (activeListeners.containsKey(uid)) {
            activeListeners.get(uid).remove();
            activeListeners.remove(uid);
        }
    }

    @Override
    public void updateProfileDetails(String uid, Map<String, Object> profileData, ResultCallback<Void> callback) {
        db.collection(INVESTORS_COLLECTION).document(uid)
                .update(profileData) // .update() é mais seguro que .set() aqui
                .addOnSuccessListener(aVoid -> callback.onResult(new Result.Success<>(null)))
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}