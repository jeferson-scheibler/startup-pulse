package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Mentor;
import com.google.android.gms.tasks.Task;
import java.util.List;

/**
 * Contrato para gerenciamento e consulta de mentores.
 */
public interface IMentorRepository {

    Task<Result<Mentor>> getMentorById(String mentorId);

    Task<Result<List<Mentor>>> getAllMentors();

    Task<Result<List<Mentor>>> getMentorsByArea(String area);

    Task<Result<List<Mentor>>> getMentorsByLocation(String city, String state);

    Task<Result<Void>> addMentor(Mentor mentor);

    Task<Result<Void>> updateMentor(String mentorId, Mentor updatedMentor);

    Task<Result<Void>> deleteMentor(String mentorId);
}
