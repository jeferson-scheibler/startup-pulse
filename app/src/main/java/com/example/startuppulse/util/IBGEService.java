package com.example.startuppulse.util;

import com.example.startuppulse.data.models.Cidade;
import com.example.startuppulse.data.models.Estado;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface IBGEService {
    @GET("api/v1/localidades/estados?orderBy=nome")
    Call<List<Estado>> getEstados();

    @GET("api/v1/localidades/estados/{UF}/municipios?orderBy=nome")
    Call<List<Cidade>> getCidadesPorEstado(@Path("UF") String siglaEstado);
}