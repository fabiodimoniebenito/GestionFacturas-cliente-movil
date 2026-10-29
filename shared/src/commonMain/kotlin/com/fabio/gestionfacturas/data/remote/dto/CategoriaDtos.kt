package com.fabio.gestionfacturas.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoriaResponse(
    val id: Long,
    val nombre: String,
    val deduciblePorDefecto: Boolean = false,
)
