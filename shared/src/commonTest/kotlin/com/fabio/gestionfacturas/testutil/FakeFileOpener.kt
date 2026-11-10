package com.fabio.gestionfacturas.testutil

import com.fabio.gestionfacturas.domain.repository.FileOpener
import com.fabio.gestionfacturas.domain.repository.ResultadoSeleccion

class FakeFileOpener : FileOpener {
    var resultado: ResultadoSeleccion = ResultadoSeleccion.Cancelado
    var llamadas = 0

    override suspend fun elegirImagen(): ResultadoSeleccion {
        llamadas++
        return resultado
    }
}
