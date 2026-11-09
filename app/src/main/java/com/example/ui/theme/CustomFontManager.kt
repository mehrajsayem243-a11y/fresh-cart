package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import java.io.File
import java.io.FileOutputStream

enum class AppFontPreset(val displayName: String, val subtitle: String) {
    SYSTEM("System Default", "Native Android sans-serif"),
    OUTFIT("Outfit", "Modern geometric expressive typography"),
    PLUS_JAKARTA("Plus Jakarta Sans", "Crisp high-readability sans"),
    FREDOKA("Fredoka", "Playful rounded friendly aesthetic"),
    IMPORTED("Imported Font", "Custom TTF / OTF font from device storage")
}

object CustomFontManager {

    private const val IMPORTED_FONT_FILENAME = "custom_font.ttf"

    fun hasImportedFont(context: Context): Boolean {
        val file = File(context.filesDir, IMPORTED_FONT_FILENAME)
        return file.exists() && file.length() > 0
    }

    fun getImportedFontName(context: Context): String {
        val pref = context.getSharedPreferences("freshcart_prefs", Context.MODE_PRIVATE)
        return pref.getString("imported_font_name", "custom_font.ttf") ?: "custom_font.ttf"
    }

    fun importFontFromUri(context: Context, uri: Uri, originalName: String? = null): Boolean {
        return try {
            val destinationFile = File(context.filesDir, IMPORTED_FONT_FILENAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Verify typeface can be loaded
            val typeface = Typeface.createFromFile(destinationFile)
            if (typeface != null) {
                val pref = context.getSharedPreferences("freshcart_prefs", Context.MODE_PRIVATE)
                val safeName = originalName ?: uri.lastPathSegment ?: "Imported Font"
                pref.edit()
                    .putString("imported_font_name", safeName)
                    .apply()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun resolveFontFamily(context: Context, preset: AppFontPreset): FontFamily {
        return when (preset) {
            AppFontPreset.SYSTEM -> FontFamily.SansSerif
            AppFontPreset.OUTFIT -> FontFamily(
                Font(R.font.outfit, FontWeight.Normal),
                Font(R.font.outfit, FontWeight.Bold)
            )
            AppFontPreset.PLUS_JAKARTA -> FontFamily(
                Font(R.font.plus_jakarta_sans, FontWeight.Normal),
                Font(R.font.plus_jakarta_sans, FontWeight.Bold)
            )
            AppFontPreset.FREDOKA -> FontFamily(
                Font(R.font.fredoka, FontWeight.Normal),
                Font(R.font.fredoka, FontWeight.Bold)
            )
            AppFontPreset.IMPORTED -> {
                val fontFile = File(context.filesDir, IMPORTED_FONT_FILENAME)
                if (fontFile.exists() && fontFile.length() > 0) {
                    try {
                        val typeface = Typeface.createFromFile(fontFile)
                        if (typeface != null) {
                            FontFamily(typeface)
                        } else {
                            FontFamily.SansSerif
                        }
                    } catch (_: Exception) {
                        FontFamily.SansSerif
                    }
                } else {
                    FontFamily.SansSerif
                }
            }
        }
    }
}
