package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Investor;
import com.google.android.gms.tasks.Task;
import java.util.List;

/**
 * Contrato para gerenciamento e consulta de investidores.
 */
public interface IInvestorRepository {

    Task<Result<Investor>> getInvestorById(String investorId);

    Task<Result<List<Investor>>> getAllInvestors();

    Task<Result<List<Investor>>> getInvestorsBySector(String sector);

    Task<Result<List<Investor>>> getInvestorsByLocation(String city, String state);

    Task<Result<Void>> addInvestor(Investor investor);

    Task<Result<Void>> updateInvestor(String investorId, Investor updatedInvestor);

    Task<Result<Void>> deleteInvestor(String investorId);
}
