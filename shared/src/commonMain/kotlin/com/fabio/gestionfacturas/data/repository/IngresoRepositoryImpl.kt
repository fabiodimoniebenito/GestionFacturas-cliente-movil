package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.IngresoApi
import com.fabio.gestionfacturas.data.remote.dto.RegistrarCobroRequest
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.domain.model.NuevoIngreso
import com.fabio.gestionfacturas.domain.repository.IngresoRepository
import kotlinx.datetime.LocalDate

class IngresoRepositoryImpl(private val api: IngresoApi) : IngresoRepository {

    override suspend fun listar(): Result<List<Ingreso>> =
        resultadoDe { api.listar().map { it.aModelo() } }

    override suspend fun obtener(id: Long): Result<Ingreso> =
        resultadoDe { api.obtener(id).aModelo() }

    override suspend fun crear(nuevo: NuevoIngreso): Result<Ingreso> =
        resultadoDe { api.crear(nuevo.aRequest()).aModelo() }

    override suspend fun registrarCobro(id: Long, fechaCobro: LocalDate?): Result<Ingreso> =
        resultadoDe { api.registrarCobro(id, RegistrarCobroRequest(fechaCobro?.toString())).aModelo() }

    override suspend fun revertirCobro(id: Long): Result<Ingreso> =
        resultadoDe { api.revertirCobro(id).aModelo() }
}
