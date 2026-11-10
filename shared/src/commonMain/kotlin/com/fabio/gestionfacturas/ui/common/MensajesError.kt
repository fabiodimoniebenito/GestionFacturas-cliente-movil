package com.fabio.gestionfacturas.ui.common

import androidx.compose.runtime.Composable
import com.fabio.gestionfacturas.domain.model.ErrorAuth
import com.fabio.gestionfacturas.ui.gastos.ErrorDigitalizar
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.campo_error_email_invalido
import com.fabio.gestionfacturas.resources.campo_error_email_vacio
import com.fabio.gestionfacturas.resources.campo_error_nombre_vacio
import com.fabio.gestionfacturas.resources.campo_error_password_vacia
import com.fabio.gestionfacturas.resources.digitalizar_error_formato
import com.fabio.gestionfacturas.resources.digitalizar_error_grande
import com.fabio.gestionfacturas.resources.digitalizar_error_no_leer
import com.fabio.gestionfacturas.resources.digitalizar_error_pdf
import com.fabio.gestionfacturas.resources.digitalizar_error_vacio
import com.fabio.gestionfacturas.resources.error_archivo_demasiado_grande
import com.fabio.gestionfacturas.resources.error_conflicto
import com.fabio.gestionfacturas.resources.error_credenciales
import com.fabio.gestionfacturas.resources.error_demasiadas_peticiones
import com.fabio.gestionfacturas.resources.error_demasiadas_peticiones_espera
import com.fabio.gestionfacturas.resources.error_email_registrado
import com.fabio.gestionfacturas.resources.error_inesperado
import com.fabio.gestionfacturas.resources.error_no_encontrado
import com.fabio.gestionfacturas.resources.error_no_se_pudo_leer
import com.fabio.gestionfacturas.resources.error_sin_conexion
import com.fabio.gestionfacturas.resources.error_validacion
import org.jetbrains.compose.resources.stringResource

/** The single place that turns an auth error into the text the user reads. */
@Composable
fun ErrorAuth.mensaje(): String {
    return when (this) {
        ErrorAuth.Credenciales -> stringResource(Res.string.error_credenciales)
        is ErrorAuth.DemasiadasPeticiones -> {
            val espera = reintentarEnSegundos
            if (espera != null) {
                stringResource(Res.string.error_demasiadas_peticiones_espera, espera)
            } else {
                stringResource(Res.string.error_demasiadas_peticiones)
            }
        }
        is ErrorAuth.Validacion -> stringResource(Res.string.error_validacion)
        ErrorAuth.EmailYaRegistrado -> stringResource(Res.string.error_email_registrado)
        ErrorAuth.NoEncontrado -> stringResource(Res.string.error_no_encontrado)
        ErrorAuth.Conflicto -> stringResource(Res.string.error_conflicto)
        is ErrorAuth.NoEncontradoConDetalle -> stringResource(Res.string.error_no_encontrado)
        is ErrorAuth.ConflictoConDetalle -> stringResource(Res.string.error_conflicto)
        is ErrorAuth.ArchivoDemasiadoGrande -> stringResource(Res.string.error_archivo_demasiado_grande)
        is ErrorAuth.NoSePudoLeer -> stringResource(Res.string.error_no_se_pudo_leer)
        ErrorAuth.ServidorInaccesible -> stringResource(Res.string.error_sin_conexion)
        ErrorAuth.Inesperado -> stringResource(Res.string.error_inesperado)
    }
}

@Composable
fun ErrorCampo.mensaje(): String {
    return when (this) {
        ErrorCampo.EMAIL_VACIO -> stringResource(Res.string.campo_error_email_vacio)
        ErrorCampo.EMAIL_INVALIDO -> stringResource(Res.string.campo_error_email_invalido)
        ErrorCampo.NOMBRE_VACIO -> stringResource(Res.string.campo_error_nombre_vacio)
        ErrorCampo.PASSWORD_VACIA -> stringResource(Res.string.campo_error_password_vacia)
    }
}

/**
 * Message to show under [campo]: the local check first, then what the backend said about that field.
 */
@Composable
fun mensajeDeCampo(
    campo: CampoFormulario,
    erroresLocales: Map<CampoFormulario, ErrorCampo>,
    error: ErrorAuth?,
): String? {
    val local = erroresLocales[campo]
    if (local != null) {
        return local.mensaje()
    }
    if (error is ErrorAuth.Validacion) {
        return error.campos[campo.claveApi]
    }
    return null
}

/** A backend domain-rule message (`{"error": "..."}` on a 400), shown below the general message. */
fun ErrorAuth?.detalleDelServidor(): String? {
    if (this is ErrorAuth.Validacion) {
        return campos["error"]
    }
    return null
}

/** Texts for the digitalizar screen; rate limit, offline and unexpected reuse the common ones. */
@Composable
fun ErrorDigitalizar.mensaje(): String {
    return when (this) {
        ErrorDigitalizar.ArchivoVacio -> stringResource(Res.string.digitalizar_error_vacio)
        ErrorDigitalizar.FormatoNoSoportado -> stringResource(Res.string.digitalizar_error_formato)
        ErrorDigitalizar.EsPdf -> stringResource(Res.string.digitalizar_error_pdf)
        ErrorDigitalizar.DemasiadoGrande -> stringResource(Res.string.digitalizar_error_grande)
        ErrorDigitalizar.NoSePudoLeer -> stringResource(Res.string.digitalizar_error_no_leer)
        is ErrorDigitalizar.DemasiadasPeticiones ->
            ErrorAuth.DemasiadasPeticiones(reintentarEnSegundos).mensaje()
        ErrorDigitalizar.SinConexion -> ErrorAuth.ServidorInaccesible.mensaje()
        ErrorDigitalizar.Inesperado -> ErrorAuth.Inesperado.mensaje()
        is ErrorDigitalizar.Seleccion -> mensaje
    }
}
