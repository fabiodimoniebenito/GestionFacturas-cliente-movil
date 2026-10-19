package com.fabio.gestionfacturas.ui.login

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.testutil.FakeAuthRepository
import com.fabio.gestionfacturas.ui.common.CampoFormulario
import com.fabio.gestionfacturas.ui.common.ErrorCampo
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
class RegistroViewModelTest {

    private val repositorio = FakeAuthRepository()
    private lateinit var viewModel: RegistroViewModel

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RegistroViewModel(repositorio)
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun rellenar(
        email: String = "ana@example.com",
        nombre: String = "Ana",
        password: String = "secreta",
    ) {
        viewModel.onEmailChange(email)
        viewModel.onNombreChange(nombre)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun conCamposVaciosMuestraErroresPorCampoYNoLlamaAlRepositorio() {
        viewModel.onRegistrar()

        val estado = viewModel.uiState.value
        assertEquals(ErrorCampo.EMAIL_VACIO, estado.erroresCampo[CampoFormulario.EMAIL])
        assertEquals(ErrorCampo.NOMBRE_VACIO, estado.erroresCampo[CampoFormulario.NOMBRE])
        assertEquals(ErrorCampo.PASSWORD_VACIA, estado.erroresCampo[CampoFormulario.PASSWORD])
        assertFalse(estado.cargando)
        assertTrue(repositorio.llamadasRegistro.isEmpty())
    }

    @Test
    fun unNombreSoloConEspaciosCuentaComoVacio() {
        rellenar(nombre = "   ")

        viewModel.onRegistrar()

        assertEquals(ErrorCampo.NOMBRE_VACIO, viewModel.uiState.value.erroresCampo[CampoFormulario.NOMBRE])
        assertTrue(repositorio.llamadasRegistro.isEmpty())
    }

    @Test
    fun mientrasRegistraEstaCargandoYAlTerminarSeIndicaElExito() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puerta = puerta
        rellenar()

        viewModel.onRegistrar()

        assertTrue(viewModel.uiState.value.cargando)
        assertFalse(viewModel.uiState.value.registroExitoso)

        puerta.complete(Unit)

        val estado = viewModel.uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.registroExitoso)
        assertNull(estado.error)
    }

    @Test
    fun enviaEmailYNombreSinEspaciosYLaContrasenaTalCual() {
        rellenar(email = " ana@example.com ", nombre = " Ana ", password = " p ")

        viewModel.onRegistrar()

        assertEquals(listOf(Triple("ana@example.com", "Ana", " p ")), repositorio.llamadasRegistro)
    }

    @Test
    fun unSegundoEnvioMientrasCargaNoLlamaOtraVezAlRepositorio() {
        repositorio.puerta = CompletableDeferred()
        rellenar()

        viewModel.onRegistrar()
        viewModel.onRegistrar()

        assertEquals(1, repositorio.llamadasRegistro.size)
    }

    @Test
    fun elEfectoDeNavegacionSeLimpiaAlConsumirlo() {
        rellenar()
        viewModel.onRegistrar()
        assertTrue(viewModel.uiState.value.registroExitoso)

        viewModel.onNavegacionConsumida()

        assertFalse(viewModel.uiState.value.registroExitoso)
    }

    @Test
    fun cadaTipoDeErrorSeExponeTalCualEnElEstado() {
        val errores = listOf(
            ErrorAuth.Credenciales,
            ErrorAuth.DemasiadasPeticiones(3600),
            ErrorAuth.Validacion(mapOf("email" to "no válido", "nombre" to "obligatorio")),
            ErrorAuth.EmailYaRegistrado,
            ErrorAuth.ServidorInaccesible,
            ErrorAuth.Inesperado,
        )

        for (error in errores) {
            repositorio.resultadoRegistro = Result.failure(error)
            rellenar()

            viewModel.onRegistrar()

            val estado = viewModel.uiState.value
            assertEquals(error, estado.error, "error expuesto para $error")
            assertFalse(estado.cargando, "no debe quedarse cargando con $error")
            assertFalse(estado.registroExitoso, "no debe navegar con $error")
        }
    }

    @Test
    fun editarUnCampoQuitaElErrorDelServidor() {
        repositorio.resultadoRegistro = Result.failure(ErrorAuth.EmailYaRegistrado)
        rellenar()
        viewModel.onRegistrar()
        assertEquals(ErrorAuth.EmailYaRegistrado, viewModel.uiState.value.error)

        viewModel.onEmailChange("otra@example.com")

        assertNull(viewModel.uiState.value.error)
    }
}
