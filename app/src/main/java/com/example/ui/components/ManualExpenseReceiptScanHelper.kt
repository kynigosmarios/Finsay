package com.example.ui.components

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.entity.ExpenseCategory
import com.example.util.Localization
import com.example.util.ReceiptParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

internal data class ReceiptScanUiOutcome(
    val merchantAfm: String,
    val title: String,
    val amountText: String,
    val timestamp: Long,
    val matchedCategoryId: Long?,
    val toastMessage: String
)

internal object ManualExpenseReceiptScanHelper {

    fun decodeBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    suspend fun scanReceiptBitmap(
        bitmap: Bitmap,
        categories: List<ExpenseCategory>,
        lang: Localization.Language,
        onSuggestCategory: ((merchantOrText: String, merchantAfm: String) -> ExpenseCategory?)?
    ): ReceiptScanUiOutcome {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val aadeQr = com.example.util.ReceiptAmountAndQrParser.extractBestQrCode(bitmap)
        val resolvedQrData = if (!aadeQr.isNullOrBlank()) {
            com.example.util.ReceiptAmountAndQrParser.resolveAadeQrData(aadeQr)
        } else {
            null
        }

        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val visionText = withContext(Dispatchers.IO) { textRecognizer.process(inputImage).await() }

        val result = ReceiptParser.parseReceipt(
            visionText = visionText,
            rawText = visionText.text,
            aadeQr = aadeQr,
            availableCategories = categories,
            learnedCategorySuggester = { merchantTitle, merchantAfm ->
                onSuggestCategory?.invoke(merchantTitle, merchantAfm)?.name
            },
            preResolvedQrData = resolvedQrData
        )

        val matchedCategory = categories.find { it.name.equals(result.suggestedCategoryName, ignoreCase = true) }
        val amountFormatted = if (result.amount > 0.0) String.format(Locale.US, "%.2f", result.amount) else ""
        val msg = if (result.title.isNotBlank() || result.amount > 0.0) {
            if (lang == Localization.Language.ENGLISH) {
                "Receipt scanned: ${if (result.title.isNotBlank()) result.title else "Receipt"} (${if (result.amount > 0.0) "${amountFormatted}€" else ""})"
            } else {
                "Αναγνωρίστηκε: ${if (result.title.isNotBlank()) result.title else "Απόδειξη"} (${if (result.amount > 0.0) "${amountFormatted}€" else ""})"
            }
        } else {
            if (lang == Localization.Language.ENGLISH) "Could not detect receipt details" else "Δεν εντοπίστηκαν στοιχεία στην απόδειξη"
        }

        return ReceiptScanUiOutcome(
            merchantAfm = result.merchantAfm,
            title = if (result.title.isNotBlank() && result.title != "Απόδειξη") result.title else "",
            amountText = amountFormatted,
            timestamp = result.timestamp,
            matchedCategoryId = matchedCategory?.id,
            toastMessage = msg
        )
    }
}
