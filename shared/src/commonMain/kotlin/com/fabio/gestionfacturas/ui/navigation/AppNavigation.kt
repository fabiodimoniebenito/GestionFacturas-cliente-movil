package com.fabio.gestionfacturas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import com.fabio.gestionfacturas.ui.login.LoginScreen
import com.fabio.gestionfacturas.ui.login.RegistroScreen
import org.koin.compose.koinInject

/**
 * App shell. Starts on the logged-in shell when a session exists, otherwise on Login, and returns
 * to Login (clearing the back stack) whenever the session ends: logout, or a failed token refresh.
 */
@Composable
fun AppNavigation(
    authRepository: AuthRepository = koinInject(),
) {
    val navController = rememberNavController()
    val sesionActiva by authRepository.sesionActiva.collectAsStateWithLifecycle()

    val destinoInicial: Any = remember {
        if (authRepository.sesionActiva.value) {
            RutaShell
        } else {
            RutaLogin
        }
    }

    LaunchedEffect(sesionActiva) {
        val destino = navController.currentDestination
        val enPantallaPublica = destino?.hasRoute<RutaLogin>() == true || destino?.hasRoute<RutaRegistro>() == true
        if (!sesionActiva && !enPantallaPublica) {
            navController.navigate(RutaLogin) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable<RutaLogin> {
            LoginScreen(
                alIniciarSesion = { navController.irAlShellLimpiandoPila() },
                alIrARegistro = { navController.navigate(RutaRegistro) },
            )
        }
        composable<RutaRegistro> {
            RegistroScreen(
                alRegistrarse = { navController.irAlShellLimpiandoPila() },
                alVolver = { navController.popBackStack() },
            )
        }
        composable<RutaShell> {
            ShellScreen()
        }
    }
}

private fun NavHostController.irAlShellLimpiandoPila() {
    navigate(RutaShell) {
        popUpTo(graph.id) { inclusive = true }
    }
}
