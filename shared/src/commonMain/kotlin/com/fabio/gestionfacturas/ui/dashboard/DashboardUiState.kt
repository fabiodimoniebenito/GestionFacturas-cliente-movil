package com.fabio.gestionfacturas.ui.dashboard

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.IngresoPendiente
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

enum class PresetPeriodo { ESTE_MES, TRIMESTRE_ACTUAL, ESTE_ANIO, PERSONALIZADO }

/** Whether the figures shown are the "Total" ones (with IVA) or the "Base" ones (without). */
enum class ModoImporte { CON_IVA, SIN_IVA }

data class Periodo(val desde: LocalDate, val hasta: LocalDate)

/** Why the custom range was not applied. */
enum class ErrorPeriodo { DESDE_INVALIDA, HASTA_INVALIDA, RANGO_INVERTIDO }

/** The calendar range of a preset around [hoy] (whole month, quarter or year); null for the custom one. */
fun periodoDePreset(preset: PresetPeriodo, hoy: LocalDate): Periodo? = when (preset) {
    PresetPeriodo.ESTE_MES -> tramo(hoy.year, hoy.month.number, meses = 1)
    PresetPeriodo.TRIMESTRE_ACTUAL -> tramo(hoy.year, ((hoy.month.number - 1) / 3) * 3 + 1, meses = 3)
    PresetPeriodo.ESTE_ANIO -> tramo(hoy.year, 1, meses = 12)
    PresetPeriodo.PERSONALIZADO -> null
}

private fun tramo(anio: Int, mesInicial: Int, meses: Int): Periodo {
    val desde = LocalDate(anio, mesInicial, 1)
    return Periodo(desde, desde.plus(meses, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY))
}

/** The five headline figures of a [ResumenPeriodo] for one [ModoImporte]. Only picks fields: no arithmetic. */
data class CifrasResumen(
    val facturado: Importe,
    val cobrado: Importe,
    val gastos: Importe,
    /** Cobrado minus gastos, computed by the backend. */
    val beneficioCaja: Importe,
    /** Facturado minus gastos, computed by the backend. */
    val beneficioFacturado: Importe,
)

fun ResumenPeriodo.cifras(modo: ModoImporte): CifrasResumen = when (modo) {
    ModoImporte.CON_IVA -> CifrasResumen(
        facturado = facturadoTotal,
        cobrado = cobradoTotal,
        gastos = gastosTotal,
        beneficioCaja = beneficioCajaTotal,
        beneficioFacturado = beneficioFacturadoTotal,
    )
    ModoImporte.SIN_IVA -> CifrasResumen(
        facturado = facturadoBase,
        cobrado = cobradoBase,
        gastos = gastosBase,
        beneficioCaja = beneficioCajaBase,
        beneficioFacturado = beneficioFacturadoBase,
    )
}

sealed interface CargaResumen {
    data object Cargando : CargaResumen
    data class Contenido(val resumen: ResumenPeriodo) : CargaResumen
    data class Error(val error: ErrorAuth) : CargaResumen
}

/** [nombreCliente] is null when the ingreso has no cliente or the cliente is not in the loaded list. */
data class FilaPendiente(val ingreso: IngresoPendiente, val nombreCliente: String?)

sealed interface CargaPendientes {
    data object Cargando : CargaPendientes
    data object Vacio : CargaPendientes
    data class Contenido(val total: Importe, val filas: List<FilaPendiente>) : CargaPendientes
    data class Error(val error: ErrorAuth) : CargaPendientes
}

data class DashboardUiState(
    val preset: PresetPeriodo,
    /** The range the resumen was last requested for. */
    val periodo: Periodo,
    /** Text of the custom range fields; they only take effect on "Aplicar". */
    val desdeTexto: String,
    val hastaTexto: String,
    val errorPersonalizado: ErrorPeriodo? = null,
    val modo: ModoImporte = ModoImporte.CON_IVA,
    val resumen: CargaResumen = CargaResumen.Cargando,
    val pendientes: CargaPendientes = CargaPendientes.Cargando,
)
