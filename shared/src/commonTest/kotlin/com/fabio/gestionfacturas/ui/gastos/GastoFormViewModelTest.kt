package com.fabio.gestionfacturas.ui.gastos

import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Gasto
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.testutil.FakeCategoriaRepository
import com.fabio.gestionfacturas.testutil.FakeClienteRepository
import com.fabio.gestionfacturas.testutil.FakeGastoRepository
import com.fabio.gestionfacturas.ui.clientes.ResultadoFormulario
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
class GastoFormViewModelTest {

    private val gastos = FakeGastoRepository()
    private val clientes = FakeClienteRepository()
    private val categorias = FakeCategoriaRepository()

    private val ana = Cliente(1L, "Ana", "A1", null, null, true)
    private val vieja = Cliente(2L, "Vieja SL", "B2", null, null, false)
    private val material = Categoria(1L, "Material", true)
    private val existente = Gasto(
        id = 5L,
        usuarioId = 7L,
        categoriaId = 1L,
        clienteId = 1L,
        emisor = "Papeleria",
        fechaEmision = LocalDate(2026, 10, 20),
        baseImponible = Importe.de("100.00"),
        iva = Importe.de("21.00"),
        moneda = "USD",
        deducible = true,
        estado = EstadoGasto.BORRADOR,
        creadoEn = null,
    )

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        clientes.resultadoListar = Result.success(listOf(ana, vieja))
        categorias.resultadoListar = Result.success(listOf(material))
        gastos.resultadoCrear = Result.success(existente)
        gastos.resultadoEditar = Result.success(existente)
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crear() = GastoFormViewModel(null, gastos, clientes, categorias)

    private fun editar() = GastoFormViewModel(5L, gastos, clientes, categorias)

    // ---- create ------------------------------------------------------------------------------

    @Test
    fun crearEmpiezaListoConLaMonedaPorDefectoYLosSelectoresCargados() {
        val estado = crear().uiState.value

        assertEquals(CargaFormGasto.Lista, estado.carga)
        assertFalse(estado.esEdicion)
        assertEquals("EUR", estado.moneda)
        assertEquals(listOf(ana), estado.clientes)
        assertEquals(listOf(material), estado.categorias)
        assertFalse(estado.selectoresFallidos)
    }

