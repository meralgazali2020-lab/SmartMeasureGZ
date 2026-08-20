package com.smartmeasure.gz

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
    // اختيار صورة
    // =====================================================

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                b.previewImage.setImageURI(uri)

                readAndProcessImage(uri)
            }
        }

    // =====================================================
    // تصوير عالي الدقة
    // =====================================================

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                cameraImageUri?.let { uri ->

                    b.previewImage.setImageURI(uri)

                    readAndProcessImage(uri)
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

        setContentView(b.root)

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
    // اختيار مصدر الصورة
    // =====================================================

    private fun showImageSourceDialog() {

        val options =
            arrayOf(
                "📷 تصوير ورقة بالكاميرا",
                "🖼 اختيار صورة من المعرض"
            )

        AlertDialog.Builder(this)
            .setTitle("قراءة ورقة المقاسات")
            .setItems(options) { _, which ->

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
    // ملف صورة الكاميرا
    // =====================================================

    private fun launchFullResolutionCamera() {

        try {

            val cameraDirectory =
                File(
                    cacheDir,
                    "camera"
                )

            if (!cameraDirectory.exists()) {
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
    // تحميل الصورة وبدء القراءة المتعددة
    // =====================================================

    private fun readAndProcessImage(
        uri: Uri
    ) {

        b.statusText.text =
            "جاري تحسين وقراءة ورقة المقاسات..."

        measurements.clear()
        renderAll()

        try {

            val originalImage =
                InputImage.fromFilePath(
                    this,
                    uri
                )

            recognizer
                .process(originalImage)
                .addOnSuccessListener { firstResult ->

                    val firstText =
                        firstResult.text

                    val bitmap =
                        loadBitmap(uri)

                    if (bitmap == null) {

                        finishOcrAnalysis(
                            listOf(firstText)
                        )

                        return@addOnSuccessListener
                    }

                    val scaled =
                        resizeForRecognition(
                            bitmap
                        )

                    val enhanced =
                        enhanceBitmap(
                            scaled,
                            1.65f
                        )

                    val secondImage =
                        InputImage.fromBitmap(
                            enhanced,
                            0
                        )

                    recognizer
                        .process(secondImage)
                        .addOnSuccessListener { secondResult ->

                            val stronger =
                                enhanceBitmap(
                                    scaled,
                                    2.15f
                                )

                            val thirdImage =
                                InputImage.fromBitmap(
                                    stronger,
                                    0
                                )

                            recognizer
                                .process(thirdImage)
                                .addOnSuccessListener { thirdResult ->

                                    finishOcrAnalysis(
                                        listOf(
                                            firstText,
                                            secondResult.text,
                                            thirdResult.text
                                        )
                                    )
                                }
                                .addOnFailureListener {

                                    finishOcrAnalysis(
                                        listOf(
                                            firstText,
                                            secondResult.text
                                        )
                                    )
                                }
                        }
                        .addOnFailureListener {

                            finishOcrAnalysis(
                                listOf(firstText)
                            )
                        }
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

        } catch (_: Exception) {

            b.statusText.text =
                "حدث خطأ أثناء فتح الصورة"
        }
    }

    // =====================================================
    // تحميل Bitmap
    // =====================================================

    private fun loadBitmap(
        uri: Uri
    ): Bitmap? {

        return try {

            val stream =
                contentResolver.openInputStream(
                    uri
                )

            val bitmap =
                android.graphics.BitmapFactory.decodeStream(
                    stream
                )

            stream?.close()

            bitmap

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================
    // تصغير الصورة إذا كانت ضخمة
    // مع الحفاظ على التفاصيل
    // =====================================================

    private fun resizeForRecognition(
        bitmap: Bitmap
    ): Bitmap {

        val maxDimension =
            2200

        val width =
            bitmap.width

        val height =
            bitmap.height

        val largest =
            maxOf(
                width,
                height
            )

        if (largest <= maxDimension) {

            return bitmap
        }

        val ratio =
            maxDimension.toFloat() /
                largest.toFloat()

        val newWidth =
            (width * ratio)
                .toInt()
                .coerceAtLeast(1)

        val newHeight =
            (height * ratio)
                .toInt()
                .coerceAtLeast(1)

        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        )
    }

    // =====================================================
    // تحسين الصورة
    // رمادي + زيادة التباين
    // =====================================================

    private fun enhanceBitmap(
        source: Bitmap,
        contrast: Float
    ): Bitmap {

        val result =
            Bitmap.createBitmap(
                source.width,
                source.height,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(result)

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        val saturationMatrix =
            ColorMatrix()

        saturationMatrix.setSaturation(
            0f
        )

        val translate =
            (1f - contrast) *
                128f

        val contrastMatrix =
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )

        saturationMatrix.postConcat(
            contrastMatrix
        )

        paint.colorFilter =
            ColorMatrixColorFilter(
                saturationMatrix
            )

        canvas.drawBitmap(
            source,
            0f,
            0f,
            paint
        )

        return result
    }

    // =====================================================
    // تحليل نتائج المحاولات الثلاث
    // =====================================================

    private fun finishOcrAnalysis(
        texts: List<String>
    ) {

        var bestText =
            ""

        var bestMeasurements =
            mutableListOf<MeasurementItem>()

        var bestScore =
            -1

        for (text in texts) {

            val parsed =
                extractMeasurements(
                    text
                )

            val score =
                parsed.size

            if (score > bestScore) {

                bestScore =
                    score

                bestMeasurements =
                    parsed

                bestText =
                    text
            }
        }

        measurements.clear()

        if (bestMeasurements.isNotEmpty()) {

            measurements.addAll(
                bestMeasurements
            )

            measurements.forEachIndexed {
                    index,
                    item ->

                item.operationNumber =
                    index + 1
            }

            b.recognizedText.text =
                bestText.ifBlank {
                    "تم تحليل المقاسات"
                }

            b.statusText.text =
                "تم العثور على ${
                    toArabicNumber(
                        measurements.size
                    )
                } عملية - راجعها قبل الحفظ"

            renderAll()

            return
        }

        // =================================================
        // إذا لم يقرأ الأرقام لكن شاهد علامات X
        // ننشئ عمليات فارغة حسب عدد الأسطر
        // =================================================

        val allText =
            texts.joinToString(
                "\n"
            )

        val xCount =
            countMeasurementLines(
                allText
            )

        b.recognizedText.text =
            chooseBestVisibleText(
                texts
            )

        if (xCount > 0) {

            for (
                number in
                1..xCount
            ) {

                measurements.add(
                    MeasurementItem(
                        length =
                            0.0,

                        width =
                            0.0,

                        quantity =
                            1,

                        unit =
                            "سم",

                        adjustedLength =
                            0.0,

                        adjustedWidth =
                            0.0,

                        operationNumber =
                            number
                    )
                )
            }

            b.statusText.text =
                "تم العثور على ${
                    toArabicNumber(
                        xCount
                    )
                } أسطر مقاسات، لكن الأرقام تحتاج تصحيحًا يدويًا"

            renderAll()

        } else {

            b.statusText.text =
                "لم يتم العثور على مقاسات واضحة"

            renderAll()
        }
    }

    // =====================================================
    // اختيار أفضل نص للعرض
    // =====================================================

    private fun chooseBestVisibleText(
        texts: List<String>
    ): String {

        return texts
            .maxByOrNull {
                it.length
            }
            ?.ifBlank {
                "لم تتم قراءة النصوص"
            }
            ?: "لم تتم قراءة النصوص"
    }

    // =====================================================
    // عد أسطر X
    // =====================================================

    private fun countMeasurementLines(
        rawText: String
    ): Int {

        val normalized =
            rawText
                .replace(
                    "×",
                    "x"
                )
                .replace(
                    "X",
                    "x"
                )
                .replace(
                    "х",
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

        val lineCount =
            normalized
                .lines()
                .count {
                    it.contains(
                        "x",
                        ignoreCase = true
                    )
                }

        if (lineCount > 0) {

            return lineCount
        }

        return Regex(
            """\bx\b""",
            RegexOption.IGNORE_CASE
        )
            .findAll(normalized)
            .count()
    }

    // =====================================================
    // استخراج العمليات من نص واحد
    // =====================================================

    private fun extractMeasurements(
        rawText: String
    ): MutableList<MeasurementItem> {

        val result =
            mutableListOf<MeasurementItem>()

        val normalized =
            normalizeNumbers(
                rawText
            )

        val lines =
            normalized
                .lines()
                .map {
                    cleanLine(it)
                }
                .filter {
                    it.isNotBlank()
                }

        for (line in lines) {

            val item =
                parseLineToMeasurement(
                    line,
                    result.size + 1
                )

            if (item != null) {

                result.add(
                    item
                )
            }
        }

        if (result.isEmpty()) {

            val text =
                cleanLine(
                    normalized
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
                    match.groupValues[1]
                        .toDoubleOrNull()
                        ?: continue

                val width =
                    match.groupValues[2]
                        .toDoubleOrNull()
                        ?: continue

                if (
                    isReasonableMeasurement(
                        length,
                        width
                    )
                ) {

                    result.add(
                        MeasurementItem(
                            length =
                                length,

                            width =
                                width,

                            quantity =
                                1,

                            unit =
                                "سم",

                            adjustedLength =
                                length,

                            adjustedWidth =
                                width,

                            operationNumber =
                                result.size + 1
                        )
                    )
                }
            }
        }

        return result
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
    // تحويل سطر إلى عملية
    // =====================================================

    private fun parseLineToMeasurement(
        originalLine: String,
        operationNumber: Int
    ): MeasurementItem? {

        val line =
            cleanLine(
                originalLine
            )

        // =================================================
        // 120 x 80
        // =================================================

        val explicitPattern =
            Regex(
                """(\d+(?:\.\d+)?)\s*x\s*(\d+(?:\.\d+)?)(?:.*?(?:عدد|qty|quantity|pcs|piece)\s*[:=]?\s*(\d+))?"""
            )

        val explicitMatch =
            explicitPattern.find(
                line
            )

        if (explicitMatch != null) {

            val length =
                explicitMatch
                    .groupValues[1]
                    .toDoubleOrNull()
                    ?: return null

            val width =
                explicitMatch
                    .groupValues[2]
                    .toDoubleOrNull()
                    ?: return null

            val quantity =
                explicitMatch
                    .groupValues
                    .getOrNull(3)
                    .orEmpty()
                    .toIntOrNull()
                    ?: 1

            if (
                !isReasonableMeasurement(
                    length,
                    width
                )
            ) {

                return null
            }

            return MeasurementItem(
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

        if (separatedMatch != null) {

            val length =
                separatedMatch
                    .groupValues[1]
                    .toDoubleOrNull()
                    ?: return null

            val width =
                separatedMatch
                    .groupValues[2]
                    .toDoubleOrNull()
                    ?: return null

            val quantity =
                separatedMatch
                    .groupValues
                    .getOrNull(3)
                    .orEmpty()
                    .toIntOrNull()
                    ?: 1

            if (
                !isReasonableMeasurement(
                    length,
                    width
                )
            ) {

                return null
            }

            return MeasurementItem(
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
        }

        // =================================================
        // 120 80
        // =================================================

        val numbers =
            Regex(
                """\d+(?:\.\d+)?"""
            )
                .findAll(line)
                .mapNotNull {
                    it.value
                        .toDoubleOrNull()
                }
                .toList()

        if (
            numbers.size == 2
        ) {

            val length =
                numbers[0]

            val width =
                numbers[1]

            if (
                !isReasonableMeasurement(
                    length,
                    width
                )
            ) {

                return null
            }

            return MeasurementItem(
                length =
                    length,

                width =
                    width,

                quantity =
                    1,

                unit =
                    "سم",

                adjustedLength =
                    length,

                adjustedWidth =
                    width,

                operationNumber =
                    operationNumber
            )
        }

        if (
            numbers.size == 3
        ) {

            val length =
                numbers[0]

            val width =
                numbers[1]

            val quantity =
                numbers[2]
                    .toInt()

            if (
                numbers[2] ==
                quantity.toDouble() &&
                quantity in 1..999 &&
                isReasonableMeasurement(
                    length,
                    width
                )
            ) {

                return MeasurementItem(
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
            }
        }

        return null
    }

    // =====================================================
    // فلترة الأرقام الغريبة
    // =====================================================

    private fun isReasonableMeasurement(
        length: Double,
        width: Double
    ): Boolean {

        if (
            length <= 0 ||
            width <= 0
        ) {

            return false
        }

        if (
            length < 5 ||
            width < 5
        ) {

            return false
        }

        if (
            length > 10000 ||
            width > 10000
        ) {

            return false
        }

        return true
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

            .replace('٫', '.')
            .replace(',', '.')
    }

    // =====================================================
    // عرض كل شيء
    // =====================================================

    private fun renderAll() {

        renderOperationsEditor()
        renderTables()
    }

    // =====================================================
    // بطاقات مراجعة العمليات
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
                .addView(empty)

            return
        }

        measurements
            .sortedBy {
                it.operationNumber
            }
            .forEach { item ->

                addEditorCard(item)
            }
    }

    // =====================================================
    // بطاقة كل عملية
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

        container.addView(title)

        val measurement =
            TextView(this).apply {

                text =
                    if (
                        item.length <= 0 ||
                        item.width <= 0
                    ) {

                        "الأرقام لم تُقرأ - اضغط تعديل"

                    } else {

                        "الطول: ${
                            formatter.format(
                                item.length
                            )
                        } سم | العرض: ${
                            formatter.format(
                                item.width
                            )
                        } سم | العدد: ${
                            toArabicNumber(
                                item.quantity
                            )
                        }"
                    }

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
                if (
                    item.length > 0
                ) {
                    formatter.format(
                        item.length
                    )
                } else {
                    ""
                }
            )

        val widthInput =
            createNumberInput(
                "العرض بالسنتيمتر",
                if (
                    item.width > 0
                ) {
                    formatter.format(
                        item.width
                    )
                } else {
                    ""
                }
            )

        val quantityInput =
            EditText(this).apply {

                hint =
                    "العدد"

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    item.quantity
                        .coerceAtLeast(1)
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
                        "أدخل الطول والعرض والعدد بشكل صحيح",
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
                }؟"
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
    // إضافة عملية يدوية
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

        val nextNumber =
            if (
                measurements.isEmpty()
            ) {
                1
            } else {
                measurements.maxOf {
                    it.operationNumber
                } + 1
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "إضافة العملية ${
                        toArabicNumber(
                            nextNumber
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
                            nextNumber
                    )
                )

                renderAll()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // =====================================================
    // إنشاء حقل رقمي
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

            setText(value)
        }
    }

    // =====================================================
    // تطبيق التنقيص أو الزيادة
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

        val invalid =
            measurements.any {
                it.length <= 0 ||
                    it.width <= 0
            }

        if (invalid) {

            Toast.makeText(
                this,
                "صحح العمليات غير المقروءة أولًا",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val lengthAdjustment =
            getLengthAdjustment()

        val widthAdjustment =
            getWidthAdjustment()

        for (item in measurements) {

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

        for (item in measurements) {

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
    // إلغاء التعديل
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

        for (item in measurements) {

            item.adjustedLength =
                item.length

            item.adjustedWidth =
                item.width
        }

        renderAll()
    }

    private fun calculateAdjustedLength(
        original: Double
    ): Double {

        if (original <= 0) {
            return 0.0
        }

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

        if (original <= 0) {
            return 0.0
        }

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

        val invalid =
            measurements.any {
                it.length <= 0 ||
                    it.width <= 0 ||
                    it.quantity <= 0
            }

        if (invalid) {

            Toast.makeText(
                this,
                "يوجد مقاس لم تتم قراءته، صححه قبل الحفظ",
                Toast.LENGTH_LONG
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

        if (success) {

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
                if (
                    item.length > 0 &&
                    item.width > 0
                ) {

                    calculateArea(
                        item.length,
                        item.width,
                        item.quantity
                    )

                } else {

                    0.0
                }

            val adjustedArea =
                if (
                    item.adjustedLength > 0 &&
                    item.adjustedWidth > 0
                ) {

                    calculateArea(
                        item.adjustedLength,
                        item.adjustedWidth,
                        item.quantity
                    )

                } else {

                    0.0
                }

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
                if (length > 0) {
                    formatter.format(
                        length
                    )
                } else {
                    "-"
                }
            )
        )

        row.addView(
            createCell(
                if (width > 0) {
                    formatter.format(
                        width
                    )
                } else {
                    "-"
                }
            )
        )

        row.addView(
            createCell(
                toArabicNumber(
                    quantity
                        .coerceAtLeast(1)
                )
            )
        )

        row.addView(
            createCell(
                if (
                    area > 0
                ) {
                    formatter.format(
                        area
                    )
                } else {
                    "-"
                }
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
