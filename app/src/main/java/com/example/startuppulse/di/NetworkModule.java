package com.example.startuppulse.di;

// import com.example.startuppulse.BuildConfig; // <<< REMOVA ESTE IMPORT
import android.app.Application; // <<< ADICIONE ESTE IMPORT
import android.content.pm.ApplicationInfo; // <<< ADICIONE ESTE IMPORT
import com.example.startuppulse.util.IBGEService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String IBGE_BASE_URL = "https://servicodados.ibge.gov.br/";

    @Provides
    @Singleton
    // <<< RECEBA 'Application' COMO PARÂMETRO >>>
    public OkHttpClient provideOkHttpClient(Application application) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        // <<< USE O CONTEXTO DA APPLICATION PARA VERIFICAR SE É DEBUGÁVEL >>>
        boolean isDebuggable = (0 != (application.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

        // Adiciona um interceptor para loggar requisições/respostas se for debugável
        if (isDebuggable) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        return clientBuilder.build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofitIBGE(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(IBGE_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public IBGEService provideIBGEService(Retrofit retrofit) {
        return retrofit.create(IBGEService.class);
    }
}