package es.ahorra.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PetsAndFriendsTest {

    private val today = LocalDate.of(2026, 11, 26)

    private fun contribution(id: String, amount: Long, date: LocalDate, goalId: String = "g1") =
        Contribution(id, goalId, amount, date)

    private fun withGoal(): AppState = Ledger.createGoal(AppState(), goal()).okState()

    private fun add(s: AppState, id: String, amount: Long, date: LocalDate): AppState =
        Ledger.addContribution(s, contribution(id, amount, date), today).okState()

    @Test
    fun everyGoalHatchesACommonPetAndItIsCollected() {
        val s = withGoal()
        assertEquals(Rarity.COMMON, Pets.rarityOf(s, s.goals.single()))
        assertEquals(1, s.collection.size)
        assertEquals(Rarity.COMMON, Pets.species(s.collection.single())!!.rarity)
    }

    @Test
    fun theSameGoalAlwaysGetsTheSameSpecies() {
        assertEquals(Pets.speciesFor("abc", Rarity.RARE), Pets.speciesFor("abc", Rarity.RARE))
        val distinct = (1..60).map { Pets.speciesFor("goal-$it", Rarity.COMMON).id }.toSet()
        assertTrue(distinct.size > 1, "la especie debe variar entre objetivos")
        Rarity.entries.forEach { r -> assertEquals(r, Pets.speciesFor("x", r).rarity) }
    }

    @Test
    fun rarityIsEarnedByConsistencyAndProgressNotByChance() {
        // objetivo de 1.200 € (ver goal()); tres meses distintos con aportaciones pequeñas -> rara
        var s = withGoal()
        s = add(s, "a", 1_000, LocalDate.of(2026, 9, 5))
        s = add(s, "b", 1_000, LocalDate.of(2026, 10, 5))
        assertEquals(Rarity.COMMON, Pets.rarityOf(s, s.goals.single()))
        s = add(s, "c", 1_000, LocalDate.of(2026, 11, 5))
        assertEquals(Rarity.RARE, Pets.rarityOf(s, s.goals.single()))
        // la mitad del objetivo -> épica
        val half = s.goals.single().targetMinor / 2
        s = add(s, "d", half, today)
        assertEquals(Rarity.EPIC, Pets.rarityOf(s, s.goals.single()))
        // objetivo cumplido -> legendaria
        s = add(s, "e", s.goals.single().targetMinor, today.minusDays(1))
        assertEquals(Rarity.LEGENDARY, Pets.rarityOf(s, s.goals.single()))
        assertEquals(4, s.collection.size)
    }

    @Test
    fun theCollectionNeverShrinksEvenIfTheGoalIsDeleted() {
        var s = withGoal()
        s = add(s, "d", s.goals.single().targetMinor, today)
        val before = s.collection
        assertEquals(4, before.size)
        s = Ledger.deleteGoal(s, "g1").okState()
        assertEquals(before, Pets.discover(s).collection)
        assertEquals(before, s.collection)
    }

    @Test
    fun petNamesAreValidatedAndCanBeChanged() {
        val s = Ledger.createGoal(AppState(), goal().copy(petName = "  Naranjito ")).okState()
        assertEquals("Naranjito", s.goals.single().petName)
        assertEquals(
            Reason.INVALID_PET_NAME,
            (Ledger.createGoal(AppState(), goal().copy(petName = "x".repeat(Limits.MAX_PET_NAME + 1))) as LedgerResult.Rejected).reason,
        )
        assertEquals("Luna", Ledger.renamePet(s, "g1", "Luna").okState().goals.single().petName)
        assertEquals(Reason.UNKNOWN_GOAL, (Ledger.renamePet(s, "zz", "Luna") as LedgerResult.Rejected).reason)
    }

    // ---- Amigos ----

    private fun card(id: String = "friend-1") = FriendCard(
        profileId = id, name = "Lucía", level = 3, bestStreak = 4, achievements = 5,
        collection = listOf("chick", "fox"), companions = listOf(PetCompanion("fox", "Rayo")), issuedOn = today,
    )

    @Test
    fun myCardCarriesNoMoneyOrGoalNames() {
        var s = withGoal()
        s = add(s, "a", 5_000, today)
        val mine = Friends.myCard(s.copy(settings = s.settings.copy(displayName = "Ana", profileId = "me-1")), today)
        assertEquals("Ana", mine.name)
        assertEquals(1, mine.companions.size)
        val text = mine.toString()
        assertTrue(!text.contains(s.goals.single().name), "el nombre del objetivo no debe viajar en la tarjeta")
    }

    @Test
    fun friendCardsAreValidatedAsUntrustedInput() {
        assertTrue(Friends.sanitize(card(), today) != null)
        assertNull(Friends.sanitize(card().copy(profileId = ""), today))
        assertNull(Friends.sanitize(card().copy(profileId = "a b"), today))
        assertNull(Friends.sanitize(card().copy(name = "  "), today))
        assertNull(Friends.sanitize(card().copy(name = "x".repeat(Limits.MAX_DISPLAY_NAME + 1)), today))
        assertNull(Friends.sanitize(card().copy(level = 9), today))
        assertNull(Friends.sanitize(card().copy(collection = listOf("godzilla")), today))
        assertNull(Friends.sanitize(card().copy(companions = listOf(PetCompanion("dragon\u0000", ""))), today))
        assertNull(Friends.sanitize(card().copy(companions = listOf(PetCompanion("fox", "mal\nnombre"))), today))
        assertNull(Friends.sanitize(card().copy(issuedOn = today.plusDays(30)), today))
    }

    @Test
    fun addingTheSameFriendUpdatesInsteadOfDuplicating() {
        var s = AppState()
        s = Ledger.addFriend(s, card(), today).okState()
        s = Ledger.addFriend(s, card().copy(level = 4), today).okState()
        assertEquals(1, s.friends.size)
        assertEquals(4, s.friends.single().level)
        val same = Ledger.addFriend(s, card().copy(level = 4), today)
        assertEquals(false, (same as LedgerResult.Ok).changed)
        assertEquals(0, Ledger.removeFriend(s, "friend-1").okState().friends.size)
    }

    @Test
    fun youCannotAddYourOwnCardAndTheListIsCapped() {
        val me = AppState(settings = Settings(profileId = "me-1"))
        assertEquals(Reason.INVALID_FRIEND, (Ledger.addFriend(me, card("me-1"), today) as LedgerResult.Rejected).reason)
        var s = AppState()
        repeat(Limits.MAX_FRIENDS) { s = Ledger.addFriend(s, card("f-$it"), today).okState() }
        assertEquals(Reason.LIMIT_REACHED, (Ledger.addFriend(s, card("one-more"), today) as LedgerResult.Rejected).reason)
    }

    @Test
    fun importRejectsUnknownSpeciesAndBadFriends() {
        val base = withGoal()
        assertTrue(Ledger.sanitizeImport(base.copy(collection = listOf("nope")), today) == null)
        assertTrue(Ledger.sanitizeImport(base.copy(friends = listOf(card().copy(level = 0))), today) == null)
        assertTrue(Ledger.sanitizeImport(base.copy(friends = listOf(card(), card())), today) == null)
        assertNotEquals(null, Ledger.sanitizeImport(base.copy(friends = listOf(card())), today))
    }
}
