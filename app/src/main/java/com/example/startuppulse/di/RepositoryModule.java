package com.example.startuppulse.di;

import com.example.startuppulse.data.repositories.*;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

/**
 * Define os bindings entre interfaces e implementações concretas dos repositórios.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {
    @Binds
    @Singleton
    public abstract IIdeiaRepository bindIdeiaRepository(IdeiaRepository impl);

//    @Binds
//    @Singleton
//    public abstract IInvestorRepository bindInvestorRepository(InvestorRepository impl);

//    @Binds
//    @Singleton
//    public abstract IMentorRepository bindMentorRepository(MentorRepository impl);

    @Binds
    @Singleton
    public abstract IAuthRepository bindAuthRepository(AuthRepository impl);

    @Binds
    public abstract IStorageRepository bindStorageRepository(StorageRepository impl);

    @Binds
    public abstract IUserRepository bindUserRepository(UserRepository impl);
}
