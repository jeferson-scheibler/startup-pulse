package com.example.startuppulse.data.repositories;

import androidx.annotation.Nullable;
import com.example.startuppulse.data.models.Investor;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;

/**
 * Classe wrapper para encapsular os resultados da paginação de investidores.
 */
public class InvestorPagingResult {
    private final List<Investor> investors;
    private final DocumentSnapshot lastVisible;

    public InvestorPagingResult(List<Investor> investors, @Nullable DocumentSnapshot lastVisible) {
        this.investors = investors;
        this.lastVisible = lastVisible;
    }

    public List<Investor> getInvestors() {
        return investors;
    }

    @Nullable
    public DocumentSnapshot getLastVisible() {
        return lastVisible;
    }
}