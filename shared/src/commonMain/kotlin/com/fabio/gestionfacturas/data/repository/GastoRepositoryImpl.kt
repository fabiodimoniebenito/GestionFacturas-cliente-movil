package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.GastoApi
import com.fabio.gestionfacturas.domain.model.DatosGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.NuevoGasto
import com.fabio.gestionfacturas.domain.repository.GastoRepository

class GastoRepositoryImpl(private val api: GastoApi) : GastoRepository {

    override suspend fun listar(): Result<List<Gasto>> =
        resultadoDe { api.listar().map { it.aModelo() } }

    override suspend fun obtener(id: Long): Result<Gasto> =
        resultadoDe { api.obtener(id).aModelo() }

    override suspend fun crear(nuevo: NuevoGasto): Result<Gasto> =
        resultadoDe { api.crear(nuevo.aRequest()).aModelo() }

    override suspend fun editar(id: Long, datos: DatosGasto): Result<Gasto> =
        resultadoDe { api.editar(id, datos.aRequest()).aModelo() }

    override suspend fun revisar(id: Long): Result<Gasto> =
        resultadoDe { api.revisar(id).aModelo() }
}
