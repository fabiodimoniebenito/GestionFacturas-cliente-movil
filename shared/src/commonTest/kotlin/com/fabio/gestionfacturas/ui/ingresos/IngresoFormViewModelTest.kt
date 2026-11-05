package com.fabio.gestionfacturas.ui.ingresos

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.testutil.FakeClienteRepository
import com.fabio.gestionfacturas.testutil.FakeIngresoRepository
import com.fabio.gestionfacturas.testutil.ingresoVacio
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IngresoFormViewModelTest {

    private val ingresos = FakeIngresoRepository()
    private val clientes = FakeClienteRepository()

    private fun cliente(id: Long, nombre: String, activo: Boolean = true) =
        Cliente(id = id, nombre = nombre, nif = "N$id", email = null, telefono = null, activo = activo)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme"), cliente(4L, "Beta")))
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = IngresoFormViewModel(ingresos, clientes)

    private fun IngresoFormViewModel.rellenarMinimo() {
        onClienteChange(3L)
        onTotalChange("121")
    }

    // ---- selector ----------------------------------------------------------------------------

    @Test
    fun elSelectorSoloOfreceClientesActivos() {
        clientes.resultadoListar = Result.success(
            listOf(cliente(3L, "Acme"), cliente(4L, "Vieja", activo = false)),
        )

        val viewModel = crearViewModel()

        assertEquals(listOf(3L), viewModel.uiState.value.clientes.map { it.id })
        assertFalse(viewModel.uiState.value.sinClientesActivos)
    }

    @Test
    fun sinClientesActivosSeIndicaQueHaceFaltaUnCliente() {
        clientes.resultadoListar = Result.success(listOf(cliente(4L, "Vieja", activo = false)))

        val viewModel = crearViewModel()

        assertTrue(viewModel.uiState.value.sinClientesActivos)
    }

    @Test
    fun conLaListaDeClientesEnCargaNoSeDiceQueNoHayClientes() {
        clientes.puertaListar = CompletableDeferred()

        val viewModel = crearViewModel()

        assertTrue(viewModel.uiState.value.cargandoClientes)
        assertFalse(viewModel.uiState.value.sinClientesActivos)
    }

    @Test
    fun siFallaLaCargaDeClientesSeOfreceRecargarYNoSeDiceQueNoHay() {
        clientes.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = crearViewModel()

        assertTrue(viewModel.uiState.value.clientesFallidos)
        assertFalse(viewModel.uiState.value.sinClientesActivos)

        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme")))
        viewModel.onRecargarClientes()

        assertFalse(viewModel.uiState.value.clientesFallidos)
        assertEquals(1, viewModel.uiState.value.clientes.size)
    }

    @Test
    fun recargarTrasCrearUnClienteLoMuestraEnElSelector() {
        clientes.resultadoListar = Result.success(emptyList())
        val viewModel = crearViewModel()
        assertTrue(viewModel.uiState.value.sinClientesActivos)

        clientes.resultadoListar = Result.success(listOf(cliente(7L, "Nuevo")))
        viewModel.onRecargarClientes()

        assertFalse(viewModel.uiState.value.sinClientesActivos)
        assertEquals(listOf(7L), viewModel.uiState.value.clientes.map { it.id })
    }

    // ---- create ok ---------------------------------------------------------------------------

    @Test
    fun crearEnviaTodosLosCamposYDevuelveElIdDelNuevoIngreso() {
        ingresos.resultadoCrear = Result.success(ingresoVacio(42L, EstadoCobro.PENDIENTE))
        val viewModel = crearViewModel()
        viewModel.onClienteChange(3L)
        viewModel.onConceptoChange("  Diseno web ")
        viewModel.onFechaEmisionChange("2026-10-20")
        viewModel.onBaseImponibleChange("100,00")
        viewModel.onIvaChange("21")
        viewModel.onTotalChange("121,00")

        viewModel.onGuardar()

        val nuevo = ingresos.creados.single()
        assertEquals(3L, nuevo.clienteId)
        assertEquals("Diseno web", nuevo.concepto)
        assertEquals(LocalDate(2026, 10, 20), nuevo.fechaEmision)
        assertEquals("100.00", nuevo.baseImponible?.valor)
        assertEquals("21", nuevo.iva?.valor)
        assertEquals("121.00", nuevo.total.valor)
        assertEquals("EUR", nuevo.moneda)
        assertEquals(42L, viewModel.uiState.value.creadoId)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun laMonedaEmpiezaEnEurYLosOpcionalesVaciosNoSeEnvian() {
        ingresos.resultadoCrear = Result.success(ingresoVacio(1L))
        val viewModel = crearViewModel()
        assertEquals("EUR", viewModel.uiState.value.moneda)
        viewModel.rellenarMinimo()

        viewModel.onGuardar()

        val nuevo = ingresos.creados.single()
        assertNull(nuevo.concepto)
        assertNull(nuevo.fechaEmision)
        assertNull(nuevo.baseImponible)
        assertNull(nuevo.iva)
    }

    @Test
    fun noSeCalculaNadaAutomaticamente() {
        ingresos.resultadoCrear = Result.success(ingresoVacio(1L))
        val viewModel = crearViewModel()
        viewModel.onClienteChange(3L)
        viewModel.onBaseImponibleChange("100")
        viewModel.onIvaChange("21")

        assertEquals("", viewModel.uiState.value.total)
    }

    @Test
    fun alConsumirElIdCreadoQuedaLimpio() {
        ingresos.resultadoCrear = Result.success(ingresoVacio(5L))
        val viewModel = crearViewModel()
        viewModel.rellenarMinimo()
        viewModel.onGuardar()

        viewModel.onCreadoConsumido()

        assertNull(viewModel.uiState.value.creadoId)
    }

    @Test
    fun mientrasGuardaNoSePuedeEnviarDeNuevo() {
        val puerta = CompletableDeferred<Unit>()
        ingresos.puertaCrear = puerta
        ingresos.resultadoCrear = Result.success(ingresoVacio(5L))
        val viewModel = crearViewModel()
        viewModel.rellenarMinimo()

        viewModel.onGuardar()
        assertTrue(viewModel.uiState.value.guardando)
        viewModel.onGuardar()
        puerta.complete(Unit)

        assertEquals(1, ingresos.creados.size)
    }

    // ---- Importe normalization ---------------------------------------------------------------

    @Test
    fun lasComasSeNormalizanAPuntoYSeIgnoranLosEspaciosDeLosBordes() {
        ingresos.resultadoCrear = Result.success(ingresoVacio(1L))
        val viewModel = crearViewModel()
        viewModel.onClienteChange(3L)
        viewModel.onTotalChange(" 1210,5 ")
        viewModel.onBaseImponibleChange("1000.5")

        viewModel.onGuardar()

        val nuevo = ingresos.creados.single()
        assertEquals("1210.5", nuevo.total.valor)
        assertEquals("1000.5", nuevo.baseImponible?.valor)
    }

    @Test
    fun unImporteInvalidoNoLlamaAlRepositorio() {
        val viewModel = crearViewModel()
        viewModel.onClienteChange(3L)
        viewModel.onTotalChange("12,345")
        viewModel.onIvaChange("abc")
        viewModel.onBaseImponibleChange("-5")

        viewModel.onGuardar()

        assertTrue(ingresos.creados.isEmpty())
        val errores = viewModel.uiState.value.erroresLocales
        assertEquals(ErrorCampoIngreso.IMPORTE_INVALIDO, errores[CampoIngreso.TOTAL])
        assertEquals(ErrorCampoIngreso.IMPORTE_INVALIDO, errores[CampoIngreso.IVA])
        assertEquals(ErrorCampoIngreso.IMPORTE_INVALIDO, errores[CampoIngreso.BASE_IMPONIBLE])
    }

    // ---- local validation --------------------------------------------------------------------

    @Test
    fun elTotalYElClienteSonObligatorios() {
        val viewModel = crearViewModel()

        viewModel.onGuardar()

        val errores = viewModel.uiState.value.erroresLocales
        assertEquals(ErrorCampoIngreso.TOTAL_VACIO, errores[CampoIngreso.TOTAL])
        assertEquals(ErrorCampoIngreso.CLIENTE_VACIO, errores[CampoIngreso.CLIENTE])
        assertTrue(ingresos.creados.isEmpty())
    }

    @Test
    fun unaFechaInvalidaSeMarcaEnSuCampo() {
        val viewModel = crearViewModel()
        viewModel.rellenarMinimo()
        viewModel.onFechaEmisionChange("20/10/2026")

        viewModel.onGuardar()

        assertEquals(ErrorCampoIngreso.FECHA_INVALIDA, viewModel.uiState.value.erroresLocales[CampoIngreso.FECHA_EMISION])
        assertTrue(ingresos.creados.isEmpty())
    }

    @Test
    fun editarUnCampoLimpiaSuErrorLocal() {
        val viewModel = crearViewModel()
        viewModel.onGuardar()

        viewModel.onTotalChange("5")

        assertNull(viewModel.uiState.value.erroresLocales[CampoIngreso.TOTAL])
        assertEquals(ErrorCampoIngreso.CLIENTE_VACIO, viewModel.uiState.value.erroresLocales[CampoIngreso.CLIENTE])
    }

    // ---- backend errors ----------------------------------------------------------------------

    @Test
    fun un400DejaElMapaDeCamposParaMostrarloPorCampo() {
        val error = ErrorAuth.Validacion(mapOf("total" to "debe ser mayor o igual que 0", "moneda" to "no válida"))
        ingresos.resultadoCrear = Result.failure(error)
        val viewModel = crearViewModel()
        viewModel.rellenarMinimo()

        viewModel.onGuardar()

        val estado = viewModel.uiState.value
        assertEquals(error, estado.error)
        assertNull(estado.creadoId)
        assertFalse(estado.guardando)
    }

    @Test
    fun un404ConservaElMensajeYRecargaElSelectorQuitandoAlClienteDesaparecido() {
        ingresos.resultadoCrear = Result.failure(ErrorAuth.NoEncontradoConDetalle("Cliente no encontrado"))
        val viewModel = crearViewModel()
        viewModel.rellenarMinimo()
        assertEquals(1, clientes.llamadasListar)

        // Meanwhile the cliente 3 was deactivated.
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme", activo = false), cliente(4L, "Beta")))
        viewModel.onGuardar()

        val estado = viewModel.uiState.value
        assertEquals(ErrorAuth.NoEncontradoConDetalle("Cliente no encontrado"), estado.error)
        assertEquals(2, clientes.llamadasListar)
        assertEquals(listOf(4L), estado.clientes.map { it.id })
        assertNull(estado.clienteId)
        assertFalse(estado.guardando)
    }

    @Test
    fun otroFalloNoRecargaElSelector() {
        ingresos.resultadoCrear = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = crearViewModel()
        viewModel.rellenarMinimo()

        viewModel.onGuardar()

        assertEquals(ErrorAuth.ServidorInaccesible, viewModel.uiState.value.error)
        assertEquals(1, clientes.llamadasListar)
        assertEquals(3L, viewModel.uiState.value.clienteId)
    }
}
