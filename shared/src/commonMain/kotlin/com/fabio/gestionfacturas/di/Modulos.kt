package com.fabio.gestionfacturas.di

import com.fabio.gestionfacturas.data.local.EstadoSesion
import com.fabio.gestionfacturas.data.local.SettingsTokenStorage
import com.fabio.gestionfacturas.data.local.TokenStorage
import com.fabio.gestionfacturas.data.remote.ApiClient
import com.fabio.gestionfacturas.data.remote.urlBaseApi
import com.fabio.gestionfacturas.data.repository.AuthRepositoryImpl
import com.fabio.gestionfacturas.data.repository.CategoriaRepositoryImpl
import com.fabio.gestionfacturas.data.repository.GastoRepositoryImpl
import com.fabio.gestionfacturas.domain.repository.CategoriaRepository
import com.fabio.gestionfacturas.domain.repository.GastoRepository
import com.fabio.gestionfacturas.data.repository.ClienteRepositoryImpl
import com.fabio.gestionfacturas.domain.repository.AuthRepository
import com.fabio.gestionfacturas.domain.repository.ClienteRepository
import com.fabio.gestionfacturas.ui.clientes.ClienteFormViewModel
import com.fabio.gestionfacturas.ui.clientes.ClientesViewModel
import com.fabio.gestionfacturas.ui.gastos.GastosViewModel
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
    single<GastoRepository> { GastoRepositoryImpl(api = get<ApiClient>().gastoApi) }
    single<CategoriaRepository> { CategoriaRepositoryImpl(api = get<ApiClient>().categoriaApi) }
}

val moduloUi = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegistroViewModel(get()) }
    viewModel { ShellViewModel(get()) }
    viewModel { ClientesViewModel(get()) }
    viewModel { GastosViewModel(get()) }
    viewModel { parametros -> ClienteFormViewModel(parametros.getOrNull<Long>(), get()) }
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
