package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale

/**
 * Generates and parses Finsay household invite links & scannable QR codes.
 * Supported link formats:
 * - https://oikonomos-af58b.web.app/join?code=FSY-XXXXXXXX
 * - finsay://join?code=FSY-XXXXXXXX
 * - Raw code string: FSY-XXXXXXXX or OIKO-1234
 */
object QrCodeHelper {
    private const val TAG = "QrCodeHelper"
    const val INVITE_WEB_BASE_URL = "https://oikonomos-af58b.web.app/join"
    const val INVITE_APP_SCHEME = "finsay"
    const val INVITE_APP_HOST = "join"
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val MAX_SCAN_BITMAP_DIM = 1600
    private val secureRandom = SecureRandom()

    fun generateSecureInviteCode(): String {
        val sb = StringBuilder("FSY-")
        repeat(8) {
            sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
        }
        return sb.toString()
    }

    fun buildInviteUrl(code: String): String {
        val clean = normalizeInviteCode(code)
        return "$INVITE_WEB_BASE_URL?code=${Uri.encode(clean)}"
    }

    fun normalizeInviteCode(raw: String): String {
        return raw.trim().uppercase(Locale.ROOT).replace(" ", "")
    }

    /**
     * Extracts a valid household invite code from a deep-link URL, QR payload, or raw typed string.
     * Returns null if no valid code is found.
     */
    fun extractInviteCode(rawInput: String?): String? {
        val trimmed = rawInput?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        // 1. Check URL query param (?code=...)
        if (trimmed.contains("://") || trimmed.startsWith("http", ignoreCase = true) || trimmed.startsWith("finsay:", ignoreCase = true)) {
            val regexMatch = Regex("[?&]code=([A-Za-z0-9_-]{6,32})", RegexOption.IGNORE_CASE).find(trimmed)
            if (regexMatch != null) {
                return normalizeInviteCode(regexMatch.groupValues[1])
            }
            try {
                val uri = Uri.parse(trimmed)
                val param = uri.getQueryParameter("code")
                if (!param.isNullOrBlank() && param.trim().length in 6..32) {
                    return normalizeInviteCode(param)
                }
            } catch (_: Throwable) {}
        }

        // 2. Check embedded FSY-XXXXXXXX or OIKO-XXXX pattern in shared text
        val embedded = Regex("\\b((?:FSY|OIKO)-[A-Za-z0-9]{4,16})\\b", RegexOption.IGNORE_CASE).find(trimmed)
        if (embedded != null) {
            return normalizeInviteCode(embedded.groupValues[1])
        }

        // 3. Raw code directly typed by user (alphanumeric + hyphen, 6..24 chars)
        val clean = normalizeInviteCode(trimmed)
        if (clean.length in 6..24 && clean.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            return clean
        }
        return null
    }

    /**
     * Generates a crisp high-contrast QR Code Bitmap for [content] using ZXing Core.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "generateQrBitmap error: ${t.message}")
            null
        }
    }

    /**
     * Scans a QR code from a content/file [Uri] (camera capture or Photo Picker).
     * First tries ML Kit Barcode Scanning via InputImage.fromFilePath (handles EXIF orientation),
     * then falls back to safely downsampled bundled ZXing QRCodeReader so scanning works offline
     * and even before unbundled Play Services ML Kit modules finish downloading.
     */
    suspend fun scanInviteCodeFromUri(context: Context, uri: Uri): String? {
        val mlKitResult = try {
            val image = InputImage.fromFilePath(context, uri)
            scanWithMlKit(image)
        } catch (t: Throwable) {
            Log.w(TAG, "scanInviteCodeFromUri ML Kit fallback: ${t.message}")
            null
        }
        if (!mlKitResult.isNullOrBlank()) return mlKitResult

        val bitmap = decodeDownsampledBitmap(context, uri, MAX_SCAN_BITMAP_DIM) ?: return null
        return try {
            decodeQrWithZxing(bitmap) ?: scanWithMlKit(InputImage.fromBitmap(bitmap, 0))
        } finally {
            if (!bitmap.isRecycled) {
                try { bitmap.recycle() } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Scans a QR code from a captured/picked [Bitmap] using ML Kit with bundled ZXing fallback
     * and returns the extracted invite code if valid.
     */
    suspend fun scanInviteCodeFromBitmap(bitmap: Bitmap): String? {
        val mlKitResult = scanWithMlKit(InputImage.fromBitmap(bitmap, 0))
        if (!mlKitResult.isNullOrBlank()) return mlKitResult
        return decodeQrWithZxing(bitmap)
    }

    private suspend fun scanWithMlKit(image: InputImage): String? {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        return try {
            val barcodes = scanner.process(image).await()
            barcodes.firstNotNullOfOrNull { barcode ->
                extractInviteCode(barcode.rawValue ?: barcode.displayValue)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "scanWithMlKit error: ${t.message}")
            null
        } finally {
            try { scanner.close() } catch (_: Throwable) {}
        }
    }

    private fun decodeDownsampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDim || (bounds.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "decodeDownsampledBitmap error: ${t.message}")
            null
        }
    }

    private fun decodeQrWithZxing(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        val reader = QRCodeReader()
        val candidates = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
            BinaryBitmap(HybridBinarizer(source.invert()))
        )
        for (binaryBitmap in candidates) {
            try {
                reader.reset()
                val rawText = reader.decode(binaryBitmap, hints)?.text
                val extracted = extractInviteCode(rawText)
                if (!extracted.isNullOrBlank()) return extracted
            } catch (_: Throwable) {
                // Try next binarizer strategy
            }
        }
        return null
    }
}
