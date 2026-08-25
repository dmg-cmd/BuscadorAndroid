package com.buscadorandroid.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.buscadorandroid.app.domain.repository.BusquedaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que reconstruye el índice de archivos en segundo plano para que las
 * búsquedas sean instantáneas. Se programa de forma periódica desde BuscadorApp.
 */
@HiltWorker
class IndexacionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repositorio: BusquedaRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            repositorio.indexarAhora()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
