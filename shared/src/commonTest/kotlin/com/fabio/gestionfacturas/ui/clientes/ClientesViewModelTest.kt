package com.fabio.gestionfacturas.ui.clientes

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.testutil.FakeClienteRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientesViewModelTest {

    private val repositorio = FakeClienteRepository()

    private val ana = Cliente(1L, "Ana", "A1234567B", "ana@example.com", "600111222", true)
    private val luis = Cliente(2L, "Luis", "B7654321C", null, null, true)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = ClientesViewModel(repositorio)

    @Test
    fun mientrasCargaMuestraElEstadoCargando() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaListar = puerta

        val viewModel = crearViewModel()

        assertEquals(CargaClientes.Cargando, viewModel.uiState.value.carga)

        puerta.complete(Unit)

        assertEquals(CargaClientes.Vacio, viewModel.uiState.value.carga)
    }

    @Test
    fun conLaListaVaciaMuestraElEstadoVacio() {
        repositorio.resultadoListar = Result.success(emptyList())

        val viewModel = crearViewModel()

        assertEquals(CargaClientes.Vacio, viewModel.uiState.value.carga)
    }

    @Test
    fun conClientesMuestraElContenido() {
        repositorio.resultadoListar = Result.success(listOf(ana, luis))

        val viewModel = crearViewModel()

        assertEquals(CargaClientes.Contenido(listOf(ana, luis)), viewModel.uiState.value.carga)
    }

    @Test
    fun cadaErrorDeCargaSeExponeTalCual() {
        val errores = listOf(
            ErrorAuth.ServidorInaccesible,
            ErrorAuth.DemasiadasPeticiones(30),
            ErrorAuth.Inesperado,
        )
        for (error in errores) {
            repositorio.resultadoListar = Result.failure(error)

            val viewModel = crearViewModel()

            assertEquals(CargaClientes.Error(error), viewModel.uiState.value.carga)
        }
    }

    @Test
    fun unFalloQueNoEsErrorAuthSeExponeComoInesperado() {
        repositorio.resultadoListar = Result.failure(IllegalStateException("boom"))

        val viewModel = crearViewModel()

        assertEquals(CargaClientes.Error(ErrorAuth.Inesperado), viewModel.uiState.value.carga)
    }

    @Test
    fun reintentarTrasUnErrorVuelveACargarYMuestraElContenido() {
        repositorio.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = crearViewModel()
        repositorio.resultadoListar = Result.success(listOf(ana))

        viewModel.onRefrescar()

        assertEquals(CargaClientes.Contenido(listOf(ana)), viewModel.uiState.value.carga)
        assertEquals(2, repositorio.llamadasListar)
    }

    @Test
    fun refrescarMantieneLaListaVisibleMientrasCarga() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaListar = puerta
        repositorio.resultadoListar = Result.success(listOf(ana, luis))

        viewModel.onRefrescar()

        assertTrue(viewModel.uiState.value.refrescando)
        assertEquals(CargaClientes.Contenido(listOf(ana)), viewModel.uiState.value.carga)

        puerta.complete(Unit)

        assertFalse(viewModel.uiState.value.refrescando)
        assertEquals(CargaClientes.Contenido(listOf(ana, luis)), viewModel.uiState.value.carga)
    }

    @Test
    fun refrescarMientrasYaCargaSoloLlamaUnaVez() {
        repositorio.puertaListar = CompletableDeferred()
        val viewModel = crearViewModel()

        viewModel.onRefrescar()
        viewModel.onRefrescar()

        assertEquals(1, repositorio.llamadasListar)
    }

    @Test
    fun siRefrescarFallaConUnaListaVisibleLaMantieneYAvisa() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        repositorio.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)

        viewModel.onRefrescar()

        val estado = viewModel.uiState.value
        assertEquals(CargaClientes.Contenido(listOf(ana)), estado.carga)
        assertFalse(estado.refrescando)
        assertEquals(MensajeClientes.ActualizarFallido(ErrorAuth.ServidorInaccesible), estado.mensaje)
    }

    @Test
    fun pedirDesactivarGuardaElClienteSinLlamarAlRepositorio() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()

        viewModel.onPedirDesactivar(ana)

        assertEquals(ana, viewModel.uiState.value.clienteADesactivar)
        assertTrue(repositorio.desactivados.isEmpty())
    }

    @Test
    fun cancelarLaConfirmacionNoDesactivaNada() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        viewModel.onPedirDesactivar(ana)

        viewModel.onCancelarDesactivar()

        assertNull(viewModel.uiState.value.clienteADesactivar)
        assertTrue(repositorio.desactivados.isEmpty())
    }

    @Test
    fun desactivarConExitoRecargaLaLista() {
        repositorio.resultadoListar = Result.success(listOf(ana, luis))
        val viewModel = crearViewModel()
        viewModel.onPedirDesactivar(ana)
        repositorio.resultadoListar = Result.success(listOf(luis))

        viewModel.onConfirmarDesactivar()

        val estado = viewModel.uiState.value
        assertEquals(listOf(1L), repositorio.desactivados)
        assertEquals(2, repositorio.llamadasListar)
        assertEquals(CargaClientes.Contenido(listOf(luis)), estado.carga)
        assertNull(estado.clienteADesactivar)
        assertFalse(estado.desactivando)
        assertNull(estado.mensaje)
    }

    @Test
    fun mientrasDesactivaMarcaDesactivando() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaDesactivar = puerta
        viewModel.onPedirDesactivar(ana)

        viewModel.onConfirmarDesactivar()

        assertTrue(viewModel.uiState.value.desactivando)

        puerta.complete(Unit)

        assertFalse(viewModel.uiState.value.desactivando)
    }

    @Test
    fun confirmarDosVecesSoloDesactivaUnaVez() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        repositorio.puertaDesactivar = CompletableDeferred()
        viewModel.onPedirDesactivar(ana)

        viewModel.onConfirmarDesactivar()
        viewModel.onConfirmarDesactivar()

        assertEquals(1, repositorio.desactivados.size)
    }

    @Test
    fun siElClienteYaNoExisteMuestraElMensajeYRecargaLaLista() {
        repositorio.resultadoListar = Result.success(listOf(ana, luis))
        val viewModel = crearViewModel()
        viewModel.onPedirDesactivar(ana)
        repositorio.resultadoDesactivar = Result.failure(ErrorAuth.NoEncontrado)
        repositorio.resultadoListar = Result.success(listOf(luis))

        viewModel.onConfirmarDesactivar()

        val estado = viewModel.uiState.value
        assertEquals(MensajeClientes.YaNoExiste, estado.mensaje)
        assertEquals(CargaClientes.Contenido(listOf(luis)), estado.carga)
        assertEquals(2, repositorio.llamadasListar)
    }

    @Test
    fun otroErrorAlDesactivarMuestraElMensajeYNoRecarga() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        viewModel.onPedirDesactivar(ana)
        repositorio.resultadoDesactivar = Result.failure(ErrorAuth.ServidorInaccesible)

        viewModel.onConfirmarDesactivar()

        val estado = viewModel.uiState.value
        assertEquals(MensajeClientes.DesactivarFallido(ErrorAuth.ServidorInaccesible), estado.mensaje)
        assertNull(estado.clienteADesactivar)
        assertEquals(1, repositorio.llamadasListar)
    }

    @Test
    fun alVolverDelFormularioGuardadoRecargaLaLista() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        repositorio.resultadoListar = Result.success(listOf(ana, luis))

        viewModel.onVolverDelFormulario(ResultadoFormulario.GUARDADO)

        assertEquals(CargaClientes.Contenido(listOf(ana, luis)), viewModel.uiState.value.carga)
        assertNull(viewModel.uiState.value.mensaje)
        assertEquals(2, repositorio.llamadasListar)
    }

    @Test
    fun alVolverDelFormularioConUnClienteQueNoExisteAvisaYRecarga() {
        repositorio.resultadoListar = Result.success(listOf(ana, luis))
        val viewModel = crearViewModel()
        repositorio.resultadoListar = Result.success(listOf(luis))

        viewModel.onVolverDelFormulario(ResultadoFormulario.NO_ENCONTRADO)

        assertEquals(MensajeClientes.YaNoExiste, viewModel.uiState.value.mensaje)
        assertEquals(CargaClientes.Contenido(listOf(luis)), viewModel.uiState.value.carga)
        assertEquals(2, repositorio.llamadasListar)
    }

    @Test
    fun alVolverDelFormularioMientrasCargaMantieneElMensajeSinSegundaLlamada() {
        repositorio.puertaListar = CompletableDeferred()
        val viewModel = crearViewModel()

        viewModel.onVolverDelFormulario(ResultadoFormulario.NO_ENCONTRADO)

        assertEquals(MensajeClientes.YaNoExiste, viewModel.uiState.value.mensaje)
        assertEquals(1, repositorio.llamadasListar)
    }

    @Test
    fun elMensajeSeLimpiaAlConsumirlo() {
        repositorio.resultadoListar = Result.success(listOf(ana))
        val viewModel = crearViewModel()
        viewModel.onPedirDesactivar(ana)
        repositorio.resultadoDesactivar = Result.failure(ErrorAuth.Inesperado)
        viewModel.onConfirmarDesactivar()

        viewModel.onMensajeConsumido()

        assertNull(viewModel.uiState.value.mensaje)
    }
}
