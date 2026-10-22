package com.fabio.gestionfacturas.di

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.local.SettingsTokenStorage
import com.fabio.gestionfacturas.data.local.TokenStorage
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.data.remote.urlBaseApi
import com.fabio.gestionfacturas.data.repository.AuthRepositoryImpl
import com.fabio.gestionfacturas.data.repository.ClienteRepositoryImpl
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.ui.clientes.ClientesViewModel
import com.fabio.gestionfacturas.ui.login.LoginViewModel
import com.fabio.gestionfacturas.ui.login.RegistroViewModel
import com.fabio.gestionfacturas.ui.navigation.ShellViewModel
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val moduloDatos = module {
    single<Settings> { Settings() }
    single<TokenStorage> { SettingsTokenStorage(get()) }
    // A stored refresh token means there is a session to resume.
    single { EstadoSesion(get<TokenStorage>().refreshToken() != null) }
    single { ApiClient(baseUrl = urlBaseApi(), tokenStorage = get(), estadoSesion = get()) }
    single<AuthRepository> { AuthRepositoryImpl(apiClient = get(), tokenStorage = get(), estadoSesion = get()) }
    single<ClienteRepository> { ClienteRepositoryImpl(api = get<ApiClient>().clienteApi) }
}

val moduloUi = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegistroViewModel(get()) }
    viewModel { ShellViewModel(get()) }
    viewModel { ClientesViewModel(get()) }
}

/** Starts Koin once; calling it again (e.g. Android activity recreation) is a no-op. */
fun initKoin() {
    if (KoinPlatform.getKoinOrNull() != null) {
        return
    }
    startKoin {
        modules(moduloDatos, moduloUi)
    }
}
