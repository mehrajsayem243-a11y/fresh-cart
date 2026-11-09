package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.GroceryItem
import com.example.data.local.entity.PantryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PantrySuggestion(
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val reason: String,
    var isSelected: Boolean = true
)

class GeminiPantryService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzePantryUsage(
        pantryItems: List<PantryItem>,
        currentGroceries: List<GroceryItem>
    ): List<PantrySuggestion> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            safeLog("API key missing or placeholder. Using heuristic fallback.")
            return@withContext generateLocalHeuristicSuggestions(pantryItems, currentGroceries)
        }

        try {
            val suggestions = callGeminiApi(apiKey, pantryItems, currentGroceries)
            if (suggestions.isNotEmpty()) suggestions else generateLocalHeuristicSuggestions(pantryItems, currentGroceries)
        } catch (e: Exception) {
            safeLog("Gemini API error, falling back to local analysis: ${e.message}")
            generateLocalHeuristicSuggestions(pantryItems, currentGroceries)
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        pantryItems: List<PantryItem>,
        currentGroceries: List<GroceryItem>
    ): List<PantrySuggestion> {
        val now = System.currentTimeMillis()

        val pantryJson = JSONArray().apply {
            pantryItems.forEach { item ->
                val daysLeft = TimeUnit.MILLISECONDS.toDays(item.expiryDate - now)
                put(JSONObject().apply {
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("category", item.category)
                    put("storageLocation", item.storageLocation)
                    put("daysUntilExpiry", daysLeft)
                    put("isLowStock", item.quantity <= item.lowStockThreshold)
                })
            }
        }

        val groceriesJson = JSONArray().apply {
            currentGroceries.forEach { item ->
                put(JSONObject().apply {
                    put("name", item.name)
                    put("isBought", item.isBought)
                })
            }
        }

        val prompt = """
            You are a smart kitchen pantry assistant.
            Analyze the following pantry inventory and current grocery shopping list:
            
            PANTRY ITEMS:
            ${pantryJson.toString(2)}
            
            CURRENT GROCERY LIST (do not suggest items already here):
            ${groceriesJson.toString(2)}
            
            TASK:
            1. Identify items in the pantry that are running low (quantity <= 1 or marked isLowStock), nearing expiry (daysUntilExpiry <= 3), or depleted.
            2. Infer complementary kitchen staples that are likely needed based on what is in the pantry (e.g., if coffee is present, check milk; if pasta is present, check sauce/cheese).
            3. Return a JSON array of recommended items to add to the grocery list.
            
            Return ONLY a valid JSON array of objects with the following schema:
            [
              {
                "name": "Item name",
                "quantity": 1.0,
                "unit": "pcs or L or kg or pack",
                "category": "Produce or Dairy or Bakery or Meat & Seafood or Pantry or Frozen or Beverages or Snacks",
                "reason": "Brief 1-sentence reason why this is needed now"
              }
            ]
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini")

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API call failed with code ${response.code}: $responseBody")
        }

        val root = JSONObject(responseBody)
        val text = root.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val results = mutableListOf<PantrySuggestion>()
        val parsedArray = JSONArray(text.trim())
        for (i in 0 until parsedArray.length()) {
            val obj = parsedArray.getJSONObject(i)
            results.add(
                PantrySuggestion(
                    name = obj.optString("name", "Unknown item"),
                    quantity = obj.optDouble("quantity", 1.0),
                    unit = obj.optString("unit", "pcs"),
                    category = obj.optString("category", "Pantry"),
                    reason = obj.optString("reason", "Replenish pantry stock"),
                    isSelected = true
                )
            )
        }
        return results
    }

    private fun generateLocalHeuristicSuggestions(
        pantryItems: List<PantryItem>,
        currentGroceries: List<GroceryItem>
    ): List<PantrySuggestion> {
        val existingGroceryNames = currentGroceries.map { it.name.lowercase().trim() }.toSet()
        val suggestions = mutableListOf<PantrySuggestion>()
        val now = System.currentTimeMillis()

        // 1. Items running low or expiring soon in pantry
        for (item in pantryItems) {
            val normalizedName = item.name.lowercase().trim()
            if (normalizedName in existingGroceryNames) continue

            val daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(item.expiryDate - now)
            val isLow = item.quantity <= item.lowStockThreshold

            if (isLow) {
                suggestions.add(
                    PantrySuggestion(
                        name = item.name,
                        quantity = if (item.quantity > 0) item.quantity else 1.0,
                        unit = item.unit,
                        category = item.category,
                        reason = "Running low in ${item.storageLocation} (${item.quantity} ${item.unit} left)",
                        isSelected = true
                    )
                )
            } else if (daysUntilExpiry <= 2) {
                suggestions.add(
                    PantrySuggestion(
                        name = item.name,
                        quantity = 1.0,
                        unit = item.unit,
                        category = item.category,
                        reason = "Expires in $daysUntilExpiry day(s), restock soon",
                        isSelected = true
                    )
                )
            }
        }

        // 2. Intelligent complement rules if list is short
        val pantryNames = pantryItems.map { it.name.lowercase() }
        if (pantryNames.any { it.contains("coffee") } && !pantryNames.any { it.contains("milk") } && "milk" !in existingGroceryNames) {
            suggestions.add(
                PantrySuggestion(
                    name = "Milk",
                    quantity = 1.0,
                    unit = "L",
                    category = "Dairy",
                    reason = "Pantry has coffee but no fresh milk detected",
                    isSelected = true
                )
            )
        }

        if (pantryNames.any { it.contains("pasta") } && !pantryNames.any { it.contains("sauce") } && "pasta sauce" !in existingGroceryNames) {
            suggestions.add(
                PantrySuggestion(
                    name = "Pasta Sauce",
                    quantity = 1.0,
                    unit = "cans",
                    category = "Pantry",
                    reason = "Pantry has pasta; pair with marinara or pesto sauce",
                    isSelected = true
                )
            )
        }

        // If pantry is completely empty, provide foundational staples
        if (suggestions.isEmpty() && pantryItems.isEmpty()) {
            suggestions.add(
                PantrySuggestion(
                    name = "Olive Oil",
                    quantity = 1.0,
                    unit = "L",
                    category = "Pantry",
                    reason = "Essential kitchen cooking oil",
                    isSelected = true
                )
            )
            suggestions.add(
                PantrySuggestion(
                    name = "Eggs",
                    quantity = 12.0,
                    unit = "pcs",
                    category = "Dairy",
                    reason = "High-protein daily kitchen staple",
                    isSelected = true
                )
            )
            suggestions.add(
                PantrySuggestion(
                    name = "Fresh Bread",
                    quantity = 1.0,
                    unit = "packs",
                    category = "Bakery",
                    reason = "Fresh breakfast and meal staple",
                    isSelected = true
                )
            )
        }

        return suggestions
    }

    private fun safeLog(msg: String) {
        try {
            Log.d("GeminiPantryService", msg)
        } catch (_: Throwable) {
            println("[GeminiPantryService] $msg")
        }
    }
}
