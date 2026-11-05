package com.fabio.gestionfacturas.ui.ingresos

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.domain.model.Ingreso
import com.fabio.gestionfacturas.testutil.FakeClienteRepository
import com.fabio.gestionfacturas.testutil.FakeIngresoRepository
import com.fabio.gestionfacturas.testutil.ingresoVacio
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IngresoDetalleViewModelTest {

    private val ingresos = FakeIngresoRepository()
    private val clientes = FakeClienteRepository()
    private val hoy = LocalDate(2026, 11, 5)

    private val pendiente = ingresoVacio(9L, EstadoCobro.PENDIENTE, clienteId = 3L)
    private val cobrado = ingresoVacio(9L, EstadoCobro.COBRADA, clienteId = 3L)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = IngresoDetalleViewModel(9L, ingresos, clientes) { hoy }

    private fun conIngreso(ingreso: Ingreso): IngresoDetalleViewModel {
        ingresos.resultadoObtener = Result.success(ingreso)
        return crearViewModel()
    }

    private fun contenido(viewModel: IngresoDetalleViewModel) =
        viewModel.uiState.value.carga as CargaIngresoDetalle.Contenido

    // ---- load --------------------------------------------------------------------------------

    @Test
    fun cargaElIngresoPorIdConElNombreDelCliente() {
        clientes.resultadoListar = Result.success(
            listOf(Cliente(3L, "Acme", "N3", null, null, true)),
        )

        val viewModel = conIngreso(pendiente)

        assertEquals(listOf(9L), ingresos.obtenidos)
        assertEquals("Acme", contenido(viewModel).nombreCliente)
    }

    @Test
    fun unClienteDesactivadoQueNoEstaEnLaListaDejaElNombreVacioSinRomper() {
        clientes.resultadoListar = Result.success(emptyList())

        val viewModel = conIngreso(pendiente)

        assertNull(contenido(viewModel).nombreCliente)
        assertEquals(pendiente, contenido(viewModel).ingreso)
    }

    @Test
    fun un404AlCargarEsNoEncontrado() {
        ingresos.resultadoObtener = Result.failure(ErrorAuth.NoEncontradoConDetalle("Ingreso no encontrado"))

        val viewModel = crearViewModel()

        assertEquals(CargaIngresoDetalle.NoEncontrado, viewModel.uiState.value.carga)
    }

    @Test
    fun otroFalloAlCargarEsErrorYSePuedeReintentar() {
        ingresos.resultadoObtener = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = crearViewModel()
        assertEquals(CargaIngresoDetalle.Error(ErrorAuth.ServidorInaccesible), viewModel.uiState.value.carga)

        ingresos.resultadoObtener = Result.success(pendiente)
        viewModel.onReintentar()

        assertTrue(viewModel.uiState.value.carga is CargaIngresoDetalle.Contenido)
    }

    // ---- which action is enabled -------------------------------------------------------------

    @Test
    fun soloUnPendienteSePuedeCobrar() {
        val viewModel = conIngreso(pendiente)

        assertTrue(viewModel.uiState.value.puedeRegistrarCobro)
        assertFalse(viewModel.uiState.value.puedeRevertirCobro)
    }

    @Test
    fun soloUnCobradoSePuedeRevertir() {
        val viewModel = conIngreso(cobrado)

        assertFalse(viewModel.uiState.value.puedeRegistrarCobro)
        assertTrue(viewModel.uiState.value.puedeRevertirCobro)
    }

    @Test
    fun unEstadoDesconocidoNoPermiteNingunaAccion() {
        val viewModel = conIngreso(ingresoVacio(9L, EstadoCobro.DESCONOCIDO))

        assertFalse(viewModel.uiState.value.puedeRegistrarCobro)
        assertFalse(viewModel.uiState.value.puedeRevertirCobro)
    }

    @Test
    fun sinPermisoNoSeAbreElDialogoNiLaConfirmacion() {
        val viewModel = conIngreso(cobrado)

        viewModel.onPedirRegistrarCobro()
        assertNull(viewModel.uiState.value.dialogoCobro)

        val otro = conIngreso(pendiente)
        otro.onPedirRevertirCobro()
        assertFalse(otro.uiState.value.confirmandoReversion)
    }

    // ---- registrar cobro ---------------------------------------------------------------------

    @Test
    fun elDialogoDeCobroEmpiezaConLaFechaDeHoy() {
        val viewModel = conIngreso(pendiente)

        viewModel.onPedirRegistrarCobro()

        assertEquals("2026-11-05", viewModel.uiState.value.dialogoCobro?.fecha)
    }

    @Test
    fun registrarCobroEnviaLaFechaRefrescaYAvisaALaLista() {
        val viewModel = conIngreso(pendiente)
        ingresos.resultadoRegistrarCobro = Result.success(cobrado)
        ingresos.resultadoObtener = Result.success(cobrado)

        viewModel.onPedirRegistrarCobro()
        viewModel.onFechaCobroCambiada("2026-11-01")
        viewModel.onConfirmarRegistrarCobro()

        val estado = viewModel.uiState.value
        assertEquals(listOf<Pair<Long, LocalDate?>>(9L to LocalDate(2026, 11, 1)), ingresos.cobrosRegistrados)
        assertNull(estado.dialogoCobro)
        assertFalse(estado.operando)
        assertEquals(MensajeIngresoDetalle.CobroRegistrado, estado.mensaje)
        assertTrue(estado.huboCambios)
        assertEquals(EstadoCobro.COBRADA, contenido(viewModel).ingreso.estadoCobro)
        assertEquals(2, ingresos.obtenidos.size)
    }

    @Test
    fun unaFechaInvalidaNoLlamaAlRepositorioYMantieneElDialogo() {
        val viewModel = conIngreso(pendiente)
        viewModel.onPedirRegistrarCobro()

        viewModel.onFechaCobroCambiada("2026-13-40")
        viewModel.onConfirmarRegistrarCobro()

        assertTrue(ingresos.cobrosRegistrados.isEmpty())
        assertEquals(true, viewModel.uiState.value.dialogoCobro?.fechaInvalida)
    }

    @Test
    fun corregirLaFechaLimpiaElError() {
        val viewModel = conIngreso(pendiente)
        viewModel.onPedirRegistrarCobro()
        viewModel.onFechaCobroCambiada("mañana")
        viewModel.onConfirmarRegistrarCobro()

        viewModel.onFechaCobroCambiada("2026-11-05")

        assertEquals(false, viewModel.uiState.value.dialogoCobro?.fechaInvalida)
    }

    @Test
    fun un409AlRegistrarMuestraElMensajeDelBackendYCierraElDialogo() {
        val viewModel = conIngreso(pendiente)
        ingresos.resultadoRegistrarCobro =
            Result.failure(ErrorAuth.ConflictoConDetalle("La fecha de cobro es anterior a la de emisión"))

        viewModel.onPedirRegistrarCobro()
        viewModel.onConfirmarRegistrarCobro()

        val estado = viewModel.uiState.value
        assertEquals(ConflictoCobro("La fecha de cobro es anterior a la de emisión"), estado.conflicto)
        assertNull(estado.dialogoCobro)
        assertFalse(estado.operando)
        assertFalse(estado.huboCambios)
    }

    @Test
    fun un400AlRegistrarMantieneElDialogoConElErrorDelServidor() {
        val viewModel = conIngreso(pendiente)
        val error = ErrorAuth.Validacion(mapOf("fechaCobro" to "fecha no válida"))
        ingresos.resultadoRegistrarCobro = Result.failure(error)

        viewModel.onPedirRegistrarCobro()
        viewModel.onConfirmarRegistrarCobro()

        assertEquals(error, viewModel.uiState.value.dialogoCobro?.errorServidor)
        assertFalse(viewModel.uiState.value.operando)
    }

    @Test
    fun un404AlRegistrarPasaANoEncontrado() {
        val viewModel = conIngreso(pendiente)
        ingresos.resultadoRegistrarCobro = Result.failure(ErrorAuth.NoEncontradoConDetalle("Ingreso no encontrado"))

        viewModel.onPedirRegistrarCobro()
        viewModel.onConfirmarRegistrarCobro()

        assertEquals(CargaIngresoDetalle.NoEncontrado, viewModel.uiState.value.carga)
    }

    @Test
    fun unFalloDeRedAlRegistrarSeAvisaConUnMensaje() {
        val viewModel = conIngreso(pendiente)
        ingresos.resultadoRegistrarCobro = Result.failure(ErrorAuth.ServidorInaccesible)

        viewModel.onPedirRegistrarCobro()
        viewModel.onConfirmarRegistrarCobro()

        assertEquals(
            MensajeIngresoDetalle.OperacionFallida(ErrorAuth.ServidorInaccesible),
            viewModel.uiState.value.mensaje,
        )
        assertTrue(viewModel.uiState.value.carga is CargaIngresoDetalle.Contenido)
    }

    @Test
    fun cancelarElDialogoNoLlamaAlRepositorio() {
        val viewModel = conIngreso(pendiente)
        viewModel.onPedirRegistrarCobro()

        viewModel.onCancelarRegistrarCobro()

        assertNull(viewModel.uiState.value.dialogoCobro)
        assertTrue(ingresos.cobrosRegistrados.isEmpty())
    }

    // ---- revertir cobro ----------------------------------------------------------------------

    @Test
    fun revertirCobroPideConfirmacionAntesDeLlamar() {
        val viewModel = conIngreso(cobrado)

        viewModel.onPedirRevertirCobro()

        assertTrue(viewModel.uiState.value.confirmandoReversion)
        assertTrue(ingresos.cobrosRevertidos.isEmpty())
    }

    @Test
    fun revertirCobroConfirmadoRefrescaYAvisaALaLista() {
        val viewModel = conIngreso(cobrado)
        ingresos.resultadoRevertirCobro = Result.success(pendiente)
        ingresos.resultadoObtener = Result.success(pendiente)

        viewModel.onPedirRevertirCobro()
        viewModel.onConfirmarRevertirCobro()

        val estado = viewModel.uiState.value
        assertEquals(listOf(9L), ingresos.cobrosRevertidos)
        assertFalse(estado.confirmandoReversion)
        assertEquals(MensajeIngresoDetalle.CobroRevertido, estado.mensaje)
        assertTrue(estado.huboCambios)
        assertEquals(EstadoCobro.PENDIENTE, contenido(viewModel).ingreso.estadoCobro)
    }

    @Test
    fun un409AlRevertirMuestraElMensajeDelBackend() {
        val viewModel = conIngreso(cobrado)
        ingresos.resultadoRevertirCobro = Result.failure(ErrorAuth.ConflictoConDetalle("El ingreso no está cobrado"))

        viewModel.onPedirRevertirCobro()
        viewModel.onConfirmarRevertirCobro()

        assertEquals(ConflictoCobro("El ingreso no está cobrado"), viewModel.uiState.value.conflicto)
        assertFalse(viewModel.uiState.value.confirmandoReversion)
    }

    @Test
    fun un404AlRevertirPasaANoEncontrado() {
        val viewModel = conIngreso(cobrado)
        ingresos.resultadoRevertirCobro = Result.failure(ErrorAuth.NoEncontradoConDetalle(null))

        viewModel.onPedirRevertirCobro()
        viewModel.onConfirmarRevertirCobro()

        assertEquals(CargaIngresoDetalle.NoEncontrado, viewModel.uiState.value.carga)
    }

    @Test
    fun cancelarLaConfirmacionNoRevierte() {
        val viewModel = conIngreso(cobrado)
        viewModel.onPedirRevertirCobro()

        viewModel.onCancelarRevertirCobro()

        assertFalse(viewModel.uiState.value.confirmandoReversion)
        assertTrue(ingresos.cobrosRevertidos.isEmpty())
    }

    @Test
    fun unaNuevaAccionLimpiaElConflictoAnterior() {
        val viewModel = conIngreso(pendiente)
        ingresos.resultadoRegistrarCobro = Result.failure(ErrorAuth.ConflictoConDetalle("ya cobrado"))
        viewModel.onPedirRegistrarCobro()
        viewModel.onConfirmarRegistrarCobro()

        viewModel.onPedirRegistrarCobro()

        assertNull(viewModel.uiState.value.conflicto)
    }
}
