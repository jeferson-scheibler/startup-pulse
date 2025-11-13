package com.example.startuppulse.ui.vortex;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.models.Spark;
import com.example.startuppulse.data.repositories.ISparkRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class VortexViewModel extends ViewModel {

    private final ISparkRepository sparkRepository;

    // LiveData para as coordenadas do Heatmap
    private final MutableLiveData<List<LatLng>> _heatmapLocations = new MutableLiveData<>();
    public LiveData<List<LatLng>> getHeatmapLocations() { return _heatmapLocations; }

    // LiveData para os pinos das Faíscas
    private final MutableLiveData<List<Spark>> _sparkPins = new MutableLiveData<>();
    public LiveData<List<Spark>> getSparkPins() { return _sparkPins; }

    // LiveData para erros
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    @Inject
    public VortexViewModel(ISparkRepository sparkRepository) {
        this.sparkRepository = sparkRepository;
    }

    /**
     * Carrega ambos os conjuntos de dados (heatmap e faíscas) em paralelo.
     */
    public void loadMapData() {
        // Carregar o Heatmap
        sparkRepository.getIdeaLocations(result -> {
            if (result instanceof Result.Success) {
                _heatmapLocations.setValue(((Result.Success<List<LatLng>>) result).data);
            } else if (result instanceof Result.Error) {
                _error.setValue("Erro ao carregar o heatmap.");
            }
        });

        // Carregar as Faíscas
        sparkRepository.getPublicSparks(result -> {
            if (result instanceof Result.Success) {
                _sparkPins.setValue(((Result.Success<List<Spark>>) result).data);
            } else if (result instanceof Result.Error) {
                _error.setValue("Erro ao carregar as faíscas.");
            }
        });
    }
}