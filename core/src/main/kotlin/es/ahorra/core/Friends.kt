package es.ahorra.core

import java.time.LocalDate

data class PetCompanion(val speciesId: String, val petName: String)

/**
 * Tarjeta de perfil que una persona comparte a mano (mensaje, correo…). Solo lleva datos "de juego":
 * NUNCA importes, nombres de objetivos ni fechas de aportaciones. No está firmada: cualquiera podría
 * escribir una tarjeta a su gusto, así que sirve para presumir entre amigos, no como prueba de nada.
 */
data class FriendCard(
    val profileId: String,
    val name: String,
    val level: Int,
    val bestStreak: Int,
    val achievements: Int,
    val collection: List<String>,
    val companions: List<PetCompanion>,
    val issuedOn: LocalDate,
)

object Friends {
    private const val MAX_COMPANIONS = 12

    private fun clean(text: String, max: Int): String? {
        val t = text.trim()
        if (t.length > max || t.any { it.isISOControl() }) return null
        return t
    }

    private fun validId(id: String): Boolean =
        id.isNotEmpty() && id.length <= Limits.MAX_PROFILE_ID && id.all { it.isLetterOrDigit() || it == '-' }

    /** Mi tarjeta, construida a partir de mi estado. */
    fun myCard(state: AppState, today: LocalDate): FriendCard {
        val companions = state.goals
            .sortedByDescending { Pets.rarityOf(state, it).ordinal }
            .take(MAX_COMPANIONS)
            .map { PetCompanion(Pets.companionOf(state, it).id, it.petName) }
        return FriendCard(
            profileId = state.settings.profileId,
            name = state.settings.displayName,
            level = Gamification.level(state.contributions.size).number,
            bestStreak = Gamification.streak(state, today).best,
            achievements = Gamification.achievements(state, today).size,
            collection = state.collection.filter(Pets::isKnown),
            companions = companions,
            issuedOn = today,
        )
    }

    /** Valida una tarjeta recibida (no fiable). Devuelve null si algo no cuadra; nunca la "arregla". */
    fun sanitize(card: FriendCard, today: LocalDate): FriendCard? {
        if (!validId(card.profileId)) return null
        val name = clean(card.name, Limits.MAX_DISPLAY_NAME)?.ifEmpty { null } ?: return null
        if (card.level !in 1..8) return null
        if (card.bestStreak !in 0..1200) return null
        if (card.achievements !in 0..Achievement.entries.size) return null
        if (card.collection.size > Pets.all.size || card.collection.any { !Pets.isKnown(it) }) return null
        if (card.companions.size > MAX_COMPANIONS) return null
        val companions = card.companions.map {
            if (!Pets.isKnown(it.speciesId)) return null
            PetCompanion(it.speciesId, clean(it.petName, Limits.MAX_PET_NAME) ?: return null)
        }
        if (card.issuedOn.isAfter(today.plusDays(1)) || card.issuedOn.isBefore(Limits.MIN_DATE)) return null
        return card.copy(name = name, collection = card.collection.distinct(), companions = companions)
    }
}
