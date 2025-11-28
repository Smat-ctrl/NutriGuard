package k.nutriguard.domain

import java.util.UUID

data class CartModel(val id: UUID = UUID.randomUUID(),
                     val items: MutableList<FoodItem> = mutableListOf())
{
    fun addFoodItem(foodItem: FoodItem) {
        items.add(foodItem)
    }
    fun removeFoodItem(foodItem: FoodItem) {
        items.remove(foodItem)
    }

    fun totalPrice(): Double {
        if (items.isEmpty()) return 0.0
        return items.map { it.price }.sum()
    }

    fun expiredCount(): Int {
        var count = 0
        for (item in items) {
            if (item.isExpired()) {
                count += 1
            }
        }
        return count
    }


}