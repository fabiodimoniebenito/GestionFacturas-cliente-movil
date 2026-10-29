package com.fabio.gestionfacturas.data.repository

import com.fabio.gestionfacturas.data.remote.CategoriaApi
import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.repository.CategoriaRepository

class CategoriaRepositoryImpl(private val api: CategoriaApi) : CategoriaRepository {

    override suspend fun listar(): Result<List<Categoria>> =
        resultadoDe { api.listar().map { it.aModelo() } }
}
