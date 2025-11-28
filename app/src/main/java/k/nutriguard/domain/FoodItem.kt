package k.nutriguard.domain
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class FoodItem(val id: UUID = UUID.randomUUID(),
                    var price: Double = 0.0,
                    var barcode: String = "",
                    var name: String = " ",
                    var ingredients: MutableList<String> = mutableListOf(),
                    var allergens: MutableSet<Allergen> = mutableSetOf(),
                    var purchasedDate: String = "", //in this format YYYY/MM/DD
                    var expirationDate: String = " ", // in this format YYYY/MM/DD
                    var owners: MutableList<String>, //string of cartIds
                    var quantity: Double = 1.0,
                    var imageUrl: String= " ") {

    fun containsAllergen(user: UserProfile): Boolean {
        return this.allergens.intersect(user.allergies).isNotEmpty()
    }

    fun isExpired(): Boolean {
        if (expirationDate.isBlank()) return false

        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            val expiryDate = LocalDate.parse(expirationDate, formatter)
            val today = LocalDate.now()
            expiryDate.isBefore(today)
        }
        catch (e : Exception) {
            false
        }
    }
}

