package es.ahorra.core

/** Rarezas de las mascotas. Se ganan por constancia y progreso, nunca por azar ni con dinero. */
enum class Rarity { COMMON, RARE, EPIC, LEGENDARY }

data class Species(val id: String, val rarity: Rarity)

/**
 * Mascotas coleccionables. Cada objetivo tiene un compañero que nace al crearlo (común) y evoluciona con tu progreso:
 *  - RARA: aportaciones en 3 meses distintos a ese objetivo (constancia).
 *  - ÉPICA: la mitad del objetivo ahorrada.
 *  - LEGENDARIA: objetivo cumplido.
 * La especie concreta de cada fase sale del identificador del objetivo (siempre la misma para el mismo objetivo).
 */
object Pets {

    val all: List<Species> = listOf(
        Species("chick", Rarity.COMMON), Species("bunny", Rarity.COMMON), Species("kitten", Rarity.COMMON),
        Species("puppy", Rarity.COMMON), Species("hamster", Rarity.COMMON), Species("froglet", Rarity.COMMON),
        Species("fox", Rarity.RARE), Species("panda", Rarity.RARE), Species("koala", Rarity.RARE),
        Species("owl", Rarity.RARE), Species("penguin", Rarity.RARE),
        Species("unicorn", Rarity.EPIC), Species("octopus", Rarity.EPIC), Species("peacock", Rarity.EPIC),
        Species("wolf", Rarity.EPIC),
        Species("lion", Rarity.LEGENDARY), Species("dragon", Rarity.LEGENDARY), Species("whale", Rarity.LEGENDARY),
    )

    private val byId: Map<String, Species> = all.associateBy { it.id }

    fun isKnown(id: String): Boolean = id in byId
    fun species(id: String): Species? = byId[id]
    fun ofRarity(rarity: Rarity): List<Species> = all.filter { it.rarity == rarity }

    /** Hash estable (FNV-1a) para que la misma mascota salga siempre, en cualquier móvil. */
    private fun stableHash(text: String): Int {
        var h = -0x7ee3623b // 2166136261 como Int
        for (ch in text) {
            h = h xor ch.code
            h *= 16777619
        }
        return h and 0x7fffffff
    }

    fun speciesFor(goalId: String, rarity: Rarity): Species {
        val pool = ofRarity(rarity)
        return pool[stableHash(goalId + ":" + rarity.name) % pool.size]
    }

    fun rarityOf(state: AppState, goal: Goal): Rarity {
        val saved = Ledger.savedMinor(state, goal.id)
        if (goal.status == GoalStatus.COMPLETED || saved >= goal.targetMinor) return Rarity.LEGENDARY
        if (saved * 2 >= goal.targetMinor) return Rarity.EPIC
        val months = state.contributions.asSequence()
            .filter { it.goalId == goal.id }
            .map { java.time.YearMonth.from(it.date) }
            .distinct()
            .count()
        return if (months >= 3) Rarity.RARE else Rarity.COMMON
    }

    fun companionOf(state: AppState, goal: Goal): Species = speciesFor(goal.id, rarityOf(state, goal))

    /** Fracción (0..1) hacia la siguiente rareza, para mostrar "te falta poco". 1 si ya es legendaria. */
    fun nextRarityFraction(state: AppState, goal: Goal): Float {
        val saved = Ledger.savedMinor(state, goal.id)
        return when (rarityOf(state, goal)) {
            Rarity.LEGENDARY -> 1f
            Rarity.EPIC -> (saved.toFloat() / goal.targetMinor).coerceIn(0f, 1f)
            Rarity.RARE -> (saved.toFloat() * 2f / goal.targetMinor).coerceIn(0f, 1f)
            Rarity.COMMON -> {
                val months = state.contributions.filter { it.goalId == goal.id }
                    .map { java.time.YearMonth.from(it.date) }.distinct().size
                (months / 3f).coerceIn(0f, 1f)
            }
        }
    }

    /** Todas las especies que los objetivos actuales han dado (cada fase superada cuenta). */
    fun derivedDiscoveries(state: AppState): Set<String> {
        val result = mutableSetOf<String>()
        for (goal in state.goals) {
            val top = rarityOf(state, goal)
            Rarity.entries.filter { it.ordinal <= top.ordinal }.forEach { result += speciesFor(goal.id, it).id }
        }
        return result
    }

    /** Añade a la colección lo descubierto; nunca quita nada. */
    fun discover(state: AppState): AppState {
        val merged = (state.collection + derivedDiscoveries(state)).filter(::isKnown).distinct()
        return if (merged == state.collection) state else state.copy(collection = merged)
    }
}
