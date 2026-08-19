package com.smartmeasure.gz

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartmeasure.gz.databinding.ActivityScanMeasurementsBinding
import java.text.DecimalFormat

class ScanMeasurementsActivity : AppCompatActivity() {

    private lateinit var b: ActivityScanMeasurementsBinding

    private val measurements =
        mutableListOf<MeasurementItem>()

    private val formatter =
        DecimalFormat("#.##")

    private val recognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    // =====================================================
    // اختيار صورة من المعرض
    // =====================================================

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                b.previewImage.setImageURI(uri)

                readImageFromUri(uri)
            }
        }

    // =====================================================
    // التقاط صورة بالكاميرا
    // =====================================================

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            if (bitmap != null) {

                b.previewImage.setImageBitmap(bitmap)

                readImageFromBitmap(bitmap)
            }
        }

    // =====================================================
    // طلب صلاحية الكاميرا
    // =====================================================

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                cameraLauncher.launch(null)

            } else {

                Toast.makeText(
                    this,
                    "يجب السماح باستخدام الكاميرا لتصوير ورقة المقاسات",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // =====================================================
    // بدء الشاشة
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivityScanMeasurementsBinding.inflate(
                layoutInflater
            )

        setContentView(b.root)

        setupButtons()
    }

    // =====================================================
    // الأزرار
    // =====================================================

    private fun setupButtons() {

        b.selectImageBtn.setOnClickListener {

            showImageSourceDialog()
        }

        b.applyAllBtn.setOnClickListener {

            applyAdjustmentToAll()
        }

        b.resetAdjustmentBtn.setOnClickListener {

            resetAdjustments()
        }
    }

    // =====================================================
    // اختيار كاميرا أو معرض
    // =====================================================

    private fun showImageSourceDialog() {

        val options =
            arrayOf(
                "📷 تصوير ورقة بالكاميرا",
                "🖼 اختيار صورة من المعرض"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "قراءة ورقة المقاسات"
            )
            .setItems(
                options
            ) { _, which ->

                when (which) {

                    0 ->
                        openCamera()

                    1 ->
                        imagePicker.launch(
                            "image/*"
                        )
                }
            }
            .setNegativeButton(
                "إلغاء",
                null
            )
            .show()
    }

    // =====================================================
    // فتح الكاميرا
    // =====================================================

    private fun openCamera() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {

            cameraLauncher.launch(null)

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // =====================================================
    // قراءة صورة من المعرض
    // =====================================================

    private fun readImageFromUri(
        uri: Uri
    ) {

        try {

            val image =
                InputImage.fromFilePath(
                    this,
                    uri
                )

            processImage(image)

        } catch (e: Exception) {

            b.statusText.text =
                "حدث خطأ أثناء فتح الصورة"

            Toast.makeText(
                this,
                "تعذر فتح الصورة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =====================================================
    // قراءة صورة الكاميرا
    // =====================================================

    private fun readImageFromBitmap(
        bitmap: Bitmap
    ) {

        val image =
            InputImage.fromBitmap(
                bitmap,
                0
            )

        processImage(image)
    }

    // =====================================================
    // إرسال الصورة إلى قارئ النص
    // =====================================================

    private fun processImage(
        image: InputImage
    ) {

        b.statusText.text =
            "جاري قراءة ورقة المقاسات..."

        recognizer
            .process(image)
            .addOnSuccessListener { result ->

                b.recognizedText.text =
                    result.text

                parseMeasurements(
                    result.text
                )
            }
            .addOnFailureListener {

                b.statusText.text =
                    "تعذر قراءة الصورة"

                Toast.makeText(
                    this,
                    "تأكد من وضوح الورقة والإضاءة ثم حاول مرة أخرى",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // تحليل النص واستخراج المقاسات
    // =====================================================

    private fun parseMeasurements(
        rawText: String
    ) {

        measurements.clear()

        val normalized =
            normalizeNumbers(
                rawText
            )

        val lines =
            normalized.lines()

        for (line in lines) {

            parseLine(line)
        }

        if (measurements.isEmpty()) {

            b.statusText.text =
                "لم يتم العثور على مقاسات واضحة"

            renderTables()

            return
        }

        b.statusText.text =
            "تم العثور على ${measurements.size} مقاس"

        renderTables()
    }

    // =====================================================
    // قراءة سطر واحد
    // =====================================================

    private fun parseLine(
        originalLine: String
    ) {

        var line =
            originalLine
                .lowercase()
                .replace("×", "x")
                .replace("*", "x")
                .replace("X", "x")

        line =
            line.replace(
                Regex("\\s+"),
                " "
            )

        /*
         أمثلة:

         120x80
         120 x 80
         120 × 80
         120x80 عدد 2
        */

        val pattern =
            Regex(
                """(\d+(?:\.\d+)?)\s*x\s*(\d+(?:\.\d+)?)(?:.*?(?:عدد|qty|quantity)\s*(\d+))?"""
            )

        val match =
            pattern.find(line)
                ?: return

        val length =
            match.groupValues[1]
                .toDoubleOrNull()
                ?: return

        val width =
            match.groupValues[2]
                .toDoubleOrNull()
                ?: return

        var quantity = 1

        val quantityText =
            match.groupValues
                .getOrNull(3)
                .orEmpty()

        if (
            quantityText.isNotBlank()
        ) {

            quantity =
                quantityText
                    .toIntOrNull()
                    ?: 1
        }

        if (
            length <= 0 ||
            width <= 0 ||
            quantity <= 0
        ) {

            return
        }

        measurements.add(
            MeasurementItem(
                length = length,
                width = width,
                quantity = quantity,
                unit = "سم"
            )
        )
    }

    // =====================================================
    // تحويل الأرقام العربية إلى إنجليزية
    // =====================================================

    private fun normalizeNumbers(
        text: String
    ): String {

        return text
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
            .replace('۰', '0')
            .replace('۱', '1')
            .replace('۲', '2')
            .replace('۳', '3')
            .replace('۴', '4')
            .replace('۵', '5')
            .replace('۶', '6')
            .replace('۷', '7')
            .replace('۸', '8')
            .replace('۹', '9')
            .replace('٫', '.')
    }

    // =====================================================
    // تطبيق زيادة أو تنقيص على جميع مقاسات الورقة
    // =====================================================

    private fun applyAdjustmentToAll() {

        if (measurements.isEmpty()) {

            Toast.makeText(
                this,
                "لا توجد مقاسات لتعديلها",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val lengthAdjustment =
            b.lengthAdjustmentInput.text
                .toString()
                .trim()
                .toDoubleOrNull()
                ?: 0.0

        val widthAdjustment =
            b.widthAdjustmentInput.text
                .toString()
                .trim()
                .toDoubleOrNull()
                ?: 0.0

        for (item in measurements) {

            if (b.subtractRadio.isChecked) {

                item.adjustedLength =
                    item.length -
                        lengthAdjustment

                item.adjustedWidth =
                    item.width -
                        widthAdjustment

            } else {

                item.adjustedLength =
                    item.length +
                        lengthAdjustment

                item.adjustedWidth =
                    item.width +
                        widthAdjustment
            }

            if (
                item.adjustedLength <= 0 ||
                item.adjustedWidth <= 0
            ) {

                Toast.makeText(
                    this,
                    "قيمة التعديل كبيرة بالنسبة لبعض المقاسات",
                    Toast.LENGTH_LONG
                ).show()

                resetAdjustments()

                return
            }
        }

        renderTables()

        Toast.makeText(
            this,
            "تم تطبيق التعديل على جميع مقاسات الورقة",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // إرجاع المقاسات الأصلية
    // =====================================================

    private fun resetAdjustments() {

        for (item in measurements) {

            item.adjustedLength =
                item.length

            item.adjustedWidth =
                item.width
        }

        b.lengthAdjustmentInput.text?.clear()

        b.widthAdjustmentInput.text?.clear()

        b.subtractRadio.isChecked =
            true

        renderTables()
    }

    // =====================================================
    // رسم الجدولين
    // =====================================================

    private fun renderTables() {

        while (
            b.originalTable.childCount > 1
        ) {

            b.originalTable.removeViewAt(1)
        }

        while (
            b.adjustedTable.childCount > 1
        ) {

            b.adjustedTable.removeViewAt(1)
        }

        var originalTotal =
            0.0

        var adjustedTotal =
            0.0

        for (
            (index, item) in
            measurements.withIndex()
        ) {

            val originalArea =
                calculateArea(
                    item.length,
                    item.width,
                    item.quantity
                )

            val adjustedArea =
                calculateArea(
                    item.adjustedLength,
                    item.adjustedWidth,
                    item.quantity
                )

            originalTotal +=
                originalArea

            adjustedTotal +=
                adjustedArea

            b.originalTable.addView(
                createRow(
                    index + 1,
                    item.length,
                    item.width,
                    item.quantity,
                    originalArea
                )
            )

            b.adjustedTable.addView(
                createRow(
                    index + 1,
                    item.adjustedLength,
                    item.adjustedWidth,
                    item.quantity,
                    adjustedArea
                )
            )
        }

        b.originalTotalText.text =
            "إجمالي الأصلي: " +
                formatter.format(
                    originalTotal
                ) +
                " م²"

        b.adjustedTotalText.text =
            "إجمالي بعد التعديل: " +
                formatter.format(
                    adjustedTotal
                ) +
                " م²"
    }

    // =====================================================
    // حساب المتر المربع
    // =====================================================

    private fun calculateArea(
        length: Double,
        width: Double,
        quantity: Int
    ): Double {

        return (
            length / 100.0
        ) * (
            width / 100.0
        ) * quantity
    }

    // =====================================================
    // إنشاء صف
    // =====================================================

    private fun createRow(
        number: Int,
        length: Double,
        width: Double,
        quantity: Int,
        area: Double
    ): TableRow {

        val row =
            TableRow(this)

        row.addView(
            createCell(
                number.toString()
            )
        )

        row.addView(
            createCell(
                formatter.format(
                    length
                )
            )
        )

        row.addView(
            createCell(
                formatter.format(
                    width
                )
            )
        )

        row.addView(
            createCell(
                quantity.toString()
            )
        )

        row.addView(
            createCell(
                formatter.format(
                    area
                )
            )
        )

        return row
    }

    // =====================================================
    // خلية داخل الجدول
    // =====================================================

    private fun createCell(
        value: String
    ): TextView {

        return TextView(this).apply {

            text =
                value

            gravity =
                Gravity.CENTER

            setPadding(
                12,
                16,
                12,
                16
            )
        }
    }

    // =====================================================
    // إغلاق قارئ النص
    // =====================================================

    override fun onDestroy() {

        recognizer.close()

        super.onDestroy()
    }
}
