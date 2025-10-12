package com.example.startuppulse.data;

import com.google.firebase.firestore.FirebaseFirestore;

public class InvestorRepository {

    private static volatile InvestorRepository instance;
    private final FirebaseFirestore firestore;

    public InvestorRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static InvestorRepository getInstance() {
        if (instance == null) {
            synchronized (InvestorRepository.class) {
                if (instance == null) {
                    instance = new InvestorRepository();
                }
            }
        }
        return instance;
    }

    public void getInvestorDetails(String investorId, ResultCallback<Investor> callback) {
        firestore.collection("investors").document(investorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Investor investor = documentSnapshot.toObject(Investor.class);
                        callback.onSuccess(investor);
                    } else {
                        callback.onError(new Exception("Investidor não encontrado."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}

