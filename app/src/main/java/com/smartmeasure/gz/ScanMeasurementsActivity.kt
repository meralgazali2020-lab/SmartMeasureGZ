package com.smartmeasure.gz

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartmeasure.gz.databinding.ActivityScanMeasurementsBinding
import java.io.File
import java.text.DecimalFormat

class ScanMeasurementsActivity : AppCompatActivity() {

    private lateinit var b: ActivityScanMeasurementsBinding

    private val measurements =
        mutableListOf<MeasurementItem>()

    private val formatter =
        DecimalFormat("#.##")

    private var cameraImageUri: Uri? =
        null

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

                b.previewImage.setImageURI(
                    uri
                )

                readImageFromUri(
                    uri
                )
            }
        }

    // =====================================================
    // تصوير صورة كاملة الجودة
    // =====================================================

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                val uri =
                    cameraImageUri

                if (uri != null) {

                    b.previewImage.setImageURI(
                        uri
                    )

                    readImageFromUri(
                        uri
                    )
                }

            } else {

                b.statusText.text =
                    "تم إلغاء التصوير"
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

                launchFullResolutionCamera()

            } else {

                Toast.makeText(
                    this,
                    "يجب السماح باستخدام الكاميرا",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // =====================================================
    // بداية الشاشة
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivityScanMeasurementsBinding.inflate(
                layoutInflater
            )

        setContentView(
            b.root
        )

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

        b.saveProjectBtn.setOnClickListener {

            saveProject()
        }
    }

    // =====================================================
    // اختيار مصدر الصورة
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

            launchFullResolutionCamera()

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // =====================================================
    // إنشاء ملف الصورة ثم فتح الكاميرا
    // =====================================================

    private fun launchFullResolutionCamera() {

        try {

            val cameraDirectory =
                File(
                    cacheDir,
                    "camera"
                )

            if (
                !cameraDirectory.exists()
            ) {

                cameraDirectory.mkdirs()
            }

            val imageFile =
                File.createTempFile(
                    "smart_measure_",
                    ".jpg",
                    cameraDirectory
                )

            cameraImageUri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    imageFile
                )

            cameraLauncher.launch(
                cameraImageUri
            )

        } catch (e: Exception) {

            b.statusText.text =
                "تعذر فتح الكاميرا"

            Toast.makeText(
                this,
                "حدث خطأ أثناء تجهيز صورة الكاميرا",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // قراءة الصورة
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

            processImage(
                image
            )

        } catch (e: Exception) {

            b.statusText.text =
                "حدث خطأ أثناء فتح الصورة"

            Toast.makeText(
                this,
                "تعذر تجهيز الصورة للقراءة",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // OCR
    // =====================================================

    private fun processImage(
        image: InputImage
    ) {

        b.statusText.text =
            "جاري قراءة ورقة المقاسات..."

        recognizer
            .process(
                image
            )

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
                    "حاول تصوير الورقة بشكل مستقيم وبإضاءة واضحة",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // استخراج المقاسات من النص
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
            normalized
                .lines()
                .map {
                    cleanLine(
                        it
                    )
                }
                .filter {
                    it.isNotBlank()
                }

        // =================================================
        // نحاول قراءة كل سطر كمقاس مستقل
        // =================================================

        for (
            line in
            lines
        ) {

            parseLine(
                line
            )
        }

        // =================================================
        // إذا فشل تقسيم الأسطر نحاول النص كاملاً
        // =================================================

        if (
            measurements.isEmpty()
        ) {

            parseWholeText(
                normalized
            )
        }

        // =================================================
        // ترقيم العمليات ١، ٢، ٣...
        // =================================================

        measurements.forEachIndexed {
                index,
                item ->

            item.operationNumber =
                index + 1
        }

        if (
            measurements.isEmpty()
        ) {

            b.statusText.text =
                "لم يتم العثور على مقاسات واضحة"

            renderTables()

            return
        }

        b.statusText.text =
            "تم العثور على ${
                toArabicNumber(
                    measurements.size
                )
            } عملية"

        renderTables()
    }

    // =====================================================
    // تنظيف النص
    // =====================================================

    private fun cleanLine(
        value: String
    ): String {

        return value
            .lowercase()
            .replace(
                "×",
                "x"
            )
            .replace(
                "х",
                "x"
            )
            .replace(
                "X",
                "x"
            )
            .replace(
                "*",
                "x"
            )
            .replace(
                "✕",
                "x"
            )
            .replace(
                "✖",
                "x"
            )
            .replace(
                "ـ",
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    // =====================================================
    // قراءة سطر واحد
    // =====================================================

    private fun parseLine(
        originalLine: String
    ) {

        val line =
            cleanLine(
                originalLine
            )

        // =================================================
        // 120 × 80
        // 120 x 80
        // 120 * 80
        // 120 x 80 عدد 2
        // =================================================

        val explicitPattern =
            Regex(
                """(\d+(?:\.\d+)?)\s*x\s*(\d+(?:\.\d+)?)(?:.*?(?:عدد|qty|quantity|pcs|piece)\s*[:=]?\s*(\d+))?"""
            )

        val explicitMatch =
            explicitPattern.find(
                line
            )

        if (
            explicitMatch != null
        ) {

            val length =
                explicitMatch
                    .groupValues[1]
                    .toDoubleOrNull()
                    ?: return

            val width =
                explicitMatch
                    .groupValues[2]
                    .toDoubleOrNull()
                    ?: return

            val quantityText =
                explicitMatch
                    .groupValues
                    .getOrNull(3)
                    .orEmpty()

            val quantity =
                quantityText
                    .toIntOrNull()
                    ?: 1

            addMeasurement(
                length =
                    length,

                width =
                    width,

                quantity =
                    quantity
            )

            return
        }

        // =================================================
        // 120 - 80
        // 120 / 80
        // 120 : 80
        // =================================================

        val separatedPattern =
            Regex(
                """(?:^|\s)(\d+(?:\.\d+)?)\s*[-/:|]\s*(\d+(?:\.\d+)?)(?:\s+(\d+))?(?:\s|$)"""
            )

        val separatedMatch =
            separatedPattern.find(
                line
            )

        if (
            separatedMatch != null
        ) {

            val length =
                separatedMatch
                    .groupValues[1]
                    .toDoubleOrNull()
                    ?: return

            val width =
                separatedMatch
                    .groupValues[2]
                    .toDoubleOrNull()
                    ?: return

            val quantity =
                separatedMatch
                    .groupValues
                    .getOrNull(3)
                    .orEmpty()
                    .toIntOrNull()
                    ?: 1

            addMeasurement(
                length =
                    length,

                width =
                    width,

                quantity =
                    quantity
            )

            return
        }

        // =================================================
        // مثال OCR:
        // 120 80
        // بدون علامة ×
        // =================================================

        val numbers =
            Regex(
                """\d+(?:\.\d+)?"""
            )
                .findAll(
                    line
                )
                .mapNotNull {
                    it.value
                        .toDoubleOrNull()
                }
                .toList()

        if (
            numbers.size == 2
        ) {

            addMeasurement(
                length =
                    numbers[0],

                width =
                    numbers[1],

                quantity =
                    1
            )

            return
        }

        // =================================================
        // 120 80 2
        // طول - عرض - عدد
        // =================================================

        if (
            numbers.size == 3
        ) {

            val possibleQuantity =
                numbers[2]
                    .toInt()

            if (
                numbers[2] ==
                possibleQuantity.toDouble() &&
                possibleQuantity in 1..999
            ) {

                addMeasurement(
                    length =
                        numbers[0],

                    width =
                        numbers[1],

                    quantity =
                        possibleQuantity
                )
            }
        }
    }

    // =====================================================
    // البحث داخل النص كاملاً
    // =====================================================

    private fun parseWholeText(
        rawText: String
    ) {

        val text =
            cleanLine(
                rawText
            )

        val pattern =
            Regex(
                """(\d+(?:\.\d+)?)\s*x\s*(\d+(?:\.\d+)?)"""
            )

        val matches =
            pattern.findAll(
                text
            )

        for (
            match in
            matches
        ) {

            val length =
                match
                    .groupValues[1]
                    .toDoubleOrNull()
                    ?: continue

            val width =
                match
                    .groupValues[2]
                    .toDoubleOrNull()
                    ?: continue

            addMeasurement(
                length =
                    length,

                width =
                    width,

                quantity =
                    1
            )
        }
    }

    // =====================================================
    // إضافة عملية للقائمة
    // =====================================================

    private fun addMeasurement(
        length: Double,
        width: Double,
        quantity: Int
    ) {

        if (
            length <= 0 ||
            width <= 0 ||
            quantity <= 0
        ) {

            return
        }

        // نتجنب أرقام صغيرة غالباً ليست مقاسات
        if (
            length < 5 ||
            width < 5
        ) {

            return
        }

        val operationNumber =
            if (
                measurements.isEmpty()
            ) {

                1

            } else {

                measurements
                    .maxOf {
                        it.operationNumber
                    } + 1
            }

        measurements.add(
            MeasurementItem(
                length =
                    length,

                width =
                    width,

                quantity =
                    quantity,

                unit =
                    "سم",

                adjustedLength =
                    length,

                adjustedWidth =
                    width,

                operationNumber =
                    operationNumber
            )
        )
    }

    // =====================================================
    // تحويل الأرقام العربية والفارسية
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

            .replace(
                '٫',
                '.'
            )
    }

    // =====================================================
    // تطبيق التعديل على جميع العمليات
    // =====================================================

    private fun applyAdjustmentToAll() {

        if (
            measurements.isEmpty()
        ) {

            Toast.makeText(
                this,
                "لا توجد مقاسات لتعديلها",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val lengthAdjustment =
            normalizeNumberInput(
                b.lengthAdjustmentInput
                    .text
                    .toString()
            )
                .toDoubleOrNull()
                ?: 0.0

        val widthAdjustment =
            normalizeNumberInput(
                b.widthAdjustmentInput
                    .text
                    .toString()
            )
                .toDoubleOrNull()
                ?: 0.0

        for (
            item in
            measurements
        ) {

            if (
                b.subtractRadio.isChecked
            ) {

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
            "تم تطبيق التعديل على جميع المقاسات",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // إعادة المقاسات للأصل
    // =====================================================

    private fun resetAdjustments() {

        for (
            item in
            measurements
        ) {

            item.adjustedLength =
                item.length

            item.adjustedWidth =
                item.width
        }

        b.lengthAdjustmentInput
            .text
            ?.clear()

        b.widthAdjustmentInput
            .text
            ?.clear()

        b.subtractRadio.isChecked =
            true

        renderTables()
    }

    // =====================================================
    // حفظ المشروع
    // =====================================================

    private fun saveProject() {

        val projectName =
            b.projectNameInput
                .text
                .toString()
                .trim()

        val customerName =
            b.customerNameInput
                .text
                .toString()
                .trim()

        val notes =
            b.notesInput
                .text
                .toString()
                .trim()

        if (
            projectName.isBlank()
        ) {

            Toast.makeText(
                this,
                "اكتب اسم المشروع أولًا",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            measurements.isEmpty()
        ) {

            Toast.makeText(
                this,
                "لا توجد مقاسات لحفظها",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val lengthAdjustment =
            normalizeNumberInput(
                b.lengthAdjustmentInput
                    .text
                    .toString()
            )
                .toDoubleOrNull()
                ?: 0.0

        val widthAdjustment =
            normalizeNumberInput(
                b.widthAdjustmentInput
                    .text
                    .toString()
            )
                .toDoubleOrNull()
                ?: 0.0

        val adjustmentType =
            if (
                b.subtractRadio.isChecked
            ) {

                "subtract"

            } else {

                "add"
            }

        val copiedMeasurements =
            measurements.map { item ->

                MeasurementItem(
                    length =
                        item.length,

                    width =
                        item.width,

                    quantity =
                        item.quantity,

                    unit =
                        item.unit,

                    adjustedLength =
                        item.adjustedLength,

                    adjustedWidth =
                        item.adjustedWidth,

                    operationNumber =
                        item.operationNumber
                )
            }

        val project =
            SavedProject(
                id =
                    System.currentTimeMillis(),

                projectName =
                    projectName,

                customerName =
                    customerName,

                notes =
                    notes,

                createdAt =
                    System.currentTimeMillis(),

                adjustmentType =
                    adjustmentType,

                lengthAdjustment =
                    lengthAdjustment,

                widthAdjustment =
                    widthAdjustment,

                measurements =
                    copiedMeasurements
            )

        val success =
            ProjectStorage.saveProject(
                this,
                project
            )

        if (
            success
        ) {

            Toast.makeText(
                this,
                "تم حفظ المشروع وعدد عملياته ${
                    toArabicNumber(
                        measurements.size
                    )
                }",
                Toast.LENGTH_LONG
            ).show()

        } else {

            Toast.makeText(
                this,
                "حدث خطأ أثناء حفظ المشروع",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // عرض المقاسات في الجدولين
    // =====================================================

    private fun renderTables() {

        while (
            b.originalTable.childCount > 1
        ) {

            b.originalTable.removeViewAt(
                1
            )
        }

        while (
            b.adjustedTable.childCount > 1
        ) {

            b.adjustedTable.removeViewAt(
                1
            )
        }

        var originalTotal =
            0.0

        var adjustedTotal =
            0.0

        val sortedMeasurements =
            measurements.sortedBy {
                it.operationNumber
            }

        for (
            item in
            sortedMeasurements
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
                    number =
                        item.operationNumber,

                    length =
                        item.length,

                    width =
                        item.width,

                    quantity =
                        item.quantity,

                    area =
                        originalArea
                )
            )

            b.adjustedTable.addView(
                createRow(
                    number =
                        item.operationNumber,

                    length =
                        item.adjustedLength,

                    width =
                        item.adjustedWidth,

                    quantity =
                        item.quantity,

                    area =
                        adjustedArea
                )
            )
        }

        b.originalTotalText.text =
            "إجمالي الأصلي: ${
                formatter.format(
                    originalTotal
                )
            } م²"

        b.adjustedTotalText.text =
            "إجمالي بعد التعديل: ${
                formatter.format(
                    adjustedTotal
                )
            } م²"
    }

    // =====================================================
    // حساب المساحة
    // الوحدة الأساسية الحالية = سم
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
    // إنشاء صف في الجدول
    // =====================================================

    private fun createRow(
        number: Int,
        length: Double,
        width: Double,
        quantity: Int,
        area: Double
    ): TableRow {

        val row =
            TableRow(
                this
            )

        row.addView(
            createCell(
                toArabicNumber(
                    number
                )
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
                toArabicNumber(
                    quantity
                )
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
    // خلية جدول
    // =====================================================

    private fun createCell(
        value: String
    ): TextView {

        return TextView(
            this
        ).apply {

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
    // تطبيع الرقم المدخل يدويًا
    // =====================================================

    private fun normalizeNumberInput(
        value: String
    ): String {

        return normalizeNumbers(
            value
        )
            .trim()
    }

    // =====================================================
    // تحويل 1 إلى ١
    // =====================================================

    private fun toArabicNumber(
        value: Int
    ): String {

        return value
            .toString()

            .replace(
                '0',
                '٠'
            )

            .replace(
                '1',
                '١'
            )

            .replace(
                '2',
                '٢'
            )

            .replace(
                '3',
                '٣'
            )

            .replace(
                '4',
                '٤'
            )

            .replace(
                '5',
                '٥'
            )

            .replace(
                '6',
                '٦'
            )

            .replace(
                '7',
                '٧'
            )

            .replace(
                '8',
                '٨'
            )

            .replace(
                '9',
                '٩'
            )
    }

    // =====================================================
    // إغلاق ML Kit
    // =====================================================

    override fun onDestroy() {

        recognizer.close()

        super.onDestroy()
    }
}
