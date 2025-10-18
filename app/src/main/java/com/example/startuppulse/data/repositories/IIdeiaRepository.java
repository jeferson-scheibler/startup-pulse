package com.example.startuppulse.data.repositories;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Ideia;
import com.google.android.gms.tasks.Task;
import java.util.List;

/**
 * Contrato para operações de CRUD e consulta de Ideias.
 */
public interface IIdeiaRepository {

    Task<Result<Ideia>> createIdea(Ideia ideia);

    Task<Result<Void>> updateIdea(String ideiaId, Ideia updatedIdeia);

    Task<Result<Ideia>> getIdeaById(String ideiaId);

    Task<Result<List<Ideia>>> getIdeasByUser(String userId);

    Task<Result<List<Ideia>>> getAllIdeas();

    Task<Result<Void>> deleteIdea(String ideiaId);

    /**
     * Upload do pitch deck ou anexos relacionados à ideia.
     * @param ideiaId ID da ideia
     * @param filePath Caminho local do arquivo a ser enviado
     */
    Task<Result<String>> uploadPitchDeck(String ideiaId, String filePath);

    /**
     * Atualiza apenas alguns campos da ideia, sem sobrescrever todo o documento.
     */
    Task<Result<Void>> updatePartial(String ideiaId, String field, Object value);
}
