package com.fabio.gestionfacturas.data.remote

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 400 bodies are a field -> message map (validation) or `{"error": "..."}` (domain rule). */
internal suspend fun camposDeError(respuesta: HttpResponse): Map<String, String> {
    val cuerpo = try {
        respuesta.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return emptyMap()
    }
    val raiz = try {
        Json.parseToJsonElement(cuerpo)
    } catch (e: Exception) {
        return emptyMap()
    }
    if (raiz !is JsonObject) {
        return emptyMap()
    }
    val campos = mutableMapOf<String, String>()
    for ((clave, valor) in raiz) {
        if (valor is JsonPrimitive) {
            campos[clave] = valor.content
        }
    }
    return campos
}
