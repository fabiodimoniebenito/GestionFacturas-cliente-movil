package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.ArchivoSeleccionado
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.TAMANO_MAXIMO_IMAGEN_BYTES
import com.fabio.gestionfacturas.domain.repository.ResultadoSeleccion
import com.fabio.gestionfacturas.testutil.FakeFileOpener
import com.fabio.gestionfacturas.testutil.FakeGastoRepository
import com.fabio.gestionfacturas.testutil.gastoVacio
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
class DigitalizarViewModelTest {

    private val opener = FakeFileOpener()
    private val gastos = FakeGastoRepository()
    private val jpg = ArchivoSeleccionado("factura.jpg", "image/jpeg", ByteArray(2048) { 3 })
    private val png = ArchivoSeleccionado("otra.png", "image/png", ByteArray(512) { 4 })

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = DigitalizarViewModel(opener, gastos)

    private fun DigitalizarViewModel.conArchivoSeleccionado(archivo: ArchivoSeleccionado = jpg): DigitalizarViewModel {
        opener.resultado = ResultadoSeleccion.Seleccionado(archivo)
        onElegirImagen()
        return this
    }

    // ---- selection ---------------------------------------------------------------------------

    @Test
    fun empiezaEnInicialSinArchivoNiError() {
        val estado = crearViewModel().uiState.value

        assertEquals(FaseDigitalizar.INICIAL, estado.fase)
        assertNull(estado.archivo)
        assertNull(estado.error)
        assertTrue(estado.puedeVolver)
    }

    @Test
    fun unaSeleccionValidaPasaASeleccionado() {
        val viewModel = crearViewModel().conArchivoSeleccionado()

        val estado = viewModel.uiState.value
        assertEquals(FaseDigitalizar.SELECCIONADO, estado.fase)
        assertEquals(jpg, estado.archivo)
        assertNull(estado.error)
    }

    @Test
    fun cancelarElSelectorNoCambiaNadaNiMuestraMensaje() {
        val viewModel = crearViewModel()
        opener.resultado = ResultadoSeleccion.Cancelado

        viewModel.onElegirImagen()

        assertEquals(DigitalizarUiState(), viewModel.uiState.value)
        assertEquals(1, opener.llamadas)
    }

    @Test
    fun cancelarConUnArchivoYaElegidoLoDejaIntacto() {
        val viewModel = crearViewModel().conArchivoSeleccionado()
        val antes = viewModel.uiState.value
        opener.resultado = ResultadoSeleccion.Cancelado

        viewModel.onElegirImagen()

        assertEquals(antes, viewModel.uiState.value)
    }

    @Test
    fun unErrorDelSelectorSeMuestraConSuMensaje() {
        val viewModel = crearViewModel()
        opener.resultado = ResultadoSeleccion.Error("No se pudo leer el archivo.")

        viewModel.onElegirImagen()

        assertEquals(ErrorDigitalizar.Seleccion("No se pudo leer el archivo."), viewModel.uiState.value.error)
        assertEquals(FaseDigitalizar.INICIAL, viewModel.uiState.value.fase)
    }

    @Test
    fun cambiarSustituyeElArchivo() {
        val viewModel = crearViewModel().conArchivoSeleccionado(jpg)

        viewModel.conArchivoSeleccionado(png)

        assertEquals(png, viewModel.uiState.value.archivo)
        assertEquals(FaseDigitalizar.SELECCIONADO, viewModel.uiState.value.fase)
    }

    // ---- invalid files are blocked before any request ---------------------------------------

    @Test
    fun unPdfSeBloqueaConSuPropioMensajeYNoSeEnviaNada() {
        val viewModel = crearViewModel().conArchivoSeleccionado(
            ArchivoSeleccionado("factura.pdf", "application/pdf", ByteArray(10) { 1 }),
        )

        assertEquals(ErrorDigitalizar.EsPdf, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.archivo)
        viewModel.onSubir()
        assertTrue(gastos.digitalizados.isEmpty())
    }

