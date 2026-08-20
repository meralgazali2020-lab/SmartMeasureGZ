package com.smartmeasure.gz

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

                cameraImageUri?.let { uri ->

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
    // صلاحية الكاميرا
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

        renderAll()
    }

    // =====================================================
    // الأزرار
    // =====================================================

    private fun setupButtons() {

        b.selectImageBtn.setOnClickListener {

            showImageSourceDialog()
        }

        b.addManualMeasurementBtn.setOnClickListener {

            showAddManualMeasurementDialog()
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
    // مصدر الصورة
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
    // كاميرا عالية الدقة
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

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "تعذر فتح الكاميرا",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // فتح الصورة
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

        } catch (_: Exception) {

            b.statusText.text =
                "حدث خطأ أثناء فتح الصورة"
        }
    }

    // =====================================================
    // قراءة OCR
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
                    "حاول تصوير الورقة بوضوح وبشكل مستقيم",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // تحليل النص
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

        for (line in lines) {

            parseLine(
                line
            )
        }

        if (
            measurements.isEmpty()
        ) {

            parseWholeText(
                normalized
            )
        }

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

        } else {

            b.statusText.text =
                "تم العثور على ${
                    toArabicNumber(
                        measurements.size
                    )
                } عملية - راجع المقاسات قبل الحفظ"
        }

        renderAll()
    }

    // =====================================================
    // تنظيف السطر
    // =====================================================

    private fun cleanLine(
        value: String
    ): String {

        return value
            .lowercase()
            .replace("×", "x")
            .replace("х", "x")
            .replace("X", "x")
            .replace("*", "x")
            .replace("✕", "x")
            .replace("✖", "x")
            .replace("ـ", " ")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    // =====================================================
    // تحليل سطر
    // =====================================================

    private fun parseLine(
        originalLine: String
    ) {

        val line =
            cleanLine(
                originalLine
            )

        // 120 x 80
        // 120 x 80 عدد 2

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

            val quantity =
                explicitMatch
                    .groupValues
                    .getOrNull(3)
                    .orEmpty()
                    .toIntOrNull()
                    ?: 1

            addMeasurement(
                length,
                width,
                quantity
            )

            return
        }

        // 120 - 80
        // 120 / 80
        // 120 : 80

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
                length,
                width,
                quantity
            )

            return
        }

        // OCR قد يقرأ:
        // 120 80

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
                numbers[0],
                numbers[1],
                1
            )

            return
        }

        // 120 80 2

        if (
            numbers.size == 3
        ) {

            val quantity =
                numbers[2]
                    .toInt()

            if (
                numbers[2] ==
                quantity.toDouble() &&
                quantity in 1..999
            ) {

                addMeasurement(
                    numbers[0],
                    numbers[1],
                    quantity
                )
            }
        }
    }

    // =====================================================
    // تحليل النص كاملاً
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

        for (
            match in
            pattern.findAll(text)
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
                length,
                width,
                1
            )
        }
    }

    // =====================================================
    // إضافة عملية
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

        if (
            length < 5 ||
            width < 5
        ) {

            return
        }

        val operationNumber =
            getNextOperationNumber()

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
                    calculateAdjustedLength(
                        length
                    ),

                adjustedWidth =
                    calculateAdjustedWidth(
                        width
                    ),

                operationNumber =
                    operationNumber
            )
        )
    }

    // =====================================================
    // الرقم التالي
    // =====================================================

    private fun getNextOperationNumber(): Int {

        return if (
            measurements.isEmpty()
        ) {

            1

        } else {

            measurements.maxOf {
                it.operationNumber
            } + 1
        }
    }

    // =====================================================
    // عرض كل شيء
    // =====================================================

    private fun renderAll() {

        renderOperationsEditor()
        renderTables()
    }

    // =====================================================
    // قسم مراجعة وتصحيح العمليات
    // =====================================================

    private fun renderOperationsEditor() {

        b.operationsEditorContainer
            .removeAllViews()

        b.operationsCountText.text =
            "عدد العمليات: ${
                toArabicNumber(
                    measurements.size
                )
            }"

        if (
            measurements.isEmpty()
        ) {

            val empty =
                TextView(this).apply {

                    text =
                        "لا توجد عمليات حاليًا"

                    gravity =
                        Gravity.CENTER

                    textSize =
                        15f

                    setTextColor(
                        Color.GRAY
                    )

                    setPadding(
                        10,
                        20,
                        10,
                        20
                    )
                }

            b.operationsEditorContainer
                .addView(
                    empty
                )

            return
        }

        measurements
            .sortedBy {
                it.operationNumber
            }
            .forEach { item ->

                addEditorCard(
                    item
                )
            }
    }

    // =====================================================
    // بطاقة العملية
    // =====================================================

    private fun addEditorCard(
        item: MeasurementItem
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.WHITE
                )

                setPadding(
                    20,
                    18,
                    20,
                    18
                )

                val params =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                params.setMargins(
                    0,
                    0,
                    0,
                    14
                )

                layoutParams =
                    params
            }

        val title =
            TextView(this).apply {

                text =
                    "العملية ${
                        toArabicNumber(
                            item.operationNumber
                        )
                    }"

                textSize =
                    19f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.parseColor(
                        "#0B2341"
                    )
                )
            }

        container.addView(
            title
        )

        val measurement =
            TextView(this).apply {

                text =
                    "الطول: ${
                        formatter.format(
                            item.length
                        )
                    } سم   |   العرض: ${
                        formatter.format(
                            item.width
                        )
                    } سم   |   العدد: ${
                        toArabicNumber(
                            item.quantity
                        )
                    }"

                textSize =
                    16f

                setPadding(
                    0,
                    10,
                    0,
                    10
                )
            }

        container.addView(
            measurement
        )

        val buttonsRow =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL
            }

        val editButton =
            Button(this).apply {

                text =
                    "تعديل"

                isAllCaps =
                    false

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                setOnClickListener {

                    showEditMeasurementDialog(
                        item
                    )
                }
            }

        val deleteButton =
            Button(this).apply {

                text =
                    "حذف"

                isAllCaps =
                    false

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                setOnClickListener {

                    confirmDeleteMeasurement(
                        item
                    )
                }
            }

        buttonsRow.addView(
            editButton
        )

        buttonsRow.addView(
            deleteButton
        )

        container.addView(
            buttonsRow
        )

        b.operationsEditorContainer
            .addView(
                container
            )
    }

    // =====================================================
    // تعديل عملية
    // =====================================================

    private fun showEditMeasurementDialog(
        item: MeasurementItem
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    10,
                    40,
                    0
                )
            }

        val lengthInput =
            createNumberInput(
                "الطول بالسنتيمتر",
                formatter.format(
                    item.length
                )
            )

        val widthInput =
            createNumberInput(
                "العرض بالسنتيمتر",
                formatter.format(
                    item.width
                )
            )

        val quantityInput =
            EditText(this).apply {

                hint =
                    "العدد"

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    item.quantity
                        .toString()
                )
            }

        container.addView(
            lengthInput
        )

        container.addView(
            widthInput
        )

        container.addView(
            quantityInput
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "تعديل العملية ${
                        toArabicNumber(
                            item.operationNumber
                        )
                    }"
                )
                .setView(
                    container
                )
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .setPositiveButton(
                    "حفظ",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val length =
                    normalizedDouble(
                        lengthInput.text
                            .toString()
                    )

                val width =
                    normalizedDouble(
                        widthInput.text
                            .toString()
                    )

                val quantity =
                    normalizedInt(
                        quantityInput.text
                            .toString()
                    )

                if (
                    length == null ||
                    width == null ||
                    quantity == null ||
                    length <= 0 ||
                    width <= 0 ||
                    quantity <= 0
                ) {

                    Toast.makeText(
                        this,
                        "أدخل المقاسات والعدد بشكل صحيح",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                item.length =
                    length

                item.width =
                    width

                item.quantity =
                    quantity

                item.adjustedLength =
                    calculateAdjustedLength(
                        length
                    )

                item.adjustedWidth =
                    calculateAdjustedWidth(
                        width
                    )

                if (
                    item.adjustedLength <= 0 ||
                    item.adjustedWidth <= 0
                ) {

                    item.adjustedLength =
                        length

                    item.adjustedWidth =
                        width
                }

                renderAll()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // =====================================================
    // حذف عملية
    // =====================================================

    private fun confirmDeleteMeasurement(
        item: MeasurementItem
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "حذف العملية"
            )
            .setMessage(
                "هل تريد حذف العملية ${
                    toArabicNumber(
                        item.operationNumber
                    )
                }؟\nلن تتغير أرقام العمليات الأخرى."
            )
            .setNegativeButton(
                "إلغاء",
                null
            )
            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                measurements.remove(
                    item
                )

                renderAll()
            }
            .show()
    }

    // =====================================================
    // إضافة عملية يدوياً
    // =====================================================

    private fun showAddManualMeasurementDialog() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    10,
                    40,
                    0
                )
            }

        val lengthInput =
            createNumberInput(
                "الطول بالسنتيمتر",
                ""
            )

        val widthInput =
            createNumberInput(
                "العرض بالسنتيمتر",
                ""
            )

        val quantityInput =
            EditText(this).apply {

                hint =
                    "العدد"

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    "1"
                )
            }

        container.addView(
            lengthInput
        )

        container.addView(
            widthInput
        )

        container.addView(
            quantityInput
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "إضافة العملية ${
                        toArabicNumber(
                            getNextOperationNumber()
                        )
                    }"
                )
                .setView(
                    container
                )
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .setPositiveButton(
                    "إضافة",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val length =
                    normalizedDouble(
                        lengthInput.text
                            .toString()
                    )

                val width =
                    normalizedDouble(
                        widthInput.text
                            .toString()
                    )

                val quantity =
                    normalizedInt(
                        quantityInput.text
                            .toString()
                    )

                if (
                    length == null ||
                    width == null ||
                    quantity == null ||
                    length <= 0 ||
                    width <= 0 ||
                    quantity <= 0
                ) {

                    Toast.makeText(
                        this,
                        "أدخل الطول والعرض والعدد",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                addMeasurement(
                    length,
                    width,
                    quantity
                )

                renderAll()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // =====================================================
    // حقل رقمي
    // =====================================================

    private fun createNumberInput(
        hintText: String,
        value: String
    ): EditText {

        return EditText(this).apply {

            hint =
                hintText

            inputType =
                InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL

            setText(
                value
            )
        }
    }

    // =====================================================
    // تطبيق تعديل جماعي
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
            getLengthAdjustment()

        val widthAdjustment =
            getWidthAdjustment()

        for (
            item in
            measurements
        ) {

            val newLength =
                if (
                    b.subtractRadio.isChecked
                ) {

                    item.length -
                        lengthAdjustment

                } else {

                    item.length +
                        lengthAdjustment
                }

            val newWidth =
                if (
                    b.subtractRadio.isChecked
                ) {

                    item.width -
                        widthAdjustment

                } else {

                    item.width +
                        widthAdjustment
                }

            if (
                newLength <= 0 ||
                newWidth <= 0
            ) {

                Toast.makeText(
                    this,
                    "قيمة التعديل كبيرة بالنسبة لبعض المقاسات",
                    Toast.LENGTH_LONG
                ).show()

                return
            }
        }

        for (
            item in
            measurements
        ) {

            item.adjustedLength =
                calculateAdjustedLength(
                    item.length
                )

            item.adjustedWidth =
                calculateAdjustedWidth(
                    item.width
                )
        }

        renderAll()

        Toast.makeText(
            this,
            "تم تطبيق التعديل على جميع العمليات",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // إعادة الأصل
    // =====================================================

    private fun resetAdjustments() {

        b.lengthAdjustmentInput
            .text
            ?.clear()

        b.widthAdjustmentInput
            .text
            ?.clear()

        b.subtractRadio.isChecked =
            true

        for (
            item in
            measurements
        ) {

            item.adjustedLength =
                item.length

            item.adjustedWidth =
                item.width
        }

        renderAll()
    }

    // =====================================================
    // حساب التعديل
    // =====================================================

    private fun calculateAdjustedLength(
        original: Double
    ): Double {

        val adjustment =
            getLengthAdjustment()

        return if (
            b.subtractRadio.isChecked
        ) {

            original -
                adjustment

        } else {

            original +
                adjustment
        }
    }

    private fun calculateAdjustedWidth(
        original: Double
    ): Double {

        val adjustment =
            getWidthAdjustment()

        return if (
            b.subtractRadio.isChecked
        ) {

            original -
                adjustment

        } else {

            original +
                adjustment
        }
    }

    private fun getLengthAdjustment(): Double {

        return normalizedDouble(
            b.lengthAdjustmentInput
                .text
                .toString()
        ) ?: 0.0
    }

    private fun getWidthAdjustment(): Double {

        return normalizedDouble(
            b.widthAdjustmentInput
                .text
                .toString()
        ) ?: 0.0
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
                "لا توجد عمليات لحفظها",
                Toast.LENGTH_SHORT
            ).show()

            return
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
                    if (
                        b.subtractRadio.isChecked
                    ) {
                        "subtract"
                    } else {
                        "add"
                    },

                lengthAdjustment =
                    getLengthAdjustment(),

                widthAdjustment =
                    getWidthAdjustment(),

                measurements =
                    measurements.map {
                        it.copy()
                    }
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
    // الجداول
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

        for (
            item in
            measurements.sortedBy {
                it.operationNumber
            }
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
                    item.operationNumber,
                    item.length,
                    item.width,
                    item.quantity,
                    originalArea
                )
            )

            b.adjustedTable.addView(
                createRow(
                    item.operationNumber,
                    item.adjustedLength,
                    item.adjustedWidth,
                    item.quantity,
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
    // المساحة
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
    // صف جدول
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
    // الأرقام
    // =====================================================

    private fun normalizedDouble(
        value: String
    ): Double? {

        return normalizeNumbers(
            value
        )
            .trim()
            .toDoubleOrNull()
    }

    private fun normalizedInt(
        value: String
    ): Int? {

        return normalizeNumbers(
            value
        )
            .trim()
            .toIntOrNull()
    }

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
            .replace(',', '.')
    }

    private fun toArabicNumber(
        value: Int
    ): String {

        return value
            .toString()
            .replace('0', '٠')
            .replace('1', '١')
            .replace('2', '٢')
            .replace('3', '٣')
            .replace('4', '٤')
            .replace('5', '٥')
            .replace('6', '٦')
            .replace('7', '٧')
            .replace('8', '٨')
            .replace('9', '٩')
    }

    // =====================================================
    // إغلاق OCR
    // =====================================================

    override fun onDestroy() {

        recognizer.close()

        super.onDestroy()
    }
}
