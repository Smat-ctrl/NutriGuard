package model

import k.nutriguard.domain.FoodItem
import k.nutriguard.domain.CartModel

import org.junit.Test
import org.junit.Assert.*

internal class CartModelTest {
    @Test
    fun addingItems() {
        val testCart: CartModel = CartModel()
        assertEquals(0, testCart.items.size)
        val item1 = FoodItem(price = 15.0, barcode = "123",
            purchasedDate = "2025/10/05", expirationDate = "2025/10/07",
            owners =  mutableListOf())
        testCart.addFoodItem(item1)
        assertEquals(1, testCart.items.size)
        val item2 = FoodItem(price = 5.0, barcode = "367",
            purchasedDate = "2025/10/08", expirationDate = "2025/10/21",
            owners =  mutableListOf())
        testCart.addFoodItem(item2)
        assertEquals(2, testCart.items.size)
    }

    @Test
    fun removeItem() {
        val testCart: CartModel = CartModel()
        val item1 = FoodItem(price = 15.0, barcode = "123",
            purchasedDate = "2025/10/05", expirationDate = "2025/10/07",
            owners =  mutableListOf())
        val item2 = FoodItem(price = 5.0, barcode = "367",
            purchasedDate = "2025/10/08", expirationDate = "2025/10/21",
            owners =  mutableListOf())
        testCart.addFoodItem(item1)
        testCart.addFoodItem(item2)
        assertEquals(2, testCart.items.size)
        testCart.removeFoodItem(item1)
        assertEquals(1, testCart.items.size)
        testCart.removeFoodItem(item2)
        assertEquals(0, testCart.items.size)
    }

    @Test
    fun calculateTotal() {
        val testCart: CartModel = CartModel()
        val item1 = FoodItem(price = 15.0, barcode = "123",
            purchasedDate = "2025/10/05", expirationDate = "2025/10/07",
            owners =  mutableListOf())
        val item2 = FoodItem(price = 5.0, barcode = "367",
            purchasedDate = "2025/10/08", expirationDate = "2025/10/21",
            owners =  mutableListOf())
        testCart.addFoodItem(item1)
        testCart.addFoodItem(item2)
        assertEquals(20.0, testCart.totalPrice(), 1e-6)
    }

    @Test
    fun expiredCount() {
        val testCart: CartModel = CartModel()
        val item1 = FoodItem(price = 15.0, barcode = "123",
            purchasedDate = "2025/10/05", expirationDate = "2025/10/07",
            owners =  mutableListOf())
        val item2 = FoodItem(price = 5.0, barcode = "367",
            purchasedDate = "2025/10/08", expirationDate = "2026/10/21",
            owners =  mutableListOf())
        testCart.addFoodItem(item1)
        testCart.addFoodItem(item2)
        assertEquals(1, testCart.expiredCount())
    }
}