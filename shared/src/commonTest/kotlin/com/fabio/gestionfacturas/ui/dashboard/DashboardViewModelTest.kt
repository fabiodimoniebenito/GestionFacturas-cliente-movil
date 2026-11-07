package com.fabio.gestionfacturas.ui.dashboard

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.domain.model.PendientesCobro
import com.fabio.gestionfacturas.domain.model.RentabilidadCliente
import com.fabio.gestionfacturas.domain.model.ResumenPeriodo
import com.fabio.gestionfacturas.testutil.FakeClienteRepository
import com.fabio.gestionfacturas.testutil.FakeDashboardRepository
import com.fabio.gestionfacturas.testutil.pendienteVacio
import com.fabio.gestionfacturas.testutil.resumenDistinto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dashboard = FakeDashboardRepository()
    private val clientes = FakeClienteRepository()

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(hoy: LocalDate = LocalDate(2026, 11, 7)) =
        DashboardViewModel(dashboard, clientes) { hoy }

    private fun cliente(id: Long, nombre: String) =
        Cliente(id = id, nombre = nombre, nif = "N$id", email = null, telefono = null, activo = true)

    private fun rango(desde: LocalDate, hasta: LocalDate) = desde to hasta

    // ---- presets -----------------------------------------------------------------------------

    @Test
    fun empiezaEnElTrimestreActualYCargaLasDosSecciones() {
        val viewModel = crearViewModel(LocalDate(2026, 11, 7))

        val estado = viewModel.uiState.value
        assertEquals(PresetPeriodo.TRIMESTRE_ACTUAL, estado.preset)
        assertEquals(Periodo(LocalDate(2026, 10, 1), LocalDate(2026, 12, 31)), estado.periodo)
        assertEquals(ModoImporte.CON_IVA, estado.modo)
        assertEquals(listOf(rango(LocalDate(2026, 10, 1), LocalDate(2026, 12, 31))), dashboard.llamadasResumen)
        assertEquals(1, dashboard.llamadasPendientes)
        assertIs<CargaResumen.Contenido>(estado.resumen)
    }

    @Test
    fun losPresetsCalculanElRangoCompletoDelMesTrimestreYAnio() {
        val hoy = LocalDate(2026, 5, 15)

        assertEquals(Periodo(LocalDate(2026, 5, 1), LocalDate(2026, 5, 31)), periodoDePreset(PresetPeriodo.ESTE_MES, hoy))
        assertEquals(
            Periodo(LocalDate(2026, 4, 1), LocalDate(2026, 6, 30)),
            periodoDePreset(PresetPeriodo.TRIMESTRE_ACTUAL, hoy),
        )
        assertEquals(Periodo(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31)), periodoDePreset(PresetPeriodo.ESTE_ANIO, hoy))
        assertNull(periodoDePreset(PresetPeriodo.PERSONALIZADO, hoy))
    }

    @Test
    fun losLimitesDeTrimestreYElFebreroBisiestoSeCalculanBien() {
        assertEquals(
            Periodo(LocalDate(2026, 1, 1), LocalDate(2026, 3, 31)),
            periodoDePreset(PresetPeriodo.TRIMESTRE_ACTUAL, LocalDate(2026, 3, 31)),
        )
        assertEquals(
            Periodo(LocalDate(2026, 4, 1), LocalDate(2026, 6, 30)),
            periodoDePreset(PresetPeriodo.TRIMESTRE_ACTUAL, LocalDate(2026, 4, 1)),
        )
        assertEquals(
            Periodo(LocalDate(2024, 2, 1), LocalDate(2024, 2, 29)),
            periodoDePreset(PresetPeriodo.ESTE_MES, LocalDate(2024, 2, 10)),
        )
        assertEquals(
            Periodo(LocalDate(2026, 10, 1), LocalDate(2026, 12, 31)),
            periodoDePreset(PresetPeriodo.TRIMESTRE_ACTUAL, LocalDate(2026, 12, 31)),
        )
    }

    @Test
    fun elegirUnPresetRecargaSoloElResumenConEseRango() {
        val viewModel = crearViewModel(LocalDate(2026, 11, 7))

        viewModel.onPresetSeleccionado(PresetPeriodo.ESTE_MES)

        assertEquals(PresetPeriodo.ESTE_MES, viewModel.uiState.value.preset)
        assertEquals(rango(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)), dashboard.llamadasResumen.last())
        assertEquals(2, dashboard.llamadasResumen.size)
        assertEquals(1, dashboard.llamadasPendientes)
    }

    @Test
    fun elegirElPresetYaActivoNoHaceNada() {
        val viewModel = crearViewModel()

        viewModel.onPresetSeleccionado(PresetPeriodo.TRIMESTRE_ACTUAL)

        assertEquals(1, dashboard.llamadasResumen.size)
    }

    @Test
    fun alCambiarDePeriodoElResumenAntiguoNoSeQuedaEnPantalla() {
        val viewModel = crearViewModel()
        val puerta = CompletableDeferred<Unit>()
        dashboard.puertaResumen = puerta

        viewModel.onPresetSeleccionado(PresetPeriodo.ESTE_ANIO)

        assertEquals(CargaResumen.Cargando, viewModel.uiState.value.resumen)
        puerta.complete(Unit)
        assertIs<CargaResumen.Contenido>(viewModel.uiState.value.resumen)
    }

    // ---- custom range ------------------------------------------------------------------------

    @Test
    fun personalizadoSoloMuestraLosCamposPrerrellenadosYNoLlama() {
        val viewModel = crearViewModel(LocalDate(2026, 11, 7))

        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)

        val estado = viewModel.uiState.value
        assertEquals(PresetPeriodo.PERSONALIZADO, estado.preset)
        assertEquals("2026-10-01", estado.desdeTexto)
        assertEquals("2026-12-31", estado.hastaTexto)
        assertEquals(1, dashboard.llamadasResumen.size)
    }

    @Test
    fun escribirEnLosCamposNoLanzaPeticiones() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)

        "2026-01-15".forEachIndexed { i, _ -> viewModel.onDesdeCambiado("2026-01-15".take(i + 1)) }
        "2026-02-20".forEachIndexed { i, _ -> viewModel.onHastaCambiado("2026-02-20".take(i + 1)) }

        assertEquals(1, dashboard.llamadasResumen.size)
    }

    @Test
    fun aplicarUnRangoValidoLlamaConLasFechasEscritas() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)
        viewModel.onDesdeCambiado("2026-01-15")
        viewModel.onHastaCambiado(" 2026-02-20 ")

        viewModel.onAplicarPersonalizado()

        assertEquals(Periodo(LocalDate(2026, 1, 15), LocalDate(2026, 2, 20)), viewModel.uiState.value.periodo)
        assertEquals(rango(LocalDate(2026, 1, 15), LocalDate(2026, 2, 20)), dashboard.llamadasResumen.last())
        assertEquals(2, dashboard.llamadasResumen.size)
        assertNull(viewModel.uiState.value.errorPersonalizado)
    }

    @Test
    fun undiaSoloEsUnRangoValido() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)
        viewModel.onDesdeCambiado("2026-03-01")
        viewModel.onHastaCambiado("2026-03-01")

        viewModel.onAplicarPersonalizado()

        assertEquals(2, dashboard.llamadasResumen.size)
    }

    @Test
    fun unRangoInvertidoBloqueaLaLlamada() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)
        viewModel.onDesdeCambiado("2026-03-02")
        viewModel.onHastaCambiado("2026-03-01")

        viewModel.onAplicarPersonalizado()

        assertEquals(ErrorPeriodo.RANGO_INVERTIDO, viewModel.uiState.value.errorPersonalizado)
        assertEquals(1, dashboard.llamadasResumen.size)
        assertIs<CargaResumen.Contenido>(viewModel.uiState.value.resumen)
    }

    @Test
    fun unaFechaIlegibleBloqueaLaLlamadaYDiceCual() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)

        viewModel.onDesdeCambiado("01/02/2026")
        viewModel.onAplicarPersonalizado()
        assertEquals(ErrorPeriodo.DESDE_INVALIDA, viewModel.uiState.value.errorPersonalizado)

        viewModel.onDesdeCambiado("2026-02-01")
        viewModel.onHastaCambiado("")
        viewModel.onAplicarPersonalizado()
        assertEquals(ErrorPeriodo.HASTA_INVALIDA, viewModel.uiState.value.errorPersonalizado)

        viewModel.onHastaCambiado("2026-02-30")
        viewModel.onAplicarPersonalizado()
        assertEquals(ErrorPeriodo.HASTA_INVALIDA, viewModel.uiState.value.errorPersonalizado)

        assertEquals(1, dashboard.llamadasResumen.size)
    }

    @Test
    fun editarUnCampoLimpiaElErrorDelRango() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)
        viewModel.onDesdeCambiado("x")
        viewModel.onAplicarPersonalizado()

        viewModel.onDesdeCambiado("2026-01-01")

        assertNull(viewModel.uiState.value.errorPersonalizado)
    }

    @Test
    fun aplicarSinEstarEnPersonalizadoNoHaceNada() {
        val viewModel = crearViewModel()

        viewModel.onAplicarPersonalizado()

        assertEquals(1, dashboard.llamadasResumen.size)
    }

    // ---- amount mode -------------------------------------------------------------------------

    @Test
    fun conIvaMuestraLosTotalesYSinIvaLasBases() {
        val resumen = resumenDistinto()

        val conIva = resumen.cifras(ModoImporte.CON_IVA)
        assertEquals("1.00", conIva.facturado.valor)
        assertEquals("3.00", conIva.cobrado.valor)
        assertEquals("5.00", conIva.gastos.valor)
        assertEquals("7.00", conIva.beneficioCaja.valor)
        assertEquals("9.00", conIva.beneficioFacturado.valor)

        val sinIva = resumen.cifras(ModoImporte.SIN_IVA)
        assertEquals("2.00", sinIva.facturado.valor)
        assertEquals("4.00", sinIva.cobrado.valor)
        assertEquals("6.00", sinIva.gastos.valor)
        assertEquals("8.00", sinIva.beneficioCaja.valor)
        assertEquals("10.00", sinIva.beneficioFacturado.valor)
    }

    @Test
    fun cambiarElModoNoHaceNingunaPeticion() {
        val viewModel = crearViewModel()

        viewModel.onModoCambiado(ModoImporte.SIN_IVA)

        assertEquals(ModoImporte.SIN_IVA, viewModel.uiState.value.modo)
        assertEquals(1, dashboard.llamadasResumen.size)
        assertEquals(1, dashboard.llamadasPendientes)
    }

    // ---- independent sections ----------------------------------------------------------------

    @Test
    fun siFallaElResumenLosPendientesSiguenEnPantalla() {
        dashboard.resultadoResumen = Result.failure(ErrorAuth.ServidorInaccesible)
        dashboard.resultadoPendientes = Result.success(PendientesCobro(listOf(pendienteVacio(1L)), Importe("12.00")))

        val estado = crearViewModel().uiState.value

        assertEquals(CargaResumen.Error(ErrorAuth.ServidorInaccesible), estado.resumen)
        val pendientes = assertIs<CargaPendientes.Contenido>(estado.pendientes)
        assertEquals("12.00", pendientes.total.valor)
    }

    @Test
    fun siFallanLosPendientesElResumenSigueEnPantalla() {
        dashboard.resultadoPendientes = Result.failure(ErrorAuth.Inesperado)

        val estado = crearViewModel().uiState.value

        assertIs<CargaResumen.Contenido>(estado.resumen)
        assertEquals(CargaPendientes.Error(ErrorAuth.Inesperado), estado.pendientes)
    }

    @Test
    fun reintentarUnaSeccionNoRecargaLaOtra() {
        dashboard.resultadoResumen = Result.failure(ErrorAuth.Inesperado)
        dashboard.resultadoPendientes = Result.failure(ErrorAuth.Inesperado)
        val viewModel = crearViewModel()
        dashboard.resultadoResumen = Result.success(resumenDistinto())

        viewModel.onReintentarResumen()

        assertIs<CargaResumen.Contenido>(viewModel.uiState.value.resumen)
        assertEquals(CargaPendientes.Error(ErrorAuth.Inesperado), viewModel.uiState.value.pendientes)
        assertEquals(2, dashboard.llamadasResumen.size)
        assertEquals(1, dashboard.llamadasPendientes)

        dashboard.resultadoPendientes = Result.success(PendientesCobro(emptyList(), Importe("0.00")))
        viewModel.onReintentarPendientes()

        assertEquals(CargaPendientes.Vacio, viewModel.uiState.value.pendientes)
        assertEquals(2, dashboard.llamadasPendientes)
        assertEquals(2, dashboard.llamadasResumen.size)
    }

    @Test
    fun un429SeGuardaTalCualYNadaSeReintentaSolo() {
        val limite = ErrorAuth.DemasiadasPeticiones(30)
        dashboard.resultadoResumen = Result.failure(limite)
        dashboard.resultadoPendientes = Result.failure(limite)

        val viewModel = crearViewModel()

        assertEquals(CargaResumen.Error(limite), viewModel.uiState.value.resumen)
        assertEquals(CargaPendientes.Error(limite), viewModel.uiState.value.pendientes)
        assertEquals(1, dashboard.llamadasResumen.size)
        assertEquals(1, dashboard.llamadasPendientes)
    }

    // ---- pendientes --------------------------------------------------------------------------

    @Test
    fun losPendientesTraenElNombreDelCliente() {
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme")))
        dashboard.resultadoPendientes = Result.success(
            PendientesCobro(
                listOf(pendienteVacio(1L, clienteId = 3L), pendienteVacio(2L, clienteId = 99L), pendienteVacio(3L)),
                Importe("30.00"),
            ),
        )

        val pendientes = assertIs<CargaPendientes.Contenido>(crearViewModel().uiState.value.pendientes)

        assertEquals(listOf("Acme", null, null), pendientes.filas.map { it.nombreCliente })
        assertEquals("30.00", pendientes.total.valor)
    }

    @Test
    fun siFallaLaListaDeClientesLosPendientesSalenSinNombre() {
        clientes.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)
        dashboard.resultadoPendientes = Result.success(PendientesCobro(listOf(pendienteVacio(1L, 3L)), Importe("1.00")))

        val pendientes = assertIs<CargaPendientes.Contenido>(crearViewModel().uiState.value.pendientes)

        assertNull(pendientes.filas.single().nombreCliente)
    }

    @Test
    fun sinPendientesElEstadoEsVacio() {
        assertEquals(CargaPendientes.Vacio, crearViewModel().uiState.value.pendientes)
    }

    // ---- rentabilidad ------------------------------------------------------------------------

    private fun entrada(clienteId: Long?, resumen: ResumenPeriodo = resumenDistinto()) =
        RentabilidadCliente(clienteId = clienteId, resumen = resumen)

    @Test
    fun laRentabilidadSeCargaConElPeriodoYConservaElOrdenDelBackend() {
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme"), cliente(4L, "Zeta")))
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(4L), entrada(null), entrada(3L)))

        val viewModel = crearViewModel(LocalDate(2026, 11, 7))

        assertEquals(listOf(rango(LocalDate(2026, 10, 1), LocalDate(2026, 12, 31))), dashboard.llamadasRentabilidad)
        val contenido = assertIs<CargaRentabilidad.Contenido>(viewModel.uiState.value.rentabilidad)
        assertEquals(listOf<Long?>(4L, null, 3L), contenido.entradas.map { it.clienteId })
        assertEquals(listOf<String?>("Zeta", null, "Acme"), contenido.entradas.map { it.clienteNombre })
    }

    @Test
    fun cadaEntradaConservaSusDiezCifrasSinTocarlas() {
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(3L)))

        val contenido = assertIs<CargaRentabilidad.Contenido>(crearViewModel().uiState.value.rentabilidad)

        assertEquals(resumenDistinto(), contenido.entradas.single().resumen)
        val sinIva = contenido.entradas.single().resumen.cifras(ModoImporte.SIN_IVA)
        assertEquals("2.00", sinIva.facturado.valor)
        assertEquals("10.00", sinIva.beneficioFacturado.valor)
    }

    @Test
    fun laEntradaSinClienteNoTieneIdNiNombreAunqueHayaClientes() {
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme")))
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(null)))

        val contenido = assertIs<CargaRentabilidad.Contenido>(crearViewModel().uiState.value.rentabilidad)

        assertNull(contenido.entradas.single().clienteId)
        assertNull(contenido.entradas.single().clienteNombre)
    }

    @Test
    fun unClienteQueNoEstaEnLaListaQuedaSinNombrePeroConId() {
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme")))
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(99L)))

        val contenido = assertIs<CargaRentabilidad.Contenido>(crearViewModel().uiState.value.rentabilidad)

        assertEquals(99L, contenido.entradas.single().clienteId)
        assertNull(contenido.entradas.single().clienteNombre)
    }

    @Test
    fun siFallaLaListaDeClientesLaRentabilidadSaleSinNombres() {
        clientes.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(3L)))

        val contenido = assertIs<CargaRentabilidad.Contenido>(crearViewModel().uiState.value.rentabilidad)

        assertNull(contenido.entradas.single().clienteNombre)
    }

    @Test
    fun sinEntradasLaRentabilidadEstaVacia() {
        assertEquals(CargaRentabilidad.Vacio, crearViewModel().uiState.value.rentabilidad)
    }

    @Test
    fun cambiarElModoNoRecargaLaRentabilidad() {
        val viewModel = crearViewModel()

        viewModel.onModoCambiado(ModoImporte.SIN_IVA)

        assertEquals(ModoImporte.SIN_IVA, viewModel.uiState.value.modo)
        assertEquals(1, dashboard.llamadasRentabilidad.size)
    }

    @Test
    fun cambiarDePeriodoRecargaLaRentabilidadConEseRango() {
        val viewModel = crearViewModel(LocalDate(2026, 11, 7))

        viewModel.onPresetSeleccionado(PresetPeriodo.ESTE_MES)

        assertEquals(rango(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)), dashboard.llamadasRentabilidad.last())
        assertEquals(2, dashboard.llamadasRentabilidad.size)
    }

    @Test
    fun unRangoPersonalizadoInvalidoNoRecargaLaRentabilidad() {
        val viewModel = crearViewModel()
        viewModel.onPresetSeleccionado(PresetPeriodo.PERSONALIZADO)
        viewModel.onDesdeCambiado("2026-03-02")
        viewModel.onHastaCambiado("2026-03-01")

        viewModel.onAplicarPersonalizado()

        assertEquals(1, dashboard.llamadasRentabilidad.size)
    }

    @Test
    fun siFallaLaRentabilidadElResumenYLosPendientesSiguenEnPantalla() {
        dashboard.resultadoRentabilidad = Result.failure(ErrorAuth.DemasiadasPeticiones(10))
        dashboard.resultadoPendientes = Result.success(PendientesCobro(listOf(pendienteVacio(1L)), Importe("1.00")))

        val estado = crearViewModel().uiState.value

        assertEquals(CargaRentabilidad.Error(ErrorAuth.DemasiadasPeticiones(10)), estado.rentabilidad)
        assertIs<CargaResumen.Contenido>(estado.resumen)
        assertIs<CargaPendientes.Contenido>(estado.pendientes)
    }

    @Test
    fun siFallaElResumenLaRentabilidadSigueEnPantalla() {
        dashboard.resultadoResumen = Result.failure(ErrorAuth.Inesperado)
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(3L)))

        val estado = crearViewModel().uiState.value

        assertEquals(CargaResumen.Error(ErrorAuth.Inesperado), estado.resumen)
        assertIs<CargaRentabilidad.Contenido>(estado.rentabilidad)
    }

    @Test
    fun reintentarLaRentabilidadNoRecargaLasOtrasSecciones() {
        dashboard.resultadoRentabilidad = Result.failure(ErrorAuth.Inesperado)
        val viewModel = crearViewModel()
        dashboard.resultadoRentabilidad = Result.success(listOf(entrada(3L)))

        viewModel.onReintentarRentabilidad()

        assertIs<CargaRentabilidad.Contenido>(viewModel.uiState.value.rentabilidad)
        assertEquals(2, dashboard.llamadasRentabilidad.size)
        assertEquals(1, dashboard.llamadasResumen.size)
        assertEquals(1, dashboard.llamadasPendientes)
    }

    @Test
    fun alVolverDelDetalleTambienSeRecargaLaRentabilidad() {
        val viewModel = crearViewModel()

        viewModel.onVolverDelDetalle()

        assertEquals(2, dashboard.llamadasRentabilidad.size)
    }

    // ---- refresh after the detail ------------------------------------------------------------

    @Test
    fun alVolverDelDetalleSeRecargaTodoSinBlanquearLoQueHabia() {
        dashboard.resultadoPendientes = Result.success(PendientesCobro(listOf(pendienteVacio(1L)), Importe("5.00")))
        val viewModel = crearViewModel()
        val puertaResumen = CompletableDeferred<Unit>()
        val puertaPendientes = CompletableDeferred<Unit>()
        dashboard.puertaResumen = puertaResumen
        dashboard.puertaPendientes = puertaPendientes

        viewModel.onVolverDelDetalle()

        assertEquals(2, dashboard.llamadasResumen.size)
        assertEquals(2, dashboard.llamadasPendientes)
        assertIs<CargaResumen.Contenido>(viewModel.uiState.value.resumen)
        assertIs<CargaPendientes.Contenido>(viewModel.uiState.value.pendientes)

        dashboard.resultadoPendientes = Result.success(PendientesCobro(emptyList(), Importe("0.00")))
        puertaResumen.complete(Unit)
        puertaPendientes.complete(Unit)

        assertEquals(CargaPendientes.Vacio, viewModel.uiState.value.pendientes)
    }

    @Test
    fun siLaRecargaFallaSeMuestraElErrorEnVezDeDatosViejos() {
        val viewModel = crearViewModel()
        dashboard.resultadoResumen = Result.failure(ErrorAuth.ServidorInaccesible)

        viewModel.onVolverDelDetalle()

        assertEquals(CargaResumen.Error(ErrorAuth.ServidorInaccesible), viewModel.uiState.value.resumen)
    }
}
