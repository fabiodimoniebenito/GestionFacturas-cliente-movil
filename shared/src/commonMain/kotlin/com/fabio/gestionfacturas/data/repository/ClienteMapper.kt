package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.dto.ClienteResponse
import com.fabio.gestionfacturas.data.remote.dto.CrearClienteRequest
import com.fabio.gestionfacturas.data.remote.dto.EditarClienteRequest
import com.fabio.gestionfacturas.domain.model.Cliente
import com.fabio.gestionfacturas.domain.model.DatosCliente
import com.fabio.gestionfacturas.domain.model.NuevoCliente

internal fun ClienteResponse.aModelo(): Cliente =
    Cliente(
        id = id,
        nombre = nombre,
        nif = nif,
        email = email,
        telefono = telefono,
        activo = activo,
    )

internal fun NuevoCliente.aRequest(): CrearClienteRequest =
    CrearClienteRequest(nombre = nombre, nif = nif, email = email, telefono = telefono)

internal fun DatosCliente.aRequest(): EditarClienteRequest =
    EditarClienteRequest(nombre = nombre, email = email, telefono = telefono)
