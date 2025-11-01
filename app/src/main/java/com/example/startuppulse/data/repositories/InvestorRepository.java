package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.example.startuppulse.common.ResultCallback;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositório para todas as operações de dados relacionadas a Investidores.
 * Gerenciado pelo Hilt como um Singleton para toda a aplicação.
 */
@Singleton
public class InvestorRepository extends BaseRepository{

    private static final String INVESTORS_COLLECTION = "investors"; // Corrigido para "investors" como no seu código original

    @Inject
    public InvestorRepository() {
        super();
    }

    /**
     * Busca os detalhes de um único investidor pelo seu ID.
     * @param investorId O ID do investidor a ser buscado.
     * @param callback O callback que será chamado com o resultado.
     */
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
     * (Método migrado do FirestoreHelper)
     * @param callback O callback que será chamado com o resultado.
     */
    public void getInvestidores(ResultCallback<List<Investor>> callback) {
        db.collection(INVESTORS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    try {
                        List<Investor> investidores = querySnapshot.toObjects(Investor.class);
                        callback.onResult(new Result.Success<>(investidores));
                    } catch (Exception e) {
                        // Captura possíveis erros durante a conversão dos objetos
                        callback.onResult(new Result.Error<>(e));
                    }
                })
                .addOnFailureListener(e -> callback.onResult(new Result.Error<>(e)));
    }
}