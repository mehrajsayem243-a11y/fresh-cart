package com.example

import com.example.data.ai.GeminiPantryService
import com.example.data.barcode.BarcodeProductService
import com.example.data.local.entity.GroceryItem
import com.example.data.local.entity.PantryItem
import com.example.data.repository.FreshCartRepository
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppFontPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExampleUnitTest {
    @Test
    fun testEstimatedShelfLife() {
        assertEquals(7, FreshCartRepository.getEstimatedShelfLifeDays("Dairy"))
        assertEquals(3, FreshCartRepository.getEstimatedShelfLifeDays("Meat & Seafood"))
        assertEquals(5, FreshCartRepository.getEstimatedShelfLifeDays("Produce"))
        assertEquals(60, FreshCartRepository.getEstimatedShelfLifeDays("Pantry"))
        assertEquals(90, FreshCartRepository.getEstimatedShelfLifeDays("Frozen"))
    }

    @Test
    fun testStorageLocationDefaults() {
        assertEquals("Fridge", FreshCartRepository.getDefaultStorageLocation("Dairy"))
        assertEquals("Freezer", FreshCartRepository.getDefaultStorageLocation("Frozen"))
        assertEquals("Pantry", FreshCartRepository.getDefaultStorageLocation("Pantry"))
    }

    @Test
    fun testGeminiPantryHeuristicSuggestions() = runBlocking {
        val service = GeminiPantryService()
        val lowItem = PantryItem(
            id = 1,
            name = "Greek Yogurt",
            quantity = 0.5,
            unit = "cups",
            category = "Dairy",
            storageLocation = "Fridge",
            lowStockThreshold = 1.0
        )
        val groceries = listOf<GroceryItem>()

        val suggestions = service.analyzePantryUsage(listOf(lowItem), groceries)
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.name == "Greek Yogurt" })
    }

    @Test
    fun testBarcodeProductServiceLookup() = runBlocking {
        val barcodeService = BarcodeProductService()
        // Test local known barcode for milk
        val milkProduct = barcodeService.lookupBarcode("011110038493")
        assertEquals("Organic Whole Milk", milkProduct.name)
        assertEquals("Dairy", milkProduct.category)
        assertEquals("Fridge", milkProduct.storageLocation)

        // Test known barcode for pasta
        val pastaProduct = barcodeService.lookupBarcode("073762806450")
        assertEquals("Spaghetti Pasta", pastaProduct.name)
        assertEquals("Pantry", pastaProduct.category)
    }

    @Test
    fun testColorPaletteCount() {
        // 5 original palettes + 23 requested = 28 total palettes
        assertEquals(28, AppColorPalette.entries.size)
        assertTrue(AppColorPalette.entries.any { it.name == "TEAL" })
        assertTrue(AppColorPalette.entries.any { it.name == "EMERALD" })
        assertTrue(AppColorPalette.entries.any { it.name == "CYAN" })
        assertTrue(AppColorPalette.entries.any { it.name == "CHARCOAL" })
    }

    @Test
    fun testFontPresets() {
        assertEquals(5, AppFontPreset.entries.size)
        assertTrue(AppFontPreset.entries.any { it == AppFontPreset.SYSTEM })
        assertTrue(AppFontPreset.entries.any { it == AppFontPreset.OUTFIT })
        assertTrue(AppFontPreset.entries.any { it == AppFontPreset.PLUS_JAKARTA })
        assertTrue(AppFontPreset.entries.any { it == AppFontPreset.FREDOKA })
        assertTrue(AppFontPreset.entries.any { it == AppFontPreset.IMPORTED })
    }

    @Test
    fun testPantryUrgentExpiryCalculation() {
        val now = System.currentTimeMillis()
        val expiringTomorrow = PantryItem(
            name = "Milk",
            expiryDate = now + TimeUnit.DAYS.toMillis(1)
        )
        val expiringNextMonth = PantryItem(
            name = "Canned Corn",
            expiryDate = now + TimeUnit.DAYS.toMillis(30)
        )

        val daysRemaining1 = TimeUnit.MILLISECONDS.toDays(expiringTomorrow.expiryDate - now).toInt()
        val daysRemaining2 = TimeUnit.MILLISECONDS.toDays(expiringNextMonth.expiryDate - now).toInt()

        assertTrue(daysRemaining1 <= 2)
        assertTrue(daysRemaining2 > 2)
    }

    @Test
    fun testGroceryItemNewAttributes() {
        val item = GroceryItem(
            name = "Greek Yogurt",
            brand = "Chobani",
            price = 4.99,
            priority = "Urgent",
            isFavorite = true
        )
        assertEquals("Chobani", item.brand)
        assertEquals(4.99, item.price, 0.001)
        assertEquals("Urgent", item.priority)
        assertTrue(item.isFavorite)
    }

    @Test
    fun testQrBarcodeMatrixGenerator() {
        val qrMatrix = com.example.data.barcode.QrBarcodeMatrixGenerator.generateQrMatrix("073762806450", 256)
        assertTrue(qrMatrix != null)
        assertTrue(qrMatrix!!.width > 0)
        assertTrue(qrMatrix.height > 0)

        val barcode1dMatrix = com.example.data.barcode.QrBarcodeMatrixGenerator.generate1DBarcodeMatrix("073762806450", 300, 100)
        assertTrue(barcode1dMatrix != null)
        assertTrue(barcode1dMatrix!!.width > 0)
    }

    @Test
    fun testAppSettingEntityAndPalettePersistence() {
        val setting = com.example.data.local.entity.AppSetting(
            key = "color_palette",
            value = AppColorPalette.TEAL.name
        )
        assertEquals("color_palette", setting.key)
        assertEquals("TEAL", setting.value)
        val restoredPalette = AppColorPalette.valueOf(setting.value)
        assertEquals(AppColorPalette.TEAL, restoredPalette)
        assertEquals("Deep Teal", restoredPalette.displayName)
    }
}
