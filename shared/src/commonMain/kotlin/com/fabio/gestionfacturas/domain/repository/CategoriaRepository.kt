package com.fabio.gestionfacturas.domain.repository

import com.fabio.gestionfacturas.domain.model.Categoria

interface CategoriaRepository {
    suspend fun listar(): Result<List<Categoria>>
}