    @Test
    fun unFormatoNoSoportadoSeBloquea() {
        val viewModel = crearViewModel().conArchivoSeleccionado(
            ArchivoSeleccionado("foto.gif", "image/gif", ByteArray(10) { 1 }),
        )

        assertEquals(ErrorDigitalizar.FormatoNoSoportado, viewModel.uiState.value.error)
        assertTrue(gastos.digitalizados.isEmpty())
    }

    @Test
    fun unArchivoVacioSeBloquea() {
        val viewModel = crearViewModel().conArchivoSeleccionado(ArchivoSeleccionado("a.jpg", "image/jpeg", ByteArray(0)))

        assertEquals(ErrorDigitalizar.ArchivoVacio, viewModel.uiState.value.error)
        assertTrue(gastos.digitalizados.isEmpty())
    }

    @Test
    fun unArchivoDemasiadoGrandeSeBloqueaSinPeticion() {
        val viewModel = crearViewModel().conArchivoSeleccionado(
            ArchivoSeleccionado("grande.png", "image/png", ByteArray(TAMANO_MAXIMO_IMAGEN_BYTES + 1)),
        )

        assertEquals(ErrorDigitalizar.DemasiadoGrande, viewModel.uiState.value.error)
        assertEquals(FaseDigitalizar.INICIAL, viewModel.uiState.value.fase)
        assertTrue(gastos.digitalizados.isEmpty())
    }

    @Test
    fun unArchivoRechazadoNoPierdeElValidoQueYaHabia() {
        val viewModel = crearViewModel().conArchivoSeleccionado(jpg)

        viewModel.conArchivoSeleccionado(ArchivoSeleccionado("factura.pdf", "application/pdf", ByteArray(5) { 1 }))

        assertEquals(jpg, viewModel.uiState.value.archivo)
        assertEquals(ErrorDigitalizar.EsPdf, viewModel.uiState.value.error)
        assertTrue(gastos.digitalizados.isEmpty())
    }

    // ---- upload -------------------------------------------------------------------------------

    @Test
    fun subirSinArchivoNoHaceNada() {
        val viewModel = crearViewModel()

        viewModel.onSubir()

        assertTrue(gastos.digitalizados.isEmpty())
        assertEquals(FaseDigitalizar.INICIAL, viewModel.uiState.value.fase)
    }

    @Test
    fun subirConExitoEmiteLaNavegacionConElIdNuevo() {
        gastos.resultadoDigitalizar = Result.success(gastoVacio(42L, EstadoGasto.BORRADOR))
        val viewModel = crearViewModel().conArchivoSeleccionado()

        viewModel.onSubir()

        assertEquals(listOf(jpg), gastos.digitalizados)
        assertEquals(42L, viewModel.uiState.value.gastoCreadoId)
        viewModel.onNavegacionConsumida()
        assertNull(viewModel.uiState.value.gastoCreadoId)
    }

    @Test
    fun mientrasSubeElEstadoEsSubiendoYNoSePuedeVolver() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaDigitalizar = puerta
        gastos.resultadoDigitalizar = Result.success(gastoVacio(1L))
        val viewModel = crearViewModel().conArchivoSeleccionado()

        viewModel.onSubir()

