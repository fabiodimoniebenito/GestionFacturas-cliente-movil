package com.fabio.gestionfacturas.ui.ingresos

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoCobro
import com.fabio.gestionfacturas.testutil.FakeClienteRepository
import com.fabio.gestionfacturas.testutil.FakeIngresoRepository
import com.fabio.gestionfacturas.testutil.ingresoVacio
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IngresosViewModelTest {

    private val ingresos = FakeIngresoRepository()
    private val clientes = FakeClienteRepository()

    private val pendiente = ingresoVacio(1L, EstadoCobro.PENDIENTE, clienteId = 3L)
    private val cobrado = ingresoVacio(2L, EstadoCobro.COBRADA, clienteId = 4L)
    private val sinEstado = ingresoVacio(3L)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun cliente(id: Long, nombre: String, activo: Boolean = true) =
        Cliente(id = id, nombre = nombre, nif = "N$id", email = null, telefono = null, activo = activo)

    private fun crearViewModel() = IngresosViewModel(ingresos, clientes)

    @Test
    fun empiezaCargandoYMuestraElContenido() {
        val puerta = CompletableDeferred<Unit>()
        ingresos.puertaListar = puerta
        ingresos.resultadoListar = Result.success(listOf(pendiente, cobrado))

        val viewModel = crearViewModel()
        assertEquals(CargaIngresos.Cargando, viewModel.uiState.value.carga)

        puerta.complete(Unit)
        assertEquals(listOf(pendiente, cobrado), viewModel.uiState.value.visibles.map { it.ingreso })
    }

    @Test
    fun unaListaVaciaEsEstadoVacio() {
        val viewModel = crearViewModel()

        assertEquals(CargaIngresos.Vacio, viewModel.uiState.value.carga)
    }

    @Test
    fun unFalloAlCargarEsEstadoError() {
        ingresos.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)

        val viewModel = crearViewModel()

        assertEquals(CargaIngresos.Error(ErrorAuth.ServidorInaccesible), viewModel.uiState.value.carga)
    }

    // ---- filter ------------------------------------------------------------------------------

    @Test
    fun elFiltroPorDefectoMuestraTodosIncluidoElQueNoTieneEstado() {
        ingresos.resultadoListar = Result.success(listOf(pendiente, cobrado, sinEstado))

        val viewModel = crearViewModel()

        assertEquals(FiltroIngresos.TODOS, viewModel.uiState.value.filtro)
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.visibles.map { it.ingreso.id })
    }

    @Test
    fun elFiltroPendientesSoloDejaLosPendientes() {
        ingresos.resultadoListar = Result.success(listOf(pendiente, cobrado, sinEstado))
        val viewModel = crearViewModel()

        viewModel.onFiltroCambiado(FiltroIngresos.PENDIENTES)

        assertEquals(listOf(1L), viewModel.uiState.value.visibles.map { it.ingreso.id })
    }

    @Test
    fun elFiltroCobradosSoloDejaLosCobrados() {
        ingresos.resultadoListar = Result.success(listOf(pendiente, cobrado, sinEstado))
        val viewModel = crearViewModel()

        viewModel.onFiltroCambiado(FiltroIngresos.COBRADOS)

        assertEquals(listOf(2L), viewModel.uiState.value.visibles.map { it.ingreso.id })
    }

    @Test
    fun cambiarDeFiltroNoVuelveAPedirLaLista() {
        ingresos.resultadoListar = Result.success(listOf(pendiente, cobrado))
        val viewModel = crearViewModel()

        viewModel.onFiltroCambiado(FiltroIngresos.COBRADOS)
        viewModel.onFiltroCambiado(FiltroIngresos.TODOS)

        assertEquals(1, ingresos.llamadasListar)
    }

    @Test
    fun unFiltroSinCoincidenciasDejaLaListaVisibleVacia() {
        ingresos.resultadoListar = Result.success(listOf(pendiente))
        val viewModel = crearViewModel()

        viewModel.onFiltroCambiado(FiltroIngresos.COBRADOS)

        assertTrue(viewModel.uiState.value.carga is CargaIngresos.Contenido)
        assertTrue(viewModel.uiState.value.visibles.isEmpty())
    }

    // ---- cliente names -----------------------------------------------------------------------

    @Test
    fun elNombreDelClienteSeResuelveDesdeLaListaDeClientes() {
        ingresos.resultadoListar = Result.success(listOf(pendiente))
        clientes.resultadoListar = Result.success(listOf(cliente(3L, "Acme")))

        val viewModel = crearViewModel()

        assertEquals("Acme", viewModel.uiState.value.visibles.single().nombreCliente)
    }

    @Test
    fun unClienteQueFaltaEnLaListaQuedaSinNombreSinRomper() {
        ingresos.resultadoListar = Result.success(listOf(pendiente, cobrado))
        clientes.resultadoListar = Result.success(listOf(cliente(4L, "Beta")))

        val viewModel = crearViewModel()

        val filas = viewModel.uiState.value.visibles
        assertNull(filas[0].nombreCliente)
        assertEquals("Beta", filas[1].nombreCliente)
    }

    @Test
    fun siFallaLaListaDeClientesLasFilasSeMuestranSinNombre() {
        ingresos.resultadoListar = Result.success(listOf(pendiente))
        clientes.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)

        val viewModel = crearViewModel()

        assertNull(viewModel.uiState.value.visibles.single().nombreCliente)
    }

    // ---- refresh -----------------------------------------------------------------------------

    @Test
    fun unFalloAlRefrescarConservaLaListaYAvisa() {
        ingresos.resultadoListar = Result.success(listOf(pendiente))
        val viewModel = crearViewModel()

        ingresos.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)
        viewModel.onRefrescar()

        val estado = viewModel.uiState.value
        assertEquals(listOf(1L), estado.visibles.map { it.ingreso.id })
        assertEquals(MensajeIngresos.ActualizarFallido(ErrorAuth.ServidorInaccesible), estado.mensaje)
        assertEquals(false, estado.refrescando)
    }

    @Test
    fun volverDelDetalleConNoEncontradoAvisaYRecarga() {
        ingresos.resultadoListar = Result.success(listOf(pendiente))
        val viewModel = crearViewModel()

        viewModel.onVolverDelDetalle(ResultadoFormulario.NO_ENCONTRADO)

        assertEquals(MensajeIngresos.YaNoExiste, viewModel.uiState.value.mensaje)
        assertEquals(2, ingresos.llamadasListar)
    }

    @Test
    fun volverDelDetalleTrasUnCambioRecargaSinMensaje() {
        val viewModel = crearViewModel()

        viewModel.onVolverDelDetalle(ResultadoFormulario.GUARDADO)

        assertNull(viewModel.uiState.value.mensaje)
        assertEquals(2, ingresos.llamadasListar)
    }
}
