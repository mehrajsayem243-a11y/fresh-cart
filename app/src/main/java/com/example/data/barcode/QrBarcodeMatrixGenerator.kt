package com.example.data.barcode

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter

import com.google.zxing.common.BitMatrix

object QrBarcodeMatrixGenerator {

    /**
     * Generates a square QR Code BitMatrix using pure ZXing logic.
     */
    fun generateQrMatrix(content: String, size: Int = 512): BitMatrix? {
        if (content.isBlank()) return null
        return try {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a 1D Barcode BitMatrix using pure ZXing logic.
     */
    fun generate1DBarcodeMatrix(content: String, width: Int = 600, height: Int = 200): BitMatrix? {
        if (content.isBlank()) return null
        return try {
            Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a square QR Code BitMatrix and converts it to a Compose ImageBitmap.
     */
    fun generateQrCodeBitmap(content: String, size: Int = 512): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a 1D Barcode BitMatrix (Code 128) and converts it to a Compose ImageBitmap.
     */
    fun generate1DBarcodeBitmap(content: String, width: Int = 600, height: Int = 200): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val bitMatrix = Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
