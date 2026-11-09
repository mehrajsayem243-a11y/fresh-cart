package com.example.data.barcode

import com.example.data.repository.FreshCartRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ScannedProduct(
    val barcode: String,
    val name: String,
    val category: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val brand: String = "",
    val storageLocation: String = "Pantry",
    val shelfLifeDays: Int = 7
)

class BarcodeProductService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Preloaded grocery barcodes dictionary for instant offline lookups & testing
    private val localDatabase = mapOf(
        "011110038493" to ("Organic Whole Milk" to "Dairy"),
        "021000658831" to ("Sharp Cheddar Cheese" to "Dairy"),
        "071514150334" to ("Plain Greek Yogurt" to "Dairy"),
        "072250037129" to ("Honey Wheat Bread" to "Bakery"),
        "073762806450" to ("Spaghetti Pasta" to "Pantry"),
        "070560000411" to ("Tomato & Basil Pasta Sauce" to "Pantry"),
        "018000001859" to ("Whole Grain Cereal" to "Pantry"),
        "030000061100" to ("Old Fashioned Rolled Oats" to "Pantry"),
        "041220272714" to ("Organic Baby Spinach" to "Produce"),
        "04011" to ("Fresh Bananas" to "Produce"),
        "04046" to ("Hass Avocados" to "Produce"),
        "04012" to ("Honeycrisp Apples" to "Produce"),
        "041789001901" to ("Roasted Turkey Breast" to "Meat & Seafood"),
        "023145678901" to ("Atlantic Salmon Fillet" to "Meat & Seafood"),
        "049000028904" to ("Sparkling Water" to "Beverages"),
        "025000057053" to ("100% Orange Juice" to "Beverages"),
        "014100085448" to ("Cheddar Crackers" to "Snacks"),
        "034000002405" to ("Dark Chocolate" to "Snacks"),
        "070847811169" to ("Energy Drink" to "Beverages"),
        "036200004018" to ("Organic Tomato Ketchup" to "Pantry")
    )

    val sampleDemoBarcodes: List<ScannedProduct> = localDatabase.map { (code, pair) ->
        val category = pair.second
        ScannedProduct(
            barcode = code,
            name = pair.first,
            category = category,
            quantity = 1.0,
            unit = if (category == "Produce" || category == "Dairy" && pair.first.contains("Milk")) "L" else "pcs",
            storageLocation = FreshCartRepository.getDefaultStorageLocation(category),
            shelfLifeDays = FreshCartRepository.getEstimatedShelfLifeDays(category)
        )
    }

    suspend fun lookupBarcode(barcodeRaw: String): ScannedProduct = withContext(Dispatchers.IO) {
        val cleanBarcode = barcodeRaw.trim().replace(Regex("[^0-9]"), "")

        // 1. Check local catalog first
        localDatabase[cleanBarcode]?.let { (name, category) ->
            return@withContext ScannedProduct(
                barcode = cleanBarcode,
                name = name,
                category = category,
                quantity = 1.0,
                unit = if (category == "Dairy" && name.contains("Milk")) "L" else "pcs",
                storageLocation = FreshCartRepository.getDefaultStorageLocation(category),
                shelfLifeDays = FreshCartRepository.getEstimatedShelfLifeDays(category)
            )
        }

        // Check suffix/prefix match
        val partialMatch = localDatabase.entries.firstOrNull { (k, _) -> cleanBarcode.endsWith(k) || k.endsWith(cleanBarcode) }
        if (partialMatch != null) {
            val (name, category) = partialMatch.value
            return@withContext ScannedProduct(
                barcode = cleanBarcode,
                name = name,
                category = category,
                quantity = 1.0,
                unit = if (category == "Dairy" && name.contains("Milk")) "L" else "pcs",
                storageLocation = FreshCartRepository.getDefaultStorageLocation(category),
                shelfLifeDays = FreshCartRepository.getEstimatedShelfLifeDays(category)
            )
        }

        // 2. Query Open Food Facts API
        try {
            val url = "https://world.openfoodfacts.org/api/v0/product/$cleanBarcode.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FreshCartGroceries-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val root = JSONObject(jsonStr)
                    if (root.optInt("status", 0) == 1) {
                        val product = root.getJSONObject("product")
                        val name = product.optString("product_name").ifBlank {
                            product.optString("product_name_en", "Grocery Item #$cleanBarcode")
                        }
                        val brand = product.optString("brands", "")
                        val categoryRaw = product.optString("categories", "")
                        val category = inferCategory(name, categoryRaw)
                        val quantityStr = product.optString("quantity", "")
                        val (qty, unit) = parseQuantityAndUnit(quantityStr)

                        return@withContext ScannedProduct(
                            barcode = cleanBarcode,
                            name = if (brand.isNotBlank() && !name.contains(brand, ignoreCase = true)) "$brand $name" else name,
                            category = category,
                            quantity = qty,
                            unit = unit,
                            brand = brand,
                            storageLocation = FreshCartRepository.getDefaultStorageLocation(category),
                            shelfLifeDays = FreshCartRepository.getEstimatedShelfLifeDays(category)
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Network fallback
        }

        // 3. Fallback generic product
        val fallbackName = "Scanned Item ($cleanBarcode)"
        val category = "Pantry"
        ScannedProduct(
            barcode = cleanBarcode,
            name = fallbackName,
            category = category,
            quantity = 1.0,
            unit = "pcs",
            storageLocation = FreshCartRepository.getDefaultStorageLocation(category),
            shelfLifeDays = FreshCartRepository.getEstimatedShelfLifeDays(category)
        )
    }

    private fun inferCategory(name: String, categoriesHierarchy: String): String {
        val combined = "$name $categoriesHierarchy".lowercase()
        return when {
            combined.contains("milk") || combined.contains("cheese") || combined.contains("yogurt") ||
                    combined.contains("butter") || combined.contains("dairy") -> "Dairy"
            combined.contains("fruit") || combined.contains("vegetable") || combined.contains("apple") ||
                    combined.contains("banana") || combined.contains("salad") || combined.contains("produce") -> "Produce"
            combined.contains("bread") || combined.contains("bakery") || combined.contains("bagel") ||
                    combined.contains("pastry") || combined.contains("toast") -> "Bakery"
            combined.contains("chicken") || combined.contains("meat") || combined.contains("beef") ||
                    combined.contains("fish") || combined.contains("salmon") || combined.contains("seafood") -> "Meat & Seafood"
            combined.contains("juice") || combined.contains("water") || combined.contains("soda") ||
                    combined.contains("tea") || combined.contains("coffee") || combined.contains("drink") -> "Beverages"
            combined.contains("chip") || combined.contains("snack") || combined.contains("cookie") ||
                    combined.contains("chocolate") || combined.contains("candy") -> "Snacks"
            combined.contains("ice cream") || combined.contains("frozen") || combined.contains("pizza") -> "Frozen"
            else -> "Pantry"
        }
    }

    private fun parseQuantityAndUnit(quantityStr: String): Pair<Double, String> {
        if (quantityStr.isBlank()) return 1.0 to "pcs"
        val lower = quantityStr.lowercase().trim()
        val numMatch = Regex("^([0-9]+(?:\\.[0-9]+)?)").find(lower)
        val num = numMatch?.value?.toDoubleOrNull() ?: 1.0
        val unit = when {
            lower.contains("kg") -> "kg"
            lower.contains(" g") || lower.endsWith("g") -> "g"
            lower.contains("ml") -> "ml"
            lower.contains("l") -> "L"
            lower.contains("oz") -> "oz"
            lower.contains("pack") -> "packs"
            lower.contains("can") -> "cans"
            else -> "pcs"
        }
        return num to unit
    }
}
