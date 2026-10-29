package com.fabio.gestionfacturas.domain.model

data class Categoria(
    val id: Long,
    val nombre: String,
    val deduciblePorDefecto: Boolean,
)
