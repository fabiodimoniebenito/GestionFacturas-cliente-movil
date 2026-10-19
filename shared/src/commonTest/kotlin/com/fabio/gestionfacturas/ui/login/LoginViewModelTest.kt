package com.fabio.gestionfacturas.ui.login

import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.domain.model.Sesion
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
class LoginViewModelTest {

    private val repositorio = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(repositorio)
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun rellenar(email: String = "ana@example.com", password: String = "secreta") {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun elEstadoInicialEstaVacioYSinCargar() {
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    @Test
    fun escribirActualizaElEstado() {
        rellenar("ana@example.com", "secreta")

        assertEquals("ana@example.com", viewModel.uiState.value.email)
        assertEquals("secreta", viewModel.uiState.value.password)
    }

    @Test
    fun conCamposVaciosMuestraErroresPorCampoYNoLlamaAlRepositorio() {
        viewModel.onIniciarSesion()

        val estado = viewModel.uiState.value
        assertEquals(ErrorCampo.EMAIL_VACIO, estado.erroresCampo[CampoFormulario.EMAIL])
        assertEquals(ErrorCampo.PASSWORD_VACIA, estado.erroresCampo[CampoFormulario.PASSWORD])
        assertFalse(estado.cargando)
        assertTrue(repositorio.llamadasLogin.isEmpty())
    }

    @Test
    fun conUnEmailMalFormadoMuestraEmailInvalido() {
        rellenar(email = "no-es-un-correo")

        viewModel.onIniciarSesion()

        assertEquals(ErrorCampo.EMAIL_INVALIDO, viewModel.uiState.value.erroresCampo[CampoFormulario.EMAIL])
        assertTrue(repositorio.llamadasLogin.isEmpty())
    }

    @Test
    fun editarUnCampoQuitaSuErrorLocal() {
        viewModel.onIniciarSesion()
        assertEquals(2, viewModel.uiState.value.erroresCampo.size)

        viewModel.onEmailChange("a")

        assertNull(viewModel.uiState.value.erroresCampo[CampoFormulario.EMAIL])
        assertEquals(ErrorCampo.PASSWORD_VACIA, viewModel.uiState.value.erroresCampo[CampoFormulario.PASSWORD])
    }

    @Test
    fun mientrasLlamaAlRepositorioEstaCargandoYAlTerminarSeIndicaElExito() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puerta = puerta
        rellenar()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.uiState.value.cargando)
        assertFalse(viewModel.uiState.value.loginExitoso)

        puerta.complete(Unit)

        val estado = viewModel.uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.loginExitoso)
        assertNull(estado.error)
    }

    @Test
    fun enviaElEmailSinEspaciosYLaContrasenaTalCual() {
        rellenar(email = "  ana@example.com ", password = " con espacios ")

        viewModel.onIniciarSesion()

        assertEquals(listOf("ana@example.com" to " con espacios "), repositorio.llamadasLogin)
    }

    @Test
    fun unSegundoEnvioMientrasCargaNoLlamaOtraVezAlRepositorio() {
        repositorio.puerta = CompletableDeferred()
        rellenar()

        viewModel.onIniciarSesion()
        viewModel.onIniciarSesion()

        assertEquals(1, repositorio.llamadasLogin.size)
    }

    @Test
    fun elEfectoDeNavegacionSeLimpiaAlConsumirlo() {
        rellenar()
        viewModel.onIniciarSesion()
        assertTrue(viewModel.uiState.value.loginExitoso)

        viewModel.onNavegacionConsumida()

        assertFalse(viewModel.uiState.value.loginExitoso)
    }

    @Test
    fun cadaTipoDeErrorSeExponeTalCualEnElEstado() {
        val errores = listOf(
            ErrorAuth.Credenciales,
            ErrorAuth.DemasiadasPeticiones(30),
            ErrorAuth.DemasiadasPeticiones(null),
            ErrorAuth.Validacion(mapOf("email" to "no válido")),
            ErrorAuth.EmailYaRegistrado,
            ErrorAuth.ServidorInaccesible,
            ErrorAuth.Inesperado,
        )

        for (error in errores) {
            repositorio.resultadoLogin = Result.failure(error)
            rellenar()

            viewModel.onIniciarSesion()

            val estado = viewModel.uiState.value
            assertEquals(error, estado.error, "error expuesto para $error")
            assertFalse(estado.cargando, "no debe quedarse cargando con $error")
            assertFalse(estado.loginExitoso, "no debe navegar con $error")
        }
    }

    @Test
    fun unFalloQueNoEsErrorAuthSeMuestraComoInesperado() {
        repositorio.resultadoLogin = Result.failure(IllegalStateException("boom"))
        rellenar()

        viewModel.onIniciarSesion()

        assertEquals(ErrorAuth.Inesperado, viewModel.uiState.value.error)
    }

    @Test
    fun editarUnCampoTrasUnErrorLoQuitaYPermiteReintentar() {
        repositorio.resultadoLogin = Result.failure(ErrorAuth.Credenciales)
        rellenar()
        viewModel.onIniciarSesion()
        assertEquals(ErrorAuth.Credenciales, viewModel.uiState.value.error)

        viewModel.onPasswordChange("otra")
        assertNull(viewModel.uiState.value.error)

        repositorio.resultadoLogin = Result.success(Sesion(1L, "ana@example.com"))
        viewModel.onIniciarSesion()

        assertTrue(viewModel.uiState.value.loginExitoso)
        assertEquals(2, repositorio.llamadasLogin.size)
    }
}
