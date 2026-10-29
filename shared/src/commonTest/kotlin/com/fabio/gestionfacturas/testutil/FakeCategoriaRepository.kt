package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.model.Categoria
import com.fabio.gestionfacturas.domain.repository.CategoriaRepository

class FakeCategoriaRepository : CategoriaRepository {
    var resultadoListar: Result<List<Categoria>> = Result.success(emptyList())
    var llamadasListar = 0

    override suspend fun listar(): Result<List<Categoria>> {
        llamadasListar++
        return resultadoListar
    }
}
