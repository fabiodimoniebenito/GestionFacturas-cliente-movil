package com.fabio.gestionfacturas.ui.home

import com.fabio.gestionfacturas.domain.model.Sesion
import com.fabio.gestionfacturas.testutil.FakeAuthRepository
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repositorio = FakeAuthRepository()

    @BeforeTest
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun muestraElEmailDeLaSesionGuardada() {
        repositorio.sesion = Sesion(7L, "ana@example.com")

        val viewModel = HomeViewModel(repositorio)

        assertEquals("ana@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun cerrarSesionLlamaAlLogoutDelRepositorioYVuelveAEstarInactivo() {
        val puerta = CompletableDeferred<Unit>()
        repositorio.puerta = puerta
        val viewModel = HomeViewModel(repositorio)

        viewModel.onCerrarSesion()

        assertTrue(viewModel.uiState.value.cerrandoSesion)

        puerta.complete(Unit)

        assertFalse(viewModel.uiState.value.cerrandoSesion)
        assertEquals(1, repositorio.logouts)
    }

    @Test
    fun pulsarDosVecesCerrarSesionSoloLlamaUnaVez() {
        repositorio.puerta = CompletableDeferred()
        val viewModel = HomeViewModel(repositorio)

        viewModel.onCerrarSesion()
        viewModel.onCerrarSesion()

        assertEquals(1, repositorio.logouts)
    }
}
