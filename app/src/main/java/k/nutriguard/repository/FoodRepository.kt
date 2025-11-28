package k.nutriguard.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import k.nutriguard.domain.Allergen
import k.nutriguard.domain.FoodItem
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

// This class handles calls to the OpenFoodFacts API and parses results into FoodItem Objects
class FoodRepository {

    suspend fun searchByName(query: String, pageSize: Int = 20): List<FoodItem> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val urlStr =
                "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=$pageSize"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "NutriGuard/1.0 (android)")
                setRequestProperty("Accept", "application/json")
            }

            try {
                val code = conn.responseCode // Here we check the HTTP status code
                if (code != HttpURLConnection.HTTP_OK) return@withContext emptyList<FoodItem>()

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body) // parse the response into JSON object
                // We try to get an array of products or empty array if we can't
                val products = root.optJSONArray("products") ?: JSONArray()
                parseProducts(products) // convert the JSON array into a list of FoodItem obejcts
            } catch (_: Exception) {
                emptyList()
            } finally {
                conn.disconnect()
            }
        }

    // This runs the barcode lookup
    suspend fun searchByBarcode(barcode: String): FoodItem? =
        withContext(Dispatchers.IO) {
            val trimmed = barcode.trim()
            // Start by ignoring blank barcodes
            if (trimmed.isEmpty()) return@withContext null

            val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name()) // The URL-encoded barcode
            val urlStr = "https://world.openfoodfacts.org/api/v0/product/$encoded.json" // The OpenFoodFacts single-product endpoint

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                // After setting the GET request method, we identify our app to the API and request a JSON response
                setRequestProperty("User-Agent", "NutriGuard/1.0 (android)")
                setRequestProperty("Accept", "application/json")
            }

            try {
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) return@withContext null

                // We read the full response and parse it into a JSON object
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)

                // This is with OpenFoodFacts: status == 1 means product found, 0 means not found
                val status = root.optInt("status", 0)
                if (status != 1) return@withContext null

                // Then we get the nested product object and parse the JSON object into a FoodItem
                val productObj = root.optJSONObject("product") ?: return@withContext null
                parseSingleProduct(productObj)
            } catch (_: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }


    // Below is the logic for parsing, it loops through all the products and parses each one
    private fun parseProducts(products: JSONArray): List<FoodItem> {
        val out = ArrayList<FoodItem>(products.length())
        for (i in 0 until products.length()) {
            val obj = products.optJSONObject(i) ?: continue
            parseSingleProduct(obj)?.let(out::add)
        }
        return out
    }

    // Below is the logic for converting a single JSON product into a FoodItem
    private fun parseSingleProduct(obj: JSONObject): FoodItem? {
        val code = obj.optString("code").takeIf { it.isNotBlank() } ?: return null

        // ingredients below that we split by common separators and remove extra spaces
        val ingredientsText = obj.optString("ingredients_text", "")
        val ingredients = ingredientsText
            .split(',', ';', '\n', '\r', '•')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        // We try to get the main front image if it's available
        val imageUrl = obj.optString("image_front_url")
            .ifBlank { obj.optString("image_url", "") }

        // allergens code where we just loop through all the allergen tags
        val allergenTags = obj.optJSONArray("allergens_tags") ?: JSONArray()
        val allergens = mutableSetOf<Allergen>()
        for (j in 0 until allergenTags.length()) {
            val tag = allergenTags.optString(j).substringAfter(':').lowercase()
            when (tag) {
                "peanut", "peanuts" -> allergens += Allergen.PEANUT
                "tree-nuts", "tree_nuts", "nuts", "hazelnut", "almond", "walnut" -> allergens += Allergen.TREE_NUT
                "milk", "lactose" -> allergens += Allergen.MILK
                "egg", "eggs" -> allergens += Allergen.EGG
                "fish" -> allergens += Allergen.FISH
                "shellfish", "crustaceans" -> allergens += Allergen.SHELLFISH
                "wheat" -> allergens += Allergen.WHEAT
                "soy", "soya" -> allergens += Allergen.SOY
                "sesame" -> allergens += Allergen.SESAME
                "gluten" -> allergens += Allergen.GLUTEN
            }
        }

        // Finally we build the domain object with parsed data
        return FoodItem(
            price = 0.0, // We didn't get a chance to implement prices so we just set it to 0
            name = obj.optString("product_name", ""),
            barcode = code,
            ingredients = ingredients,
            allergens = allergens,
            purchasedDate = "",
            expirationDate = " ",
            owners = mutableListOf(),
            imageUrl = imageUrl
        )
    }
}
