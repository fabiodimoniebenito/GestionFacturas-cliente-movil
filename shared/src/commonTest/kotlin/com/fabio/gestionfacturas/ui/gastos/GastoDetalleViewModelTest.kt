package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.testutil.FakeCategoriaRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GastoDetalleViewModelTest {

    private val gastos = FakeGastoRepository()
    private val categorias = FakeCategoriaRepository()
    private val completo = Gasto(
        id = 5L,
        usuarioId = 7L,
        categoriaId = 2L,
        clienteId = 3L,
        emisor = "Papeleria",
        fechaEmision = LocalDate(2026, 10, 20),
        baseImponible = Importe.de("100,00"),
        iva = Importe.de("21,00"),
        moneda = "EUR",
        deducible = true,
        estado = EstadoGasto.BORRADOR,
        creadoEn = null,
    )

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = GastoDetalleViewModel(5L, gastos, categorias)

    @Test
    fun empiezaCargandoYPideElGastoPorId() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaObtener = puerta
        gastos.resultadoObtener = Result.success(completo)

        val viewModel = crearViewModel()
        assertEquals(CargaGastoDetalle.Cargando, viewModel.uiState.value.carga)

        puerta.complete(Unit)
        assertEquals(listOf(5L), gastos.obtenidos)
        assertTrue(viewModel.uiState.value.carga is CargaGastoDetalle.Contenido)
    }

    @Test
    fun muestraElGastoConElNombreDeLaCategoria() {
        gastos.resultadoObtener = Result.success(completo)
        categorias.resultadoListar = Result.success(listOf(Categoria(1L, "Otra", false), Categoria(2L, "Material", true)))

        val carga = crearViewModel().uiState.value.carga

        assertEquals(CargaGastoDetalle.Contenido(completo, "Material"), carga)
    }

    @Test
    fun siLaCategoriaFallaElGastoSeMuestraIgualSinNombre() {
        gastos.resultadoObtener = Result.success(completo)
        categorias.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)

        val carga = crearViewModel().uiState.value.carga

        assertEquals(CargaGastoDetalle.Contenido(completo, null), carga)
    }

    @Test
    fun unaCategoriaDesconocidaDejaElNombreVacio() {
        gastos.resultadoObtener = Result.success(completo)
        categorias.resultadoListar = Result.success(listOf(Categoria(9L, "Otra", false)))

        assertEquals(CargaGastoDetalle.Contenido(completo, null), crearViewModel().uiState.value.carga)
    }

    @Test
    fun sinCategoriaNoSePideLaListaDeCategorias() {
        val sinCategoria = completo.copy(categoriaId = null)
        gastos.resultadoObtener = Result.success(sinCategoria)

        val carga = crearViewModel().uiState.value.carga

        assertEquals(CargaGastoDetalle.Contenido(sinCategoria, null), carga)
        assertEquals(0, categorias.llamadasListar)
    }

    @Test
    fun conTodosLosCamposNulosSeCargaSinFallar() {
        val vacio = gastoVacio(5L)
        gastos.resultadoObtener = Result.success(vacio)

        val estado = crearViewModel().uiState.value

        assertEquals(CargaGastoDetalle.Contenido(vacio, null), estado.carga)
        assertFalse(estado.puedeRevisar)
        assertTrue(estado.puedeEditar)
    }

    @Test
    fun cadaErrorDeCargaSeExponeTalCual() {
        for (error in listOf(ErrorAuth.ServidorInaccesible, ErrorAuth.DemasiadasPeticiones(30), ErrorAuth.Inesperado)) {
            gastos.resultadoObtener = Result.failure(error)

            val estado = crearViewModel().uiState.value

            assertEquals(CargaGastoDetalle.Error(error), estado.carga)
            assertFalse(estado.puedeEditar)
            assertFalse(estado.puedeRevisar)
        }
    }

    @Test
    fun unFalloQueNoEsErrorAuthSeConvierteEnInesperado() {
        gastos.resultadoObtener = Result.failure(IllegalStateException("boom"))

        assertEquals(CargaGastoDetalle.Error(ErrorAuth.Inesperado), crearViewModel().uiState.value.carga)
    }

    @Test
    fun un404EnSusDosFormasEsNoEncontrado() {
        for (error in listOf(ErrorAuth.NoEncontrado, ErrorAuth.NoEncontradoConDetalle("Gasto no encontrado"))) {
            gastos.resultadoObtener = Result.failure(error)

            assertEquals(CargaGastoDetalle.NoEncontrado, crearViewModel().uiState.value.carga)
        }
    }

    @Test
    fun reintentarTrasUnErrorVuelveAPedirElGasto() {
        gastos.resultadoObtener = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = crearViewModel()

        gastos.resultadoObtener = Result.success(completo)
        viewModel.onReintentar()

        assertEquals(listOf(5L, 5L), gastos.obtenidos)
        assertTrue(viewModel.uiState.value.carga is CargaGastoDetalle.Contenido)
    }

    @Test
    fun reintentarMientrasSeCargaNoLanzaOtraPeticion() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaObtener = puerta
        gastos.resultadoObtener = Result.success(completo)
        val viewModel = crearViewModel()

        viewModel.onReintentar()

        assertEquals(listOf(5L), gastos.obtenidos)
        puerta.complete(Unit)
    }

    @Test
    fun soloSePuedeRevisarUnGastoBorradorOProcesado() {
        val esperado = mapOf(
            EstadoGasto.BORRADOR to true,
            EstadoGasto.PROCESADA to true,
            EstadoGasto.REVISADA to false,
            EstadoGasto.EXPORTADA to false,
        )
        for ((estado, puede) in esperado) {
            gastos.resultadoObtener = Result.success(completo.copy(estado = estado))

            val ui = crearViewModel().uiState.value

            assertEquals(puede, ui.puedeRevisar, "puedeRevisar en $estado")
            assertTrue(ui.puedeEditar, "puedeEditar en $estado")
        }
    }

    @Test
    fun durantelaCargaNingunaAccionEstaDisponible() {
        gastos.puertaObtener = CompletableDeferred()

        val estado = crearViewModel().uiState.value

        assertFalse(estado.puedeEditar)
        assertFalse(estado.puedeRevisar)
    }

    // ---- review ------------------------------------------------------------------------------

    private fun viewModelConBorrador(): GastoDetalleViewModel {
        gastos.resultadoObtener = Result.success(completo)
        return crearViewModel()
    }

    @Test
    fun pedirRevisarAbreLaConfirmacionSinLlamarAlBackend() {
        val viewModel = viewModelConBorrador()

        viewModel.onPedirRevisar()

        assertTrue(viewModel.uiState.value.confirmandoRevision)
        assertTrue(gastos.revisados.isEmpty())
    }

    @Test
    fun noSePuedePedirRevisarUnGastoYaRevisado() {
        gastos.resultadoObtener = Result.success(completo.copy(estado = EstadoGasto.REVISADA))
        val viewModel = crearViewModel()

        viewModel.onPedirRevisar()

        assertFalse(viewModel.uiState.value.confirmandoRevision)
    }

    @Test
    fun cancelarCierraLaConfirmacion() {
        val viewModel = viewModelConBorrador()
        viewModel.onPedirRevisar()

        viewModel.onCancelarRevisar()

        assertFalse(viewModel.uiState.value.confirmandoRevision)
        assertTrue(gastos.revisados.isEmpty())
    }

    @Test
    fun confirmarRevisaRecargaElGastoYAvisaDeLosCambios() {
        val viewModel = viewModelConBorrador()
        gastos.resultadoRevisar = Result.success(completo.copy(estado = EstadoGasto.REVISADA))
        gastos.resultadoObtener = Result.success(completo.copy(estado = EstadoGasto.REVISADA))
        viewModel.onPedirRevisar()

        viewModel.onConfirmarRevisar()

        val estado = viewModel.uiState.value
        assertEquals(listOf(5L), gastos.revisados)
        assertEquals(MensajeGastoDetalle.Revisado, estado.mensaje)
        assertTrue(estado.huboCambios)
        assertFalse(estado.confirmandoRevision)
        assertFalse(estado.revisando)
        assertEquals(EstadoGasto.REVISADA, (estado.carga as CargaGastoDetalle.Contenido).gasto.estado)
        assertFalse(estado.puedeRevisar)

        viewModel.onCambiosNotificados()
        assertFalse(viewModel.uiState.value.huboCambios)
    }

    @Test
    fun mientrasSeRevisaLosBotonesEstanBloqueadosYUnSegundoConfirmarSeIgnora() {
        val viewModel = viewModelConBorrador()
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaRevisar = puerta
        gastos.resultadoRevisar = Result.success(completo)
        viewModel.onPedirRevisar()

        viewModel.onConfirmarRevisar()
        viewModel.onConfirmarRevisar()

        assertTrue(viewModel.uiState.value.revisando)
        assertFalse(viewModel.uiState.value.puedeRevisar)
        assertFalse(viewModel.uiState.value.puedeEditar)
        assertEquals(1, gastos.revisados.size)
        viewModel.onCancelarRevisar()
        assertTrue(viewModel.uiState.value.confirmandoRevision)
        puerta.complete(Unit)
    }

    @Test
    fun un400AlRevisarMuestraLoQueFalta() {
        val viewModel = viewModelConBorrador()
        val campos = mapOf("error" to "Falta la fecha de emisión")
        gastos.resultadoRevisar = Result.failure(ErrorAuth.Validacion(campos))
        viewModel.onPedirRevisar()

        viewModel.onConfirmarRevisar()

        assertEquals(
            MensajeGastoDetalle.RevisionFallida(ErrorAuth.Validacion(campos)),
            viewModel.uiState.value.mensaje,
        )
        assertFalse(viewModel.uiState.value.huboCambios)
        assertFalse(viewModel.uiState.value.confirmandoRevision)
        assertTrue(viewModel.uiState.value.carga is CargaGastoDetalle.Contenido)
    }

    @Test
    fun un409AlRevisarSeExponeComoConflictoDeEstado() {
        val viewModel = viewModelConBorrador()
        val error = ErrorAuth.ConflictoConDetalle("El gasto ya está revisado")
        gastos.resultadoRevisar = Result.failure(error)
        viewModel.onPedirRevisar()

        viewModel.onConfirmarRevisar()

        assertEquals(MensajeGastoDetalle.RevisionFallida(error), viewModel.uiState.value.mensaje)
    }

    @Test
    fun un404AlRevisarPasaAlEstadoNoEncontrado() {
        val viewModel = viewModelConBorrador()
        gastos.resultadoRevisar = Result.failure(ErrorAuth.NoEncontradoConDetalle("Gasto no encontrado"))
        viewModel.onPedirRevisar()

        viewModel.onConfirmarRevisar()

        assertEquals(CargaGastoDetalle.NoEncontrado, viewModel.uiState.value.carga)
        assertNull(viewModel.uiState.value.mensaje)
    }

    @Test
    fun otroErrorAlRevisarSeExpone() {
        val viewModel = viewModelConBorrador()
        gastos.resultadoRevisar = Result.failure(ErrorAuth.ServidorInaccesible)
        viewModel.onPedirRevisar()

        viewModel.onConfirmarRevisar()

        assertEquals(
            MensajeGastoDetalle.RevisionFallida(ErrorAuth.ServidorInaccesible),
            viewModel.uiState.value.mensaje,
        )
    }

    @Test
    fun consumirElMensajeLoBorra() {
        val viewModel = viewModelConBorrador()
        gastos.resultadoRevisar = Result.failure(ErrorAuth.Inesperado)
        viewModel.onPedirRevisar()
        viewModel.onConfirmarRevisar()

        viewModel.onMensajeConsumido()

        assertNull(viewModel.uiState.value.mensaje)
    }

    @Test
    fun recargarTrasEditarVuelveAPedirElGasto() {
        val viewModel = viewModelConBorrador()
        gastos.resultadoObtener = Result.success(completo.copy(emisor = "Nuevo"))

        viewModel.onRecargar()

        assertEquals(listOf(5L, 5L), gastos.obtenidos)
        assertEquals("Nuevo", (viewModel.uiState.value.carga as CargaGastoDetalle.Contenido).gasto.emisor)
    }
}
