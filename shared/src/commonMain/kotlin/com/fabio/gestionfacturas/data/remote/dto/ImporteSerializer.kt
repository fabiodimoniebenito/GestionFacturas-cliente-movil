package com.fabio.gestionfacturas.data.remote.dto

import com.fabio.gestionfacturas.domain.model.Importe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/**
 * Reads a JSON number as its literal text (so `121.00` keeps its trailing zeros) and writes it back
 * as a JSON NUMBER literal, not a quoted string. JSON only.
 */
@OptIn(ExperimentalSerializationApi::class)
object ImporteSerializer : KSerializer<Importe> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.fabio.gestionfacturas.Importe", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Importe {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Importe needs JSON")
        val elemento = json.decodeJsonElement()
        if (elemento !is JsonPrimitive || elemento.isString) {
            throw SerializationException("Importe must be a JSON number")
        }
        return Importe(elemento.content)
    }

    override fun serialize(encoder: Encoder, value: Importe) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Importe needs JSON")
        json.encodeJsonElement(JsonUnquotedLiteral(value.valor))
    }
}
