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
import com.fabio.gestionfacturas.ui.home.HomeScreen
import com.fabio.gestionfacturas.ui.login.LoginScreen
import com.fabio.gestionfacturas.ui.login.RegistroScreen
import org.koin.compose.koinInject

/**
 * App shell. Starts on Home when a session exists, otherwise on Login, and returns to Login
 * (clearing the back stack) whenever the session ends: logout, or a failed token refresh.
 */
@Composable
fun AppNavigation(
    authRepository: AuthRepository = koinInject(),
) {
    val navController = rememberNavController()
    val sesionActiva by authRepository.sesionActiva.collectAsStateWithLifecycle()

    val destinoInicial: Any = remember {
        if (authRepository.sesionActiva.value) {
            RutaHome
        } else {
            RutaLogin
        }
    }

    LaunchedEffect(sesionActiva) {
        val enHome = navController.currentDestination?.hasRoute<RutaHome>() == true
        if (!sesionActiva && enHome) {
            navController.navigate(RutaLogin) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable<RutaLogin> {
            LoginScreen(
                alIniciarSesion = { navController.irAHomeLimpiandoPila() },
                alIrARegistro = { navController.navigate(RutaRegistro) },
            )
        }
        composable<RutaRegistro> {
            RegistroScreen(
                alRegistrarse = { navController.irAHomeLimpiandoPila() },
                alVolver = { navController.popBackStack() },
            )
        }
        composable<RutaHome> {
            HomeScreen()
        }
    }
}

private fun NavHostController.irAHomeLimpiandoPila() {
    navigate(RutaHome) {
        popUpTo(graph.id) { inclusive = true }
    }
}
