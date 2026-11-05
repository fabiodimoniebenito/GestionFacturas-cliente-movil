package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.domain.model.NuevoIngreso
import com.fabio.gestionfacturas.domain.repository.IngresoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/** An ingreso with every optional field null. */
fun ingresoVacio(id: Long, estadoCobro: EstadoCobro? = null, clienteId: Long? = null) = Ingreso(
    id = id,
    usuarioId = null,
    clienteId = clienteId,
    concepto = null,
    fechaEmision = null,
    baseImponible = null,
    iva = null,
    total = null,
    moneda = null,
    estadoCobro = estadoCobro,
    fechaCobro = null,
    creadoEn = null,
)

class FakeIngresoRepository : IngresoRepository {
    var resultadoListar: Result<List<Ingreso>> = Result.success(emptyList())
    var resultadoObtener: Result<Ingreso> = Result.failure(ErrorAuth.Inesperado)
    var resultadoCrear: Result<Ingreso> = Result.failure(ErrorAuth.Inesperado)
    var resultadoRegistrarCobro: Result<Ingreso> = Result.failure(ErrorAuth.Inesperado)
    var resultadoRevertirCobro: Result<Ingreso> = Result.failure(ErrorAuth.Inesperado)
    var puertaListar: CompletableDeferred<Unit>? = null
    var puertaObtener: CompletableDeferred<Unit>? = null

    var llamadasListar = 0
    val obtenidos = mutableListOf<Long>()
    val cobrosRegistrados = mutableListOf<Pair<Long, LocalDate?>>()
    val cobrosRevertidos = mutableListOf<Long>()

    override suspend fun listar(): Result<List<Ingreso>> {
        llamadasListar++
        puertaListar?.await()
        return resultadoListar
    }

    override suspend fun obtener(id: Long): Result<Ingreso> {
        obtenidos.add(id)
        puertaObtener?.await()
        return resultadoObtener
    }

    override suspend fun crear(nuevo: NuevoIngreso): Result<Ingreso> = resultadoCrear

    override suspend fun registrarCobro(id: Long, fechaCobro: LocalDate?): Result<Ingreso> {
        cobrosRegistrados.add(id to fechaCobro)
        return resultadoRegistrarCobro
    }

    override suspend fun revertirCobro(id: Long): Result<Ingreso> {
        cobrosRevertidos.add(id)
        return resultadoRevertirCobro
    }
}
