package com.fabio.gestionfacturas.ui.clientes

import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.NuevoCliente
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
class ClienteFormViewModelTest {

    private val repositorio = FakeClienteRepository()

    private val ana = Cliente(5L, "Ana", "A1234567B", "ana@example.com", "600111222", true)

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crear() = ClienteFormViewModel(null, repositorio)

    private fun editar(id: Long = 5L) = ClienteFormViewModel(id, repositorio)

    private fun ClienteFormViewModel.rellenarCreacion(
        nombre: String = "Ana",
        nif: String = "A1234567B",
        email: String = "",
        telefono: String = "",
    ) {
        onNombreChange(nombre)
        onNifChange(nif)
        onEmailChange(email)
        onTelefonoChange(telefono)
    }

    // ---- create ----

    @Test
    fun crearEmpiezaListoYSinCargar() {
        val viewModel = crear()

        val estado = viewModel.uiState.value
        assertEquals(CargaFormCliente.Lista, estado.carga)
        assertFalse(estado.esEdicion)
        assertTrue(repositorio.obtenidos.isEmpty())
    }

    @Test
    fun crearConExitoEnviaLosDatosYTerminaGuardado() {
        repositorio.resultadoCrear = Result.success(ana)
        val viewModel = crear()
        viewModel.rellenarCreacion(email = "ana@example.com", telefono = "600111222")

        viewModel.onGuardar()

        assertEquals(
            listOf(NuevoCliente("Ana", "A1234567B", "ana@example.com", "600111222")),
            repositorio.creados,
        )
        assertEquals(ResultadoFormulario.GUARDADO, viewModel.uiState.value.salida)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun crearRecortaLosEspaciosYConvierteLosOpcionalesVaciosEnNulos() {
        repositorio.resultadoCrear = Result.success(ana)
        val viewModel = crear()
        viewModel.rellenarCreacion(nombre = "  Ana  ", nif = " A1234567B ", email = "   ", telefono = "")

        viewModel.onGuardar()

        assertEquals(listOf(NuevoCliente("Ana", "A1234567B", null, null)), repositorio.creados)
    }

    @Test
    fun crearSinNombreNiNifMarcaLosDosCamposYNoLlamaAlRepositorio() {
        val viewModel = crear()

        viewModel.onGuardar()

        assertEquals(
            mapOf(
                CampoCliente.NOMBRE to ErrorCampoCliente.NOMBRE_VACIO,
                CampoCliente.NIF to ErrorCampoCliente.NIF_VACIO,
            ),
            viewModel.uiState.value.erroresLocales,
        )
        assertTrue(repositorio.creados.isEmpty())
    }

    @Test
    fun unNombreSoloConEspaciosCuentaComoVacio() {
        val viewModel = crear()
        viewModel.rellenarCreacion(nombre = "   ")

        viewModel.onGuardar()

        assertEquals(ErrorCampoCliente.NOMBRE_VACIO, viewModel.uiState.value.erroresLocales[CampoCliente.NOMBRE])
        assertTrue(repositorio.creados.isEmpty())
    }

    @Test
    fun editarUnCampoLimpiaSuErrorLocalYNoElDeLosDemas() {
        val viewModel = crear()
        viewModel.onGuardar()

        viewModel.onNombreChange("Ana")

        assertEquals(
            mapOf(CampoCliente.NIF to ErrorCampoCliente.NIF_VACIO),
            viewModel.uiState.value.erroresLocales,
        )
    }

    @Test
    fun mientrasGuardaMarcaGuardandoYPulsarDosVecesSoloLlamaUnaVez() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaGuardar = puerta
        repositorio.resultadoCrear = Result.success(ana)
        val viewModel = crear()
        viewModel.rellenarCreacion()

        viewModel.onGuardar()
        viewModel.onGuardar()

        assertTrue(viewModel.uiState.value.guardando)
        assertEquals(1, repositorio.creados.size)

        puerta.complete(Unit)

        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun unaRespuesta400ExponeElMapaDeCamposTalCual() {
        val validacion = ErrorAuth.Validacion(mapOf("email" to "formato no válido", "nif" to "NIF no válido"))
        repositorio.resultadoCrear = Result.failure(validacion)
        val viewModel = crear()
        viewModel.rellenarCreacion(email = "x")

        viewModel.onGuardar()

        val estado = viewModel.uiState.value
        assertEquals(validacion, estado.error)
        assertNull(estado.salida)
        assertFalse(estado.guardando)
    }

    @Test
    fun unaRespuesta400ConSoloElMensajeDeDominioSeConservaBajoLaClaveError() {
        val validacion = ErrorAuth.Validacion(mapOf("error" to "regla de negocio"))
        repositorio.resultadoCrear = Result.failure(validacion)
        val viewModel = crear()
        viewModel.rellenarCreacion()

        viewModel.onGuardar()

        assertEquals(validacion, viewModel.uiState.value.error)
    }

    @Test
    fun unConflicto409AlCrearSeExponeComoConflicto() {
        repositorio.resultadoCrear = Result.failure(ErrorAuth.Conflicto)
        val viewModel = crear()
        viewModel.rellenarCreacion()

        viewModel.onGuardar()

        val estado = viewModel.uiState.value
        assertEquals(ErrorAuth.Conflicto, estado.error)
        assertNull(estado.salida)
    }

    @Test
    fun editarUnCampoTrasUnErrorLimpiaElErrorGeneral() {
        repositorio.resultadoCrear = Result.failure(ErrorAuth.Conflicto)
        val viewModel = crear()
        viewModel.rellenarCreacion()
        viewModel.onGuardar()

        viewModel.onNifChange("OTRO123")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun unFalloQueNoEsErrorAuthAlGuardarSeExponeComoInesperado() {
        repositorio.resultadoCrear = Result.failure(IllegalStateException("boom"))
        val viewModel = crear()
        viewModel.rellenarCreacion()

        viewModel.onGuardar()

        assertEquals(ErrorAuth.Inesperado, viewModel.uiState.value.error)
    }

    @Test
    fun laSalidaSeLimpiaAlConsumirla() {
        repositorio.resultadoCrear = Result.success(ana)
        val viewModel = crear()
        viewModel.rellenarCreacion()
        viewModel.onGuardar()

        viewModel.onSalidaConsumida()

        assertNull(viewModel.uiState.value.salida)
    }

    // ---- edit ----

    @Test
    fun editarCargaElClienteYRellenaLosCampos() {
        repositorio.resultadoObtener = Result.success(ana)

        val viewModel = editar()

        val estado = viewModel.uiState.value
        assertEquals(listOf(5L), repositorio.obtenidos)
        assertEquals(CargaFormCliente.Lista, estado.carga)
        assertTrue(estado.esEdicion)
        assertEquals("Ana", estado.nombre)
        assertEquals("A1234567B", estado.nif)
        assertEquals("ana@example.com", estado.email)
        assertEquals("600111222", estado.telefono)
    }

    @Test
    fun editarMuestraCargandoMientrasLlegaElCliente() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puertaObtener = puerta
        repositorio.resultadoObtener = Result.success(ana)

        val viewModel = editar()

        assertEquals(CargaFormCliente.Cargando, viewModel.uiState.value.carga)

        puerta.complete(Unit)

        assertEquals(CargaFormCliente.Lista, viewModel.uiState.value.carga)
    }

    @Test
    fun editarClienteSinEmailNiTelefonoDejaEsosCamposVacios() {
        repositorio.resultadoObtener = Result.success(ana.copy(email = null, telefono = null))

        val viewModel = editar()

        assertEquals("", viewModel.uiState.value.email)
        assertEquals("", viewModel.uiState.value.telefono)
    }

    @Test
    fun guardarEnEdicionEnviaLosDatosSinElNif() {
        repositorio.resultadoObtener = Result.success(ana)
        repositorio.resultadoEditar = Result.success(ana)
        val viewModel = editar()
        viewModel.onNombreChange("Ana María")
        viewModel.onEmailChange("")

        viewModel.onGuardar()

        assertEquals(
            listOf(5L to DatosCliente("Ana María", null, "600111222")),
            repositorio.editados,
        )
        assertTrue(repositorio.creados.isEmpty())
        assertEquals(ResultadoFormulario.GUARDADO, viewModel.uiState.value.salida)
    }

    @Test
    fun enEdicionElNifNoEsObligatorio() {
        repositorio.resultadoObtener = Result.success(ana.copy(nif = ""))
        repositorio.resultadoEditar = Result.success(ana)
        val viewModel = editar()

        viewModel.onGuardar()

        assertTrue(viewModel.uiState.value.erroresLocales.isEmpty())
        assertEquals(1, repositorio.editados.size)
    }

    @Test
    fun enEdicionUnNombreVacioNoLlamaAlRepositorio() {
        repositorio.resultadoObtener = Result.success(ana)
        val viewModel = editar()
        viewModel.onNombreChange("")

        viewModel.onGuardar()

        assertEquals(ErrorCampoCliente.NOMBRE_VACIO, viewModel.uiState.value.erroresLocales[CampoCliente.NOMBRE])
        assertTrue(repositorio.editados.isEmpty())
    }

    @Test
    fun enEdicionUnaRespuesta400ExponeElMapaDeCampos() {
        val validacion = ErrorAuth.Validacion(mapOf("telefono" to "demasiado largo"))
        repositorio.resultadoObtener = Result.success(ana)
        repositorio.resultadoEditar = Result.failure(validacion)
        val viewModel = editar()

        viewModel.onGuardar()

        assertEquals(validacion, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.salida)
    }

    @Test
    fun unClienteQueNoExisteAlCargarTerminaConNoEncontrado() {
        repositorio.resultadoObtener = Result.failure(ErrorAuth.NoEncontrado)

        val viewModel = editar()

        assertEquals(ResultadoFormulario.NO_ENCONTRADO, viewModel.uiState.value.salida)
    }

    @Test
    fun unClienteQueDejaDeExistirAlGuardarTerminaConNoEncontrado() {
        repositorio.resultadoObtener = Result.success(ana)
        repositorio.resultadoEditar = Result.failure(ErrorAuth.NoEncontrado)
        val viewModel = editar()

        viewModel.onGuardar()

        val estado = viewModel.uiState.value
        assertEquals(ResultadoFormulario.NO_ENCONTRADO, estado.salida)
        assertNull(estado.error)
        assertFalse(estado.guardando)
    }

    @Test
    fun otroErrorAlCargarMuestraElErrorYReintentarLoRecupera() {
        repositorio.resultadoObtener = Result.failure(ErrorAuth.ServidorInaccesible)
        val viewModel = editar()

        assertEquals(CargaFormCliente.Error(ErrorAuth.ServidorInaccesible), viewModel.uiState.value.carga)

        repositorio.resultadoObtener = Result.success(ana)
        viewModel.onReintentarCarga()

        assertEquals(CargaFormCliente.Lista, viewModel.uiState.value.carga)
        assertEquals("Ana", viewModel.uiState.value.nombre)
        assertEquals(2, repositorio.obtenidos.size)
    }

    @Test
    fun noSePuedeGuardarMientrasElClienteSigueCargando() {
        repositorio.puertaObtener = CompletableDeferred()
        val viewModel = editar()

        viewModel.onGuardar()

        assertTrue(repositorio.editados.isEmpty())
    }
}
