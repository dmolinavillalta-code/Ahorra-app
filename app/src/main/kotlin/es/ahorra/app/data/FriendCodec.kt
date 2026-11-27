package es.ahorra.app.data

import es.ahorra.core.FriendCard
import es.ahorra.core.Friends
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Base64

/**
 * Texto que se comparte con un amigo: "AHORRA-AMIGO1:" + JSON en Base64. Sin red: se envía por el mensajero que
 * la persona elija y el amigo lo pega en la app. Lo recibido se trata siempre como no fiable.
 */
object FriendCodec {
    const val PREFIX = "AHORRA-AMIGO1:"
    private const val MAX_CHARS = 8_000
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(card: FriendCard): String {
        val raw = json.encodeToString(FriendDto.serializer(), card.toDto()).toByteArray(Charsets.UTF_8)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
    }

    /** null si el texto no es una tarjeta válida. Busca el prefijo dentro del texto (por si viene con un mensaje). */
    fun decode(text: String, today: LocalDate): FriendCard? {
        if (text.length > MAX_CHARS * 2) return null
        val start = text.indexOf(PREFIX)
        if (start < 0) return null
        val payload = text.substring(start + PREFIX.length).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
        if (payload.isEmpty() || payload.length > MAX_CHARS) return null
        return try {
            val raw = Base64.getUrlDecoder().decode(payload)
            val dto = json.decodeFromString(FriendDto.serializer(), String(raw, Charsets.UTF_8))
            Friends.sanitize(dto.toDomain(), today)
        } catch (e: Exception) {
            null
        }
    }
}