    @Test
    fun crearEnviaLosDatosNormalizadosYTermina() {
        val viewModel = crear()
        viewModel.onTotalChange("121,50")
        viewModel.onEmisorChange("  Papeleria ")
        viewModel.onFechaEmisionChange("2026-10-20")
        viewModel.onBaseImponibleChange("100,5")
        viewModel.onIvaChange("21")
        viewModel.onClienteChange(1L)

        viewModel.onGuardar()

        val enviado = gastos.creados.single()
        assertEquals("121.50", enviado.total.valor)
        assertEquals("Papeleria", enviado.emisor)
        assertEquals(LocalDate(2026, 10, 20), enviado.fechaEmision)
        assertEquals("100.5", enviado.baseImponible?.valor)
        assertEquals("21", enviado.iva?.valor)
        assertEquals("EUR", enviado.moneda)
        assertEquals(1L, enviado.clienteId)
        assertEquals(ResultadoFormulario.GUARDADO, viewModel.uiState.value.salida)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun losCamposVaciosSeEnvianComoNulos() {
        val viewModel = crear()
        viewModel.onTotalChange("10")

        viewModel.onGuardar()

        val enviado = gastos.creados.single()
        assertNull(enviado.emisor)
        assertNull(enviado.fechaEmision)
        assertNull(enviado.baseImponible)
        assertNull(enviado.iva)
        assertNull(enviado.clienteId)
    }

    // ---- local validation --------------------------------------------------------------------

    @Test
    fun sinTotalNoSeLlamaAlBackend() {
        val viewModel = crear()

        viewModel.onGuardar()

        assertEquals(mapOf(CampoGasto.TOTAL to ErrorCampoGasto.TOTAL_VACIO), viewModel.uiState.value.erroresLocales)
        assertTrue(gastos.creados.isEmpty())
    }

    @Test
    fun importesYFechaInvalidosSeMarcanPorCampo() {
        val viewModel = crear()
        viewModel.onTotalChange("1.234")
        viewModel.onBaseImponibleChange("abc")
        viewModel.onIvaChange("1,2,3")
        viewModel.onFechaEmisionChange("20/10/2026")

        viewModel.onGuardar()

        assertEquals(
            mapOf(
                CampoGasto.TOTAL to ErrorCampoGasto.IMPORTE_INVALIDO,
                CampoGasto.BASE_IMPONIBLE to ErrorCampoGasto.IMPORTE_INVALIDO,
                CampoGasto.IVA to ErrorCampoGasto.IMPORTE_INVALIDO,
                CampoGasto.FECHA_EMISION to ErrorCampoGasto.FECHA_INVALIDA,
            ),
            viewModel.uiState.value.erroresLocales,
        )
        assertTrue(gastos.creados.isEmpty())
    }

    @Test
    fun unaFechaImposibleEsInvalida() {
        val viewModel = crear()
        viewModel.onTotalChange("1")
        viewModel.onFechaEmisionChange("2026-02-30")

        viewModel.onGuardar()

        assertEquals(ErrorCampoGasto.FECHA_INVALIDA, viewModel.uiState.value.erroresLocales[CampoGasto.FECHA_EMISION])
    }

    @Test
    fun editarUnCampoBorraSuErrorLocal() {
        val viewModel = crear()
        viewModel.onGuardar()

        viewModel.onTotalChange("5")

        assertTrue(viewModel.uiState.value.erroresLocales.isEmpty())
    }

    // ---- backend errors on save --------------------------------------------------------------

    @Test
    fun un400DejaElMapaDeCamposParaLaPantalla() {
        val campos = mapOf("total" to "debe ser mayor o igual que 0", "iva" to "no válido")
        gastos.resultadoCrear = Result.failure(ErrorAuth.Validacion(campos))
        val viewModel = crear()
        viewModel.onTotalChange("1")

        viewModel.onGuardar()

        assertEquals(ErrorAuth.Validacion(campos), viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.salida)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun un404AlGuardarConservaElMensajeYOfreceRecargarLosSelectores() {
        val error = ErrorAuth.NoEncontradoConDetalle("Categoría no encontrada")
        gastos.resultadoEditar = Result.failure(error)
        gastos.resultadoObtener = Result.success(existente)
        val viewModel = editar()
        viewModel.onTotalChange("10")

        viewModel.onGuardar()

        assertEquals(error, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.selectoresFallidos)
        assertNull(viewModel.uiState.value.salida)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun recargarLosSelectoresLimpiaElAvisoYActualizaLasListas() {
        gastos.resultadoObtener = Result.success(existente)
        gastos.resultadoEditar = Result.failure(ErrorAuth.NoEncontradoConDetalle("Cliente no encontrado"))
        val viewModel = editar()
        viewModel.onTotalChange("10")
        viewModel.onGuardar()
        clientes.resultadoListar = Result.success(emptyList())

        viewModel.onRecargarSelectores()

        assertFalse(viewModel.uiState.value.selectoresFallidos)
        assertTrue(viewModel.uiState.value.clientes.isEmpty())
    }

    @Test
    fun un409AlGuardarSeExpone() {
        gastos.resultadoObtener = Result.success(existente)
        gastos.resultadoEditar = Result.failure(ErrorAuth.ConflictoConDetalle("El gasto ya está revisado"))
        val viewModel = editar()
        viewModel.onTotalChange("10")

        viewModel.onGuardar()

        assertEquals(ErrorAuth.ConflictoConDetalle("El gasto ya está revisado"), viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.salida)
    }

    @Test
    fun unSegundoGuardarMientrasSeGuardaSeIgnora() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaGuardar = puerta
        val viewModel = crear()
        viewModel.onTotalChange("10")

        viewModel.onGuardar()
        assertTrue(viewModel.uiState.value.guardando)
        viewModel.onGuardar()

        assertEquals(1, gastos.creados.size)
        puerta.complete(Unit)
        assertEquals(ResultadoFormulario.GUARDADO, viewModel.uiState.value.salida)
    }

    @Test
    fun consumirLaSalidaLaBorra() {
        val viewModel = crear()
        viewModel.onTotalChange("10")
        viewModel.onGuardar()

        viewModel.onSalidaConsumida()

        assertNull(viewModel.uiState.value.salida)
    }

    // ---- edit --------------------------------------------------------------------------------

    @Test
    fun editarCargaElGastoYRellenaLosCamposSalvoElTotal() {
        val puerta = CompletableDeferred<Unit>()
        gastos.puertaObtener = puerta
        gastos.resultadoObtener = Result.success(existente)

        val viewModel = editar()
        assertEquals(CargaFormGasto.Cargando, viewModel.uiState.value.carga)

        puerta.complete(Unit)
        val estado = viewModel.uiState.value
        assertEquals(listOf(5L), gastos.obtenidos)
        assertEquals(CargaFormGasto.Lista, estado.carga)
        assertTrue(estado.esEdicion)
        assertEquals("Papeleria", estado.emisor)
        assertEquals("2026-10-20", estado.fechaEmision)
        assertEquals("100.00", estado.baseImponible)
        assertEquals("21.00", estado.iva)
        assertEquals("", estado.total)
        assertEquals("USD", estado.moneda)
        assertTrue(estado.deducible)
        assertEquals(1L, estado.clienteId)
        assertEquals(1L, estado.categoriaId)
    }

    @Test
    fun editarConCamposNulosUsaLosValoresNeutros() {
        gastos.resultadoObtener = Result.success(
            existente.copy(
                emisor = null, fechaEmision = null, baseImponible = null, iva = null,
                moneda = null, deducible = null, clienteId = null, categoriaId = null,
            ),
        )

        val estado = editar().uiState.value

        assertEquals(CargaFormGasto.Lista, estado.carga)
        assertEquals("", estado.emisor)
        assertEquals("", estado.fechaEmision)
        assertEquals("EUR", estado.moneda)
        assertFalse(estado.deducible)
        assertNull(estado.clienteId)
    }

    @Test
    fun guardarUnaEdicionEnviaCategoriaYDeducible() {
        gastos.resultadoObtener = Result.success(existente)
        val viewModel = editar()
        viewModel.onTotalChange("121,00")
        viewModel.onCategoriaChange(null)
        viewModel.onDeducibleChange(false)

        viewModel.onGuardar()

        val (id, datos) = gastos.editados.single()
        assertEquals(5L, id)
        assertEquals("121.00", datos.total.valor)
        assertNull(datos.categoriaId)
        assertEquals(false, datos.deducible)
        assertEquals(1L, datos.clienteId)
        assertEquals(ResultadoFormulario.GUARDADO, viewModel.uiState.value.salida)
    }

    @Test
    fun un404AlCargarVuelveALaListaConNoEncontrado() {
        for (error in listOf(ErrorAuth.NoEncontrado, ErrorAuth.NoEncontradoConDetalle("Gasto no encontrado"))) {
            gastos.resultadoObtener = Result.failure(error)

            assertEquals(ResultadoFormulario.NO_ENCONTRADO, editar().uiState.value.salida)
        }
    }

    @Test
    fun otroErrorAlCargarPermiteReintentar() {
        gastos.resultadoObtener = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = editar()
        assertEquals(CargaFormGasto.Error(ErrorAuth.ServidorInaccesible), viewModel.uiState.value.carga)

        gastos.resultadoObtener = Result.success(existente)
        viewModel.onReintentarCarga()

        assertEquals(CargaFormGasto.Lista, viewModel.uiState.value.carga)
        assertEquals(listOf(5L, 5L), gastos.obtenidos)
    }

    // ---- selectors ---------------------------------------------------------------------------

    @Test
    fun siLosSelectoresFallanElFormularioSigueFuncionandoYOfreceRecargar() {
        clientes.resultadoListar = Result.failure(ErrorAuth.ServidorInaccesible)

        val viewModel = crear()

        assertEquals(CargaFormGasto.Lista, viewModel.uiState.value.carga)
        assertTrue(viewModel.uiState.value.selectoresFallidos)

        clientes.resultadoListar = Result.success(listOf(ana))
        viewModel.onRecargarSelectores()

        assertFalse(viewModel.uiState.value.selectoresFallidos)
        assertEquals(listOf(ana), viewModel.uiState.value.clientes)
    }
}
