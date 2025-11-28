package k.nutriguard.db

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator.IN
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import k.nutriguard.domain.UserProfile
import k.nutriguard.domain.Group
import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.CartModel
import java.util.UUID
import k.nutriguard.domain.DietaryRestriction
import k.nutriguard.domain.Allergen
import java.time.LocalDate
import java.time.format.DateTimeFormatter



@Serializable
data class CartRow(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GroupRow(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val name: String,
    val owner: String,
    @SerialName("shared_cart") val sharedCartId: String,
)

private fun Group.toRow(): GroupRow =
    GroupRow(
        id = id.toString(),
        name = name,
        owner = owner,
        sharedCartId = sharedCartId.toString()
    )

private fun GroupRow.toDomain(): Group =
    Group(
        name = name,
        id = id?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
        owner = owner,
        // Members + sharedCart are handled with the junction table, need to implement this in the READ function
        members = mutableSetOf(),
        sharedCart = mutableSetOf(),
        sharedCartId = UUID.fromString(sharedCartId)
    )

@Serializable
enum class DietaryRestrictionDb {
    NONE, VEGETARIAN, VEGAN, HALAL, KOSHER, PESCETARIAN
}

// Domain -> DB
private fun k.nutriguard.domain.DietaryRestriction.toDb(): DietaryRestrictionDb = when (this) {
    k.nutriguard.domain.DietaryRestriction.NONE -> DietaryRestrictionDb.NONE
    k.nutriguard.domain.DietaryRestriction.VEGETARIAN -> DietaryRestrictionDb.VEGETARIAN
    k.nutriguard.domain.DietaryRestriction.VEGAN -> DietaryRestrictionDb.VEGAN
    k.nutriguard.domain.DietaryRestriction.HALAL -> DietaryRestrictionDb.HALAL
    k.nutriguard.domain.DietaryRestriction.KOSHER -> DietaryRestrictionDb.KOSHER
    k.nutriguard.domain.DietaryRestriction.PESCETARIAN -> DietaryRestrictionDb.PESCETARIAN
}

private fun DietaryRestrictionDb.toDomain(): k.nutriguard.domain.DietaryRestriction = when (this) {
    DietaryRestrictionDb.NONE -> k.nutriguard.domain.DietaryRestriction.NONE
    DietaryRestrictionDb.VEGETARIAN -> k.nutriguard.domain.DietaryRestriction.VEGETARIAN
    DietaryRestrictionDb.VEGAN -> k.nutriguard.domain.DietaryRestriction.VEGAN
    DietaryRestrictionDb.HALAL -> k.nutriguard.domain.DietaryRestriction.HALAL
    DietaryRestrictionDb.KOSHER -> k.nutriguard.domain.DietaryRestriction.KOSHER
    DietaryRestrictionDb.PESCETARIAN -> k.nutriguard.domain.DietaryRestriction.PESCETARIAN
}


@Serializable
enum class AllergenDb {
    PEANUT, TREE_NUT, MILK, EGG, FISH, SHELLFISH, WHEAT, SOY, SESAME, GLUTEN
}

// Domain -> DB
private fun k.nutriguard.domain.Allergen.toDb(): AllergenDb = when (this) {
    k.nutriguard.domain.Allergen.PEANUT -> AllergenDb.PEANUT
    k.nutriguard.domain.Allergen.TREE_NUT -> AllergenDb.TREE_NUT
    k.nutriguard.domain.Allergen.MILK -> AllergenDb.MILK
    k.nutriguard.domain.Allergen.EGG -> AllergenDb.EGG
    k.nutriguard.domain.Allergen.FISH -> AllergenDb.FISH
    k.nutriguard.domain.Allergen.SHELLFISH -> AllergenDb.SHELLFISH
    k.nutriguard.domain.Allergen.WHEAT -> AllergenDb.WHEAT
    k.nutriguard.domain.Allergen.SOY -> AllergenDb.SOY
    k.nutriguard.domain.Allergen.SESAME -> AllergenDb.SESAME
    k.nutriguard.domain.Allergen.GLUTEN -> AllergenDb.GLUTEN
}

private fun AllergenDb.toDomain(): k.nutriguard.domain.Allergen = when (this) {
    AllergenDb.PEANUT -> k.nutriguard.domain.Allergen.PEANUT
    AllergenDb.TREE_NUT -> k.nutriguard.domain.Allergen.TREE_NUT
    AllergenDb.MILK -> k.nutriguard.domain.Allergen.MILK
    AllergenDb.EGG -> k.nutriguard.domain.Allergen.EGG
    AllergenDb.FISH -> k.nutriguard.domain.Allergen.FISH
    AllergenDb.SHELLFISH -> k.nutriguard.domain.Allergen.SHELLFISH
    AllergenDb.WHEAT -> k.nutriguard.domain.Allergen.WHEAT
    AllergenDb.SOY -> k.nutriguard.domain.Allergen.SOY
    AllergenDb.SESAME -> k.nutriguard.domain.Allergen.SESAME
    AllergenDb.GLUTEN -> k.nutriguard.domain.Allergen.GLUTEN
}

@Serializable
data class UserProfileRow(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("auth_id") val authId: String? = null,   // <-- NEW for logins
    val username: String,
    @SerialName("dietary_restrictions") val dietaryRestrictions: List<DietaryRestrictionDb>? = emptyList(),
    val allergies: List<AllergenDb>? = emptyList(),
    @SerialName("personal_cart_id") val personalCartId: String
)


private fun UserProfile.toRow(): UserProfileRow =
    UserProfileRow(
        id = id.toString(),
        authId = authId?.toString(),     // <-- NEW for logins
        username = username,
        dietaryRestrictions = dietaryRestrictions.map { it.toDb() },
        allergies = allergies.map { it.toDb() },
        personalCartId = personalCartId.toString()
    )

private fun UserProfileRow.toDomain(): UserProfile =
    UserProfile(
        username = username,
        id = id?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
        authId = authId?.let { UUID.fromString(it) },   // <-- NEW for logins
        dietaryRestrictions = (dietaryRestrictions ?: emptyList())
            .map { it.toDomain() }
            .toMutableSet(),

        allergies = (allergies ?: emptyList())
            .map { it.toDomain() }
            .toMutableSet(),

        // These live in other tables, so we start empty for now, need to implement next
        groupIds = mutableSetOf(),
        friendIds = mutableSetOf(),
        personalCart = mutableSetOf(),
        personalCartId = UUID.fromString(personalCartId)
    )

@Serializable
data class FoodItemRow(
    val id: String? = null,
    val name: String,
    val price: Double? = null,
    val barcode: String? = null,
    @SerialName("purchase_date") val purchaseDate: String? = null,
    @SerialName("expiration_date") val expirationDate: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val ingredients: List<String>? = null,
    val allergens: List<AllergenDb>? = null,
    val quantity: Double? = null
)

private val DOMAIN_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd")

private val DB_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ISO_LOCAL_DATE   // "yyyy-MM-dd"

private fun toDbDate(domainDate: String): String? {
    if (domainDate.isBlank()) return null
    return try {
        val d = LocalDate.parse(domainDate, DOMAIN_DATE_FORMAT)
        d.format(DB_DATE_FORMAT)       // "yyyy-MM-dd"
    } catch (_: Exception) {
        null
    }
}

private fun toDomainDate(dbDate: String?): String {
    if (dbDate.isNullOrBlank()) return ""
    // Supabase might return "yyyy-MM-dd" or "yyyy-MM-ddTHH:mm:ss+00:00"
    val datePart = if (dbDate.length >= 10) dbDate.substring(0, 10) else dbDate
    return try {
        val d = LocalDate.parse(datePart, DB_DATE_FORMAT)
        d.format(DOMAIN_DATE_FORMAT)   // "yyyy/MM/dd"
    } catch (_: Exception) {
        ""
    }
}

private fun FoodItem.toRow(): FoodItemRow =
    FoodItemRow(
        id = id.toString(),
        name = name,
        price = price,
        barcode = barcode.ifBlank { null },
        purchaseDate = toDbDate(purchasedDate),
        expirationDate = toDbDate(expirationDate),
        imageUrl = imageUrl.ifBlank { null },
        ingredients = if (ingredients.isEmpty()) null else ingredients.toList(),
        allergens = if (allergens.isEmpty()) null else allergens.map { it.toDb() },
        quantity = quantity
    )

private fun FoodItemRow.toDomain(): FoodItem =
    FoodItem(
        id = id?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
        price = price ?: 0.0,
        barcode = barcode.orEmpty(),
        name = name,
        ingredients = (ingredients ?: emptyList()).toMutableList(),
        allergens = (allergens ?: emptyList()).map { it.toDomain() }.toMutableSet(),
        purchasedDate = toDomainDate(purchaseDate),
        expirationDate = toDomainDate(expirationDate),
        owners = mutableListOf(),  // owners live in junction table between carts and items, implement next, (cartIds)
        quantity = quantity ?: 1.0,
        imageUrl = imageUrl.orEmpty()
    )



@Serializable
private data class UserFriendRow(
    @SerialName("user_name") val username: String,
    @SerialName("friend_name") val friendUsername: String
)


@Serializable
private data class GroupMemberRow(
    @SerialName("group_id") val groupId: String,
    val username: String
)


@Serializable
private data class CartItemRow(
    @SerialName("cart_id") val cartId: String,
    @SerialName("fooditem_id") val foodItemId: String
)


class DBRepository(
    private val client: SupabaseClient
) : DBInterface {

    //UserProfile CRUD
    override suspend fun createUser(user: UserProfile): UserProfile? {
        return try {
            val result = client
                .from("userprofile")
                .insert(user.toRow()) {
                    select()
                }

            val row = result.decodeList<UserProfileRow>().firstOrNull() ?: return null
            row.toDomain()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getUserBasic(username: String): UserProfile? {
        val resp = client.from("userprofile").select {
            filter { eq("username", username) }
            limit(1)
        }

        val row = resp.decodeList<UserProfileRow>().firstOrNull() ?: return null

        // Only base info – no friends / groups / cart / items here
        return row.toDomain()
    }

    //need to add support for getting groups, friends and personal food items
    override suspend fun getUser(username: String): UserProfile? {

        val resp = client.from("userprofile").select {
            filter { eq("username", username) }
            limit(1)
        }

        val row = resp.decodeList<UserProfileRow>().firstOrNull() ?: return null
        //base user without the junction table info yet
        val base = row.toDomain()

        // We get Friends from user_friends junction table
        val friendLinks = client.from("user_friends").select {
            filter { eq("user_name", username) }
        }.decodeList<UserFriendRow>()

        val friendIds = friendLinks
            .map { it.friendUsername }
            .toMutableSet()

        // We get Groups from group_members junction table
        val groupLinks = client.from("group_members").select {
            filter { eq("username", username) }
        }.decodeList<GroupMemberRow>()

        val groupIds = groupLinks
            .map { it.groupId }
            .toMutableSet()

        // We get Personal cart items via cart_items + fooditem tables
        val cartItemLinks = client.from("cart_items").select {
            filter { eq("cart_id", base.personalCartId.toString()) }
        }.decodeList<CartItemRow>()

        val itemIds = cartItemLinks.map { it.foodItemId }.distinct()

        val personalItems: MutableSet<FoodItem> =
            if (itemIds.isEmpty()) {
                mutableSetOf()
            } else {
                val itemsResp = client.from("fooditem").select {
                    filter { isIn("id", itemIds) }
                }
                val itemRows = itemsResp.decodeList<FoodItemRow>()
                itemRows.map { it.toDomain() }.toMutableSet()
            }

        // 5) Return fully complete domain object
        return base.copy(
            groupIds = groupIds,
            friendIds = friendIds,
            personalCart = personalItems
        )
    }

    override suspend fun getUserByAuthId(authId: UUID): UserProfile? {
        val resp = client.from("userprofile").select {
            filter { eq("auth_id", authId.toString()) }
            limit(1)
        }

        val row = resp.decodeList<UserProfileRow>().firstOrNull() ?: return null

        // use getUser here to fully populate userprofile objetc after auth
        return getUser(row.username)
    }


    override suspend fun updateUser(user: UserProfile): Boolean {
        return try {
            client.from("userprofile").update(user.toRow()) {
                filter { eq("username", user.username) }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun deleteUser(username: String): Boolean {
        return try {
            client.from("userprofile").delete {
                filter { eq("username", username) }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    //Group CRUD
    override suspend fun createGroup(group: Group): Group? {
        return try {
            val result = client
                .from("group")
                .insert(group.toRow()) {
                    select()
                }

            val row = result.decodeList<GroupRow>().firstOrNull() ?: return null
            val created = row.toDomain()
            // Also add the owner as a member in group_members
            val groupId = row.id ?: created.id.toString()
            try {
                client.from("group_members").insert(
                    GroupMemberRow(
                        groupId = groupId,
                        username = created.owner
                    )
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                // non-fatal; group still exists, owner just won't show up in groupIds
            }
            created
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getGroup(groupId: String): Group? {
        val resp = client.from("group").select {
            filter { eq("id", groupId) }
            limit(1)
        }

        val row = resp.decodeList<GroupRow>().firstOrNull() ?: return null

        //need to populate sharedCart and members with junction tables
        val base = row.toDomain()

        // Members from group_members junction table
        val memberLinks = client.from("group_members").select {
            filter { eq("group_id", groupId) }
        }.decodeList<GroupMemberRow>()

        val members: MutableSet<String> = memberLinks
            .map { it.username }
            .toMutableSet()

        // Shared cart items via cart_items + fooditem tables
        val cartItemLinks = client.from("cart_items").select {
            filter { eq("cart_id", base.sharedCartId.toString()) }
        }.decodeList<CartItemRow>()

        val itemIds = cartItemLinks.map { it.foodItemId }.distinct()

        val sharedItems: MutableSet<FoodItem> =
            if (itemIds.isEmpty()) {
                mutableSetOf()
            } else {
                val itemsResp = client.from("fooditem").select {
                    filter { isIn("id", itemIds) }
                }
                val itemRows = itemsResp.decodeList<FoodItemRow>()
                itemRows.map { it.toDomain() }.toMutableSet()
            }

        // Return fully populated Group
        return base.copy(
            members = members,
            sharedCart = sharedItems
        )
    }

    override suspend fun updateGroup(group: Group): Boolean {
        return try {
            // We can only update name/owner
            client.from("group").update(group.toRow()) {
                filter { eq("id", group.id.toString()) }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteGroup(groupId: String): Boolean {
        return try {
            client.from("group").delete {
                filter { eq("id", groupId) }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
    //CartModel CRD
    override suspend fun createCart(cart: CartModel): CartModel? {
        val cartId = cart.id
        val result = client
            .from("cart")
            .insert(
                CartRow(
                    id = cartId.toString() // override DB default with our own UUID
                )
            ) {
                select()
            }

        val row = result.decodeList<CartRow>().firstOrNull() ?: return null

        // Optional: if we want to use DB's value instead of local one
        val finalId = row.id?.let { UUID.fromString(it) } ?: cartId

        return CartModel(
            id = finalId,
            items = cart.items.toMutableList()
        )
    }

    override suspend fun getCart(cartId: String): CartModel? {
        val resp = client.from("cart").select {
            filter { eq("id", cartId) }
            limit(1)
        }

        val row = resp.decodeList<CartRow>().firstOrNull() ?: return null
        val uuid = row.id?.let { UUID.fromString(it) } ?: return null

        // Items are stored in the cart_items junction table, need to populate the items field with it
        // Get all cart_items for this cart
        val links = client.from("cart_items").select {
            filter { eq("cart_id", cartId) }
        }.decodeList<CartItemRow>()

        if (links.isEmpty()) {
            // Cart exists but has no items
            return CartModel(
                id = uuid,
                items = mutableListOf()
            )
        }

        // Collect item IDs and fetch the FoodItem rows
        val itemIds = links.map { it.foodItemId }.distinct()

        val itemsResp = client.from("fooditem").select {
            filter { isIn("id", itemIds) }
        }

        val itemRows = itemsResp.decodeList<FoodItemRow>()
        val items = itemRows.map { it.toDomain() }.toMutableList()

        // Return fully populated CartModel
        return CartModel(
            id = uuid,
            items = items
        )
    }

    override suspend fun deleteCart(cartId: UUID): Boolean {
        return try {
            client.from("cart").delete {
                filter { eq("id", cartId.toString()) }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    // FoodItem CRUD
    override suspend fun createFoodItem(item: FoodItem): FoodItem? {
        return try {
            val result = client
                .from("fooditem")
                .insert(item.toRow()) {
                    select()
                }

            val row = result.decodeList<FoodItemRow>().firstOrNull() ?: return null
            val created = row.toDomain()

            val itemId = row.id ?: created.id.toString()

            // For each owner cartId, create a cart_items row
            if (item.owners.isNotEmpty()) {
                item.owners.distinct().forEach { cartId ->
                    try {
                        client.from("cart_items").insert(
                            CartItemRow(
                                cartId = cartId,
                                foodItemId = itemId
                            )
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        // non-fatal; item exists, but that owner link is missing
                    }
                }
            }

            // Return the item with owners set to what was provided
            created.copy(
                owners = item.owners.toMutableList()
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getFoodItem(itemId: String): FoodItem? {
        val resp = client.from("fooditem").select {
            filter { eq("id", itemId) }
            limit(1)
        }

        val row = resp.decodeList<FoodItemRow>().firstOrNull() ?: return null
        // base fooditem, need to populate owners property with mutableListOf cartID strings
        val base = row.toDomain()

        // Find all carts that contain this item from cart_items junction table
        val links = client.from("cart_items").select {
            filter { eq("fooditem_id", itemId) }
        }.decodeList<CartItemRow>()

        val ownerCartIds: MutableList<String> = links
            .map { it.cartId }
            .distinct()
            .toMutableList()

        // return FoodItem with owners populated
        return base.copy(
            owners = ownerCartIds
        )
    }

    override suspend fun updateFoodItem(item: FoodItem): Boolean {
        return try {
            client.from("fooditem").update(item.toRow()) {
                filter { eq("id", item.id.toString()) }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteFoodItem(itemId: String): Boolean {
        return try {
            client.from("fooditem").delete {
                filter { eq("id", itemId) }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    // Friend Functionality (user_friends) junction table
    override suspend fun addFriend(username: String, friendUsername: String): Boolean {
        return try {
            client.from("user_friends").insert(
                UserFriendRow(
                    username = username,
                    friendUsername = friendUsername
                )
            )
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getFriends(username: String): List<UserProfile> {
        return try {
            // 1) read the junction rows
            val links = client.from("user_friends").select {
                filter { eq("user_name", username) }
            }.decodeList<UserFriendRow>()

            if (links.isEmpty()) return emptyList()

            // 2) collect friend usernames
            val friendUsernames = links.map { it.friendUsername }.distinct()
            if (friendUsernames.isEmpty()) return emptyList()

            // 3) fetch their profiles
            val resp = client.from("userprofile").select {
                filter { isIn("username", friendUsernames) }
            }

            val rows = resp.decodeList<UserProfileRow>()
            rows.map { it.toDomain() }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun removeFriend(username: String, friendUsername: String): Boolean {
        return try {
            client.from("user_friends").delete {
                filter {
                    eq("user_name", username)
                    eq("friend_name", friendUsername)
                }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    // Group Member Functionality (group_members) junction table
    override suspend fun addUserToGroup(groupId: String, username: String): Boolean {
        return try {
            client.from("group_members").insert(
                GroupMemberRow(
                    groupId = groupId,
                    username = username
                )
            )
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getGroupMembers(groupId: String): List<UserProfile> {
        return try {
            val rows = client.from("group_members").select {
                filter { eq("group_id", groupId) }
            }.decodeList<GroupMemberRow>()

            if (rows.isEmpty()) return emptyList()

            val usernames = rows.map { it.username }.distinct()
            if (usernames.isEmpty()) return emptyList()

            val resp = client.from("userprofile").select {
                filter { isIn("username", usernames) }
            }

            val userRows = resp.decodeList<UserProfileRow>()
            userRows.map { it.toDomain() }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getUserGroups(username: String): List<Group> {
        return try {
            // Find all group_ids this user belongs to
            val membershipRows = client.from("group_members").select {
                filter { eq("username", username) }
            }.decodeList<GroupMemberRow>()

            if (membershipRows.isEmpty()) return emptyList()

            val groupIds = membershipRows.map { it.groupId }.distinct()
            if (groupIds.isEmpty()) return emptyList()

            // Fetch the base group rows
            val groupRows = client.from("group").select {
                filter { isIn("id", groupIds) }
            }.decodeList<GroupRow>()

            // For each group, populate with members + shared cart items (same as getGroup)
            groupRows.map { gRow ->
                val base = gRow.toDomain()
                val gid = gRow.id ?: base.id.toString()

                // members from group_members
                val memberLinks = client.from("group_members").select {
                    filter { eq("group_id", gid) }
                }.decodeList<GroupMemberRow>()

                val members = memberLinks
                    .map { it.username }
                    .toMutableSet()

                // shared cart items from cart_items + fooditem
                val cartItemLinks = client.from("cart_items").select {
                    filter { eq("cart_id", base.sharedCartId.toString()) }
                }.decodeList<CartItemRow>()

                val itemIds = cartItemLinks.map { it.foodItemId }.distinct()

                val sharedItems: MutableSet<FoodItem> =
                    if (itemIds.isEmpty()) {
                        mutableSetOf()
                    } else {
                        val itemsResp = client.from("fooditem").select {
                            filter { isIn("id", itemIds) }
                        }
                        val itemRows = itemsResp.decodeList<FoodItemRow>()
                        itemRows.map { it.toDomain() }.toMutableSet()
                    }

                // Return fully populated group
                base.copy(
                    members = members,
                    sharedCart = sharedItems
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }


    override suspend fun removeUserFromGroup(groupId: String, username: String): Boolean {
        return try {
            client.from("group_members").delete {
                filter {
                    eq("group_id", groupId)
                    eq("username", username)
                }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    // Cart Item Functionality (cart_items) junction table
    override suspend fun addItemToCart(cartId: String, itemId: String): Boolean {
        return try {
            client.from("cart_items").insert(
                CartItemRow(
                    cartId = cartId,
                    foodItemId = itemId
                )
            )
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getCartItems(cartId: String): List<FoodItem> {
        return try {
            val rows = client.from("cart_items").select {
                filter { eq("cart_id", cartId) }
            }.decodeList<CartItemRow>()

            if (rows.isEmpty()) return emptyList()

            val itemIds = rows.map { it.foodItemId }.distinct()
            if (itemIds.isEmpty()) return emptyList()

            val resp = client.from("fooditem").select {
                filter { isIn("id", itemIds) }
            }

            val foodRows = resp.decodeList<FoodItemRow>()
            foodRows.map { it.toDomain() }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun removeItemFromCart(cartId: String, itemId: String): Boolean {
        return try {
            client.from("cart_items").delete {
                filter {
                    eq("cart_id", cartId)
                    eq("fooditem_id", itemId)
                }
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
