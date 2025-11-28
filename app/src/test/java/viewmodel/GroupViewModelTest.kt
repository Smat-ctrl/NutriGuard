package model

import db.MockDBRepository
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.CartModel
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.Group
import k.nutriguard.domain.UserProfile
import k.nutriguard.viewmodel.GroupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testUser(username: String): UserProfile =
        UserProfile(
            username = username,
            authId = UUID.randomUUID(),
            dietaryRestrictions = mutableSetOf(),
            allergies = mutableSetOf(),
            personalCartId = UUID.randomUUID(),
            personalCart = mutableSetOf(),
            groupIds = mutableSetOf(),
            friendIds = mutableSetOf()
        )

    private suspend fun seedDatabase(db: MockDBRepository): Pair<Group, Group> {
        val usernames = listOf(
            "u_alice",
            "u_john",
            "u_carla",
            "u_derek",
            "u_new",
            "u_tmp",
            "u_xyz"
        )
        usernames.forEach { db.createUser(testUser(it)) }

        val cartA = db.createCart(
            CartModel(
                id = UUID.randomUUID(),
                items = mutableListOf()
            )
        )!!

        val cartB = db.createCart(
            CartModel(
                id = UUID.randomUUID(),
                items = mutableListOf()
            )
        )!!

        val groupA = Group(
            name = "Grocery Buddies",
            id = UUID.randomUUID(),
            owner = "u_alice",
            members = mutableSetOf(),
            sharedCartId = cartA.id,
            sharedCart = mutableSetOf()
        )

        val groupB = Group(
            name = "Room 2B",
            id = UUID.randomUUID(),
            owner = "u_john",
            members = mutableSetOf(),
            sharedCartId = cartB.id,
            sharedCart = mutableSetOf()
        )

        db.createGroup(groupA)
        db.createGroup(groupB)

        db.addUserToGroup(groupA.id.toString(), "u_alice")
        db.addUserToGroup(groupA.id.toString(), "u_john")
        db.addUserToGroup(groupA.id.toString(), "u_carla")

        db.addUserToGroup(groupB.id.toString(), "u_john")
        db.addUserToGroup(groupB.id.toString(), "u_derek")

        return groupA to groupB
    }

    private suspend fun TestScope.createViewModelForUser(
        currentUsername: String
    ): GroupViewModel {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)
        vm.initForUser(currentUsername)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `initial state has two groups and myGroups filters by current user`() = runTest {
        val vm = createViewModelForUser("u_john")
        val state = vm.uiState.value

        assertEquals(2, state.groups.size)
        assertEquals("u_john", state.currentUsername)

        assertEquals(2, state.myGroups.size)
        assertTrue(state.myGroups.all { g -> g.isMember("u_john") })
    }

    @Test
    fun `initForUser updates myGroups accordingly`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        vm.initForUser("u_carla")
        advanceUntilIdle()
        var state = vm.uiState.value
        assertEquals("u_carla", state.currentUsername)
        assertEquals(1, state.myGroups.size)
        assertEquals("Grocery Buddies", state.myGroups.first().name)

        vm.initForUser("u_alice")
        advanceUntilIdle()
        state = vm.uiState.value
        assertEquals("u_alice", state.currentUsername)
        assertEquals(1, state.myGroups.size)
        assertEquals("Grocery Buddies", state.myGroups.first().name)

        vm.initForUser("u_derek")
        advanceUntilIdle()
        state = vm.uiState.value
        assertEquals("u_derek", state.currentUsername)
        assertEquals(1, state.myGroups.size)
        assertEquals("Room 2B", state.myGroups.first().name)

        vm.initForUser("u_xyz")
        advanceUntilIdle()
        state = vm.uiState.value
        assertEquals("u_xyz", state.currentUsername)
        assertTrue(state.myGroups.isEmpty())
    }

    @Test
    fun `createGroup adds new group owned by current user`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        vm.initForUser("u_john")
        advanceUntilIdle()

        val before = vm.uiState.value.groups.size

        vm.createGroup("New Crew")
        advanceUntilIdle()

        val afterState = vm.uiState.value
        assertEquals(before + 1, afterState.groups.size)

        val created = afterState.groups.last()
        assertEquals("New Crew", created.name)
        assertEquals(afterState.currentUsername, created.owner)
        assertTrue(created.members.isEmpty())
    }

    @Test
    fun `createGroup blank name defaults to Untitled Group`() = runTest {
        val vm = createViewModelForUser("u_john")

        val before = vm.uiState.value.groups.size
        vm.createGroup("")
        advanceUntilIdle()
        val afterState = vm.uiState.value

        assertEquals(before + 1, afterState.groups.size)
        assertEquals("Untitled Group", afterState.groups.last().name)
    }

    @Test
    fun `deleteGroup removes the group by id`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        vm.initForUser("u_john")
        advanceUntilIdle()

        val initial = vm.uiState.value.groups
        assertTrue(initial.any { it.name == "Room 2B" })

        val toDeleteId = initial.first { it.name == "Room 2B" }.id
        vm.deleteGroup(toDeleteId)
        advanceUntilIdle()

        val after = vm.uiState.value.groups
        assertFalse(after.any { it.id == toDeleteId })
    }

    @Test
    fun `addMember adds non-owner and is idempotent`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        vm.initForUser("u_alice")
        advanceUntilIdle()

        val groupId = vm.uiState.value.groups.first { it.name == "Grocery Buddies" }.id

        vm.addMember(groupId, "u_new")
        vm.addMember(groupId, "u_new")
        advanceUntilIdle()

        val g = vm.uiState.value.groups.first { it.id == groupId }
        assertTrue("u_new" in g.members)
        assertEquals(g.members.size, g.members.toSet().size)
    }

    @Test
    fun `removeMember removes member but not owner`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        vm.initForUser("u_john")
        advanceUntilIdle()

        val groupId = vm.uiState.value.groups.first { it.name == "Room 2B" }.id
        val ownerId = vm.uiState.value.groups.first { it.id == groupId }.owner

        vm.addMember(groupId, "u_tmp")
        advanceUntilIdle()
        var g = vm.uiState.value.groups.first { it.id == groupId }
        assertTrue("u_tmp" in g.members)

        vm.removeMember(groupId, "u_tmp")
        advanceUntilIdle()
        g = vm.uiState.value.groups.first { it.id == groupId }
        assertFalse("u_tmp" in g.members)

        vm.removeMember(groupId, ownerId)
        advanceUntilIdle()
        g = vm.uiState.value.groups.first { it.id == groupId }
        assertEquals(ownerId, g.owner)
    }

    @Test
    fun `clearMembers empties non-owner members but preserves owner`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        vm.initForUser("u_alice")
        advanceUntilIdle()

        val groupId = vm.uiState.value.groups.first { it.name == "Grocery Buddies" }.id

        val before = vm.uiState.value.groups.first { it.id == groupId }
        val ownerBefore = before.owner
        // Ensure there is at least one non-owner member
        assertTrue(before.members.any { it != ownerBefore })

        vm.clearMembers(groupId)
        advanceUntilIdle()

        val after = vm.uiState.value.groups.first { it.id == groupId }

        // Owner must still be set and unchanged
        assertNotNull(after.owner)
        assertEquals(ownerBefore, after.owner)

        // No non-owner members should remain
        assertTrue(after.members.all { it == after.owner })
    }

    @Test
    fun `addItemToSharedCart and removeItemFromSharedCart do not corrupt groups`() = runTest {
        val db = MockDBRepository()
        seedDatabase(db)
        val vm = GroupViewModel(db)

        // u_john is member of both groups
        vm.initForUser("u_john")
        advanceUntilIdle()

        val initialState = vm.uiState.value
        val initialGroupIds = initialState.groups.map { it.id }.toSet()
        val initialGroupCount = initialState.groups.size

        val groupAId = initialState.groups.first { it.name == "Grocery Buddies" }.id
        val groupBId = initialState.groups.first { it.name == "Room 2B" }.id

        // Call addItemToSharedCart for both groups
        vm.addItemToSharedCart(
            groupId = groupAId,
            itemName = "milk",
            quantity = 1.0,
            allergens = emptySet<Allergen>(),
            ingredients = emptyList(),
            expirationDate = "2025-01-01"
        )
        vm.addItemToSharedCart(
            groupId = groupAId,
            itemName = "bread",
            quantity = 1.0,
            allergens = emptySet<Allergen>(),
            ingredients = emptyList(),
            expirationDate = "2025-01-01"
        )
        vm.addItemToSharedCart(
            groupId = groupBId,
            itemName = "milk",
            quantity = 1.0,
            allergens = emptySet<Allergen>(),
            ingredients = emptyList(),
            expirationDate = "2025-01-01"
        )
        advanceUntilIdle()

        // Try removing from A with a dummy id – this should not crash and should not affect membership/group list
        vm.removeItemFromSharedCart(groupAId, UUID.randomUUID())
        advanceUntilIdle()

        val afterState = vm.uiState.value

        // Groups list should not be corrupted
        assertEquals(initialGroupCount, afterState.groups.size)
        assertEquals(initialGroupIds, afterState.groups.map { it.id }.toSet())

        // Group names and owners still consistent
        val gA = afterState.groups.first { it.id == groupAId }
        val gB = afterState.groups.first { it.id == groupBId }
        assertEquals("Grocery Buddies", gA.name)
        assertEquals("Room 2B", gB.name)
        assertEquals("u_alice", gA.owner)
        assertEquals("u_john", gB.owner)
    }

}
