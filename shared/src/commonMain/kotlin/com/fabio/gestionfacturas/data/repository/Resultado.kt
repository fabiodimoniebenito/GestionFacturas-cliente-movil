package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import kotlinx.coroutines.CancellationException

internal suspend inline fun <T> resultadoDe(bloque: () -> T): Result<T> {
    try {
        return Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ErrorAuth) {
        return Result.failure(e)
    } catch (e: Throwable) {
        return Result.failure(ErrorAuth.Inesperado)
    }
}
