package com.buscadorandroid.app.di

import android.content.Context
import androidx.room.Room
import com.buscadorandroid.app.data.local.AppDatabase
import com.buscadorandroid.app.data.local.ArchivoDao
import com.buscadorandroid.app.data.local.HistorialDao
import com.buscadorandroid.app.data.MinubeSettingsRepository
import com.buscadorandroid.app.data.repository.BusquedaRepositoryImpl
import com.buscadorandroid.app.data.repository.SmbMinubeRepository
import com.buscadorandroid.app.domain.repository.BusquedaRepository
import com.buscadorandroid.app.domain.repository.MinubeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBinds {
    @Binds
    @Singleton
    abstract fun bindBusquedaRepository(impl: BusquedaRepositoryImpl): BusquedaRepository

    @Binds
    @Singleton
    abstract fun bindMinubeRepository(impl: SmbMinubeRepository): MinubeRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModuleProvides {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext contexto: Context): AppDatabase {
        return Room.databaseBuilder(contexto, AppDatabase::class.java, "buscador.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideArchivoDao(db: AppDatabase): ArchivoDao = db.archivoDao()

    @Provides
    @Singleton
    fun provideHistorialDao(db: AppDatabase): HistorialDao = db.historialDao()

    @Provides
    @Singleton
    fun provideMinubeSettingsRepository(@ApplicationContext contexto: Context): MinubeSettingsRepository =
        MinubeSettingsRepository(contexto)
}
