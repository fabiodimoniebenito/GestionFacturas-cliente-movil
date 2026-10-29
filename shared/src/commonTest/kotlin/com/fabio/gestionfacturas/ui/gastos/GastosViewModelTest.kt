package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.testutil.FakeGastoRepository
import com.fabio.gestionfacturas.testutil.gastoVacio
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GastosViewModelTest {

    private val repositorio = FakeGastoRepository()
    private val completo = Gasto(
        id = 1L,
        usuarioId = 7L,
        categoriaId = 2L,
        clienteId = 3L,
        emisor = "Papeleria",
        fechaEmision = LocalDate(2026, 10, 20),
        baseImponible = Importe.de("100,00"),
        iva = Importe.de("21,00"),
        moneda = "EUR",
        deducible = true,
        estado = EstadoGasto.REVISADA,
        creadoEn = null,
    )
    private val borrador = gastoVacio(2L, EstadoGasto.BORRADOR)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = GastosViewModel(repositorio)

    @Test
    fun empiezaCargandoYTerminaConElContenido() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaListar = puerta
        repositorio.resultadoListar = Result.success(listOf(completo, borrador))

        val viewModel = crearViewModel()
        assertEquals(CargaGastos.Cargando, viewModel.uiState.value.carga)

        puerta.complete(Unit)
        assertEquals(CargaGastos.Contenido(listOf(completo, borrador)), viewModel.uiState.value.carga)
    }

    @Test
    fun sinGastosMuestraElEstadoVacio() {
        repositorio.resultadoListar = Result.success(emptyList())

        assertEquals(CargaGastos.Vacio, crearViewModel().uiState.value.carga)
    }

    @Test
    fun unGastoConTodosLosCamposNulosSeCarga() {
        val vacio = gastoVacio(9L)
        repositorio.resultadoListar = Result.success(listOf(vacio))

        assertEquals(CargaGastos.Contenido(listOf(vacio)), crearViewModel().uiState.value.carga)
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

            assertEquals(CargaGastos.Error(error), crearViewModel().uiState.value.carga)
        }
    }

    @Test
    fun unFalloQueNoEsErrorAuthSeConvierteEnInesperado() {
        repositorio.resultadoListar = Result.failure(IllegalStateException("boom"))

        assertEquals(CargaGastos.Error(ErrorAuth.Inesperado), crearViewModel().uiState.value.carga)
    }

    @Test
    fun reintentarTrasUnErrorRecargaLaLista() {
        repositorio.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = crearViewModel()

        repositorio.resultadoListar = Result.success(listOf(completo))
        viewModel.onRefrescar()

        assertEquals(CargaGastos.Contenido(listOf(completo)), viewModel.uiState.value.carga)
        assertEquals(2, repositorio.llamadasListar)
    }

    @Test
    fun refrescarMantieneLaListaVisibleMientrasCarga() {
        repositorio.resultadoListar = Result.success(listOf(completo))
        val viewModel = crearViewModel()
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaListar = puerta
        repositorio.resultadoListar = Result.success(listOf(completo, borrador))

        viewModel.onRefrescar()
        assertEquals(CargaGastos.Contenido(listOf(completo)), viewModel.uiState.value.carga)
        assertEquals(true, viewModel.uiState.value.refrescando)

        puerta.complete(Unit)
        assertEquals(CargaGastos.Contenido(listOf(completo, borrador)), viewModel.uiState.value.carga)
        assertEquals(false, viewModel.uiState.value.refrescando)
    }

    @Test
    fun refrescarMientrasSeCargaNoLanzaOtraPeticion() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaListar = puerta
        val viewModel = crearViewModel()

        viewModel.onRefrescar()

        assertEquals(1, repositorio.llamadasListar)
        puerta.complete(Unit)
    }

    @Test
    fun unRefrescoFallidoConListaEnPantallaLaConservaYAvisa() {
        repositorio.resultadoListar = Result.success(listOf(completo))
        val viewModel = crearViewModel()
        repositorio.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)

        viewModel.onRefrescar()

        assertEquals(CargaGastos.Contenido(listOf(completo)), viewModel.uiState.value.carga)
        assertEquals(
            MensajeGastos.ActualizarFallido(ErrorAuth.ServidorInaccesible),
            viewModel.uiState.value.mensaje,
        )
        assertEquals(false, viewModel.uiState.value.refrescando)
    }

    @Test
    fun consumirElMensajeLoBorra() {
        repositorio.resultadoListar = Result.success(listOf(completo))
        val viewModel = crearViewModel()
        repositorio.resultadoListar = Result.failure(ErrorAuth.Inesperado)
        viewModel.onRefrescar()

        viewModel.onMensajeConsumido()

        assertNull(viewModel.uiState.value.mensaje)
    }
}