        assertEquals(FaseDigitalizar.SUBIENDO, viewModel.uiState.value.fase)
        assertFalse(viewModel.uiState.value.puedeVolver)
        puerta.complete(Unit)
        assertTrue(viewModel.uiState.value.puedeVolver)
    }

    @Test
    fun noSePuedeVolverMientrasSubeAunqueFalle() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaDigitalizar = puerta
        val viewModel = crearViewModel().conArchivoSeleccionado()

        viewModel.onSubir()
        assertFalse(viewModel.uiState.value.puedeVolver)

        puerta.complete(Unit)
        assertEquals(FaseDigitalizar.ERROR, viewModel.uiState.value.fase)
        assertTrue(viewModel.uiState.value.puedeVolver)
    }

    @Test
    fun unSegundoSubirMientrasSubeNoEnviaOtraPeticion() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaDigitalizar = puerta
        gastos.resultadoDigitalizar = Result.success(gastoVacio(1L))
        val viewModel = crearViewModel().conArchivoSeleccionado()

        viewModel.onSubir()
        viewModel.onSubir()
        puerta.complete(Unit)

        assertEquals(1, gastos.digitalizados.size)
    }

    @Test
    fun noSePuedeElegirOtroArchivoMientrasSube() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaDigitalizar = puerta
        val viewModel = crearViewModel().conArchivoSeleccionado()
        viewModel.onSubir()
        val llamadasAntes = opener.llamadas

        viewModel.onElegirImagen()

        assertEquals(llamadasAntes, opener.llamadas)
        puerta.complete(Unit)
    }

    // ---- typed errors keep the file -----------------------------------------------------------

    private fun fallaCon(error: ErrorAuth): DigitalizarViewModel {
        gastos.resultadoDigitalizar = Result.failure(error)
        val viewModel = crearViewModel().conArchivoSeleccionado()
        viewModel.onSubir()
        return viewModel
    }

    private fun DigitalizarViewModel.assertErrorConArchivo(esperado: ErrorDigitalizar) {
        assertEquals(FaseDigitalizar.ERROR, uiState.value.fase)
        assertEquals(esperado, uiState.value.error)
        assertEquals(jpg, uiState.value.archivo)
        assertNull(uiState.value.gastoCreadoId)
    }

    @Test
    fun error413EsDemasiadoGrande() {
        fallaCon(ErrorAuth.ArchivoDemasiadoGrande("max")).assertErrorConArchivo(ErrorDigitalizar.DemasiadoGrande)
    }

    @Test
    fun error422EsNoSePudoLeer() {
        fallaCon(ErrorAuth.NoSePudoLeer("ocr")).assertErrorConArchivo(ErrorDigitalizar.NoSePudoLeer)
    }

    @Test
    fun error429LlevaLaEsperaYNoReintentaSolo() {
        val viewModel = fallaCon(ErrorAuth.DemasiadasPeticiones(30))

        viewModel.assertErrorConArchivo(ErrorDigitalizar.DemasiadasPeticiones(30))
        assertEquals(1, gastos.digitalizados.size)
    }

    @Test
    fun sinConexionEsSinConexion() {
        fallaCon(ErrorAuth.ServidorInaccesible).assertErrorConArchivo(ErrorDigitalizar.SinConexion)
    }

    @Test
    fun cualquierOtroErrorEsInesperado() {
        fallaCon(ErrorAuth.Inesperado).assertErrorConArchivo(ErrorDigitalizar.Inesperado)
        fallaCon(ErrorAuth.Validacion(mapOf("error" to "x"))).assertErrorConArchivo(ErrorDigitalizar.Inesperado)
    }

    @Test
    fun reintentarTrasUnErrorEnviaElMismoArchivoYPuedeTenerExito() {
        val viewModel = fallaCon(ErrorAuth.ServidorInaccesible)
        gastos.resultadoDigitalizar = Result.success(gastoVacio(8L))

        viewModel.onSubir()

        assertEquals(listOf(jpg, jpg), gastos.digitalizados)
        assertEquals(8L, viewModel.uiState.value.gastoCreadoId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun tambienSePuedeCambiarDeArchivoTrasUnError() {
        val viewModel = fallaCon(ErrorAuth.NoSePudoLeer(null))

        viewModel.conArchivoSeleccionado(png)

        assertEquals(png, viewModel.uiState.value.archivo)
        assertEquals(FaseDigitalizar.SELECCIONADO, viewModel.uiState.value.fase)
        assertNull(viewModel.uiState.value.error)
    }
}
