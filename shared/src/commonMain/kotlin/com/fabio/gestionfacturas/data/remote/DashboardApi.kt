package com.fabio.gestionfacturas.data.remote

import com.fabio.gestionfacturas.data.remote.dto.PendientesCobroResponse
import com.fabio.gestionfacturas.data.remote.dto.RentabilidadClienteResponse
import com.fabio.gestionfacturas.data.remote.dto.ResumenPeriodoResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate

/** The endpoints under `/api/dashboard`. Must be given the AUTHENTICATED client (Bearer plugin). */
class DashboardApi(private val client: HttpClient) {

    /** Dates are sent as `yyyy-MM-dd` (`LocalDate.toString()`). */
    suspend fun resumen(desde: LocalDate, hasta: LocalDate): ResumenPeriodoResponse =
        ejecutarLlamada {
            client.get("/api/dashboard/resumen") {
                parameter("desde", desde.toString())
                parameter("hasta", hasta.toString())
            }
        }

    suspend fun rentabilidad(desde: LocalDate, hasta: LocalDate): List<RentabilidadClienteResponse> =
        ejecutarLlamada {
            client.get("/api/dashboard/rentabilidad-clientes") {
                parameter("desde", desde.toString())
                parameter("hasta", hasta.toString())
            }
        }

    suspend fun pendientesCobro(): PendientesCobroResponse =
        ejecutarLlamada { client.get("/api/dashboard/pendientes-cobro") }
}
