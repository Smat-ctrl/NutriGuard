package k.nutriguard.repository

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodRepositoryAndroidInstrumentedTest {

    @Test
    fun searchByName_TEST() = runBlocking {
        val repo = FoodRepository()
        val results = repo.searchByName("Nutella", 5)

        println("=== OpenFoodFacts results (${results.size}) ===")
        results.forEachIndexed { i, f ->
            println("#${i + 1}: barcode=${f.barcode} image=${f.imageUrl} allergens=${f.allergens} ingredients=${f.ingredients.take(5)}")
        }

        Log.d("FoodRepoTest", "Got ${results.size} items")
        results.forEachIndexed { i, f ->
            Log.d("FoodRepoTest", "#${i + 1}: $f")
        }
    }

}

