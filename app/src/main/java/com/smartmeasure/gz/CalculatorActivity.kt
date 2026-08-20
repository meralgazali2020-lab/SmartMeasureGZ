package com.smartmeasure.gz

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityCalculatorBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculatorActivity : AppCompatActivity() {

    private lateinit var b: ActivityCalculatorBinding

    private val formatter =
        DecimalFormat("#.###")

    // جميع العمليات الموجودة حاليًا داخل المشروع
    private val operations =
        mutableListOf<MeasurementItem>()

    // رقم العملية التالية
    private var nextOperationNumber =
        1

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivityCalculatorBinding.inflate(
                layoutInflater
            )

        setContentView(
            b.root
        )

        setupUnitSpinner()
        setupButtons()
        updateOperationNumber()
        refreshOperationsList()
    }

    // =====================================================
    // الوحدة
    // =====================================================

    private fun setupUnitSpinner() {

        val units =
            listOf(
                "سم",
                "مم",
                "متر"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                units
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        b.unitSpinner.adapter =
            adapter
    }

    // =====================================================
    // الأزرار
    // =====================================================

    private fun setupButtons() {

        b.calculateBtn.setOnClickListener {

            calculateMeasurement()
        }

        b.applyAdjustmentBtn.setOnClickListener {

            applyAdjustment()
        }

        b.addOperationBtn.setOnClickListener {

            addCurrentOperation()
        }

        b.saveProjectBtn.setOnClickListener {

            saveCurrentProject()
        }

        b.savedProjectsBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SavedProjectsActivity::class.java
                )
            )
        }

        b.newProjectBtn.setOnClickListener {

            requestNewProject()
        }

        b.clearBtn.setOnClickListener {

            clearCurrentOperationFields()
        }
    }

    // =====================================================
    // حساب المقاس
    // =====================================================

    private fun calculateMeasurement(): Boolean {

        val length =
            b.lengthInput.text
                .toString()
                .trim()
                .toDoubleOrNull()

        val width =
            b.widthInput.text
                .toString()
                .trim()
                .toDoubleOrNull()

        val quantity =
            b.quantityInput.text
                .toString()
                .trim()
                .toIntOrNull()

        if (
            length == null ||
            width == null ||
            quantity == null
        ) {

            Toast.makeText(
                this,
                "أدخل الطول والعرض والعدد بشكل صحيح",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        if (
            length <= 0 ||
            width <= 0 ||
            quantity <= 0
        ) {

            Toast.makeText(
                this,
                "يجب أن تكون القيم أكبر من صفر",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        val unit =
            b.unitSpinner.selectedItem
                .toString()

        val lengthMeters =
            convertToMeters(
                length,
                unit
            )

        val widthMeters =
            convertToMeters(
                width,
                unit
            )

        val areaPerPiece =
            lengthMeters *
                widthMeters

        val totalArea =
            areaPerPiece *
                quantity

        b.originalMeasurementText.text =
            "${formatter.format(length)} × " +
                "${formatter.format(width)} $unit"

        b.originalQuantityText.text =
            quantity.toString()

        b.originalAreaText.text =
            "${formatter.format(totalArea)} م²"

        return applyAdjustment()
    }

    // =====================================================
    // تطبيق التعديل
    // =====================================================

    private fun applyAdjustment(): Boolean {

        val originalLength =
            b.lengthInput.text
                .toString()
                .trim()
                .toDoubleOrNull()

        val originalWidth =
            b.widthInput.text
                .toString()
                .trim()
                .toDoubleOrNull()

        val quantity =
            b.quantityInput.text
                .toString()
                .trim()
                .toIntOrNull()

        if (
            originalLength == null ||
            originalWidth == null ||
            quantity == null
        ) {

            return false
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

        val adjustedLength =
            if (
                b.subtractRadio.isChecked
            ) {

                originalLength -
                    lengthAdjustment

            } else {

                originalLength +
                    lengthAdjustment
            }

        val adjustedWidth =
            if (
                b.subtractRadio.isChecked
            ) {

                originalWidth -
                    widthAdjustment

            } else {

                originalWidth +
                    widthAdjustment
            }

        if (
            adjustedLength <= 0 ||
            adjustedWidth <= 0
        ) {

            Toast.makeText(
                this,
                "التعديل جعل أحد المقاسات صفرًا أو أقل",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        val unit =
            b.unitSpinner.selectedItem
                .toString()

        val lengthMeters =
            convertToMeters(
                adjustedLength,
                unit
            )

        val widthMeters =
            convertToMeters(
                adjustedWidth,
                unit
            )

        val totalArea =
            lengthMeters *
                widthMeters *
                quantity

        b.adjustedMeasurementText.text =
            "${formatter.format(adjustedLength)} × " +
                "${formatter.format(adjustedWidth)} $unit"

        b.adjustedQuantityText.text =
            quantity.toString()

        b.adjustedAreaText.text =
            "${formatter.format(totalArea)} م²"

        return true
    }

    // =====================================================
    // إضافة العملية الحالية
    // =====================================================

    private fun addCurrentOperation() {

        if (
            !calculateMeasurement()
        ) {

            return
        }

        val length =
            b.lengthInput.text
                .toString()
                .trim()
                .toDoubleOrNull()
                ?: return

        val width =
            b.widthInput.text
                .toString()
                .trim()
                .toDoubleOrNull()
                ?: return

        val quantity =
            b.quantityInput.text
                .toString()
                .trim()
                .toIntOrNull()
                ?: return

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

        val adjustedLength =
            if (
                b.subtractRadio.isChecked
            ) {

                length -
                    lengthAdjustment

            } else {

                length +
                    lengthAdjustment
            }

        val adjustedWidth =
            if (
                b.subtractRadio.isChecked
            ) {

                width -
                    widthAdjustment

            } else {

                width +
                    widthAdjustment
            }

        if (
            adjustedLength <= 0 ||
            adjustedWidth <= 0
        ) {

            Toast.makeText(
                this,
                "المقاس المعدل غير صحيح",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val item =
            MeasurementItem(
                length =
                    length,

                width =
                    width,

                quantity =
                    quantity,

                unit =
                    b.unitSpinner.selectedItem
                        .toString(),

                adjustedLength =
                    adjustedLength,

                adjustedWidth =
                    adjustedWidth,

                operationNumber =
                    nextOperationNumber
            )

        operations.add(
            item
        )

        Toast.makeText(
            this,
            "تمت إضافة العملية ${
                toArabicNumber(
                    nextOperationNumber
                )
            }",
            Toast.LENGTH_SHORT
        ).show()

        nextOperationNumber++

        updateOperationNumber()

        refreshOperationsList()

        clearCurrentOperationFields(
            keepAdjustments = true
        )
    }

    // =====================================================
    // عرض رقم العملية التالية
    // =====================================================

    private fun updateOperationNumber() {

        b.currentOperationText.text =
            "العملية ${
                toArabicNumber(
                    nextOperationNumber
                )
            }"
    }

    // =====================================================
    // عرض العمليات المضافة
    // =====================================================

    private fun refreshOperationsList() {

        b.operationsContainer
            .removeAllViews()

        b.operationsCountText.text =
            "عدد العمليات: ${
                toArabicNumber(
                    operations.size
                )
            }"

        if (
            operations.isEmpty()
        ) {

            val emptyText =
                TextView(this).apply {

                    text =
                        "لم تتم إضافة عمليات بعد"

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

            b.operationsContainer.addView(
                emptyText
            )

            return
        }

        operations
            .sortedBy {
                it.operationNumber
            }
            .forEach { item ->

                addOperationCard(
                    item
                )
            }
    }

    // =====================================================
    // بطاقة العملية
    // =====================================================

    private fun addOperationCard(
        item: MeasurementItem
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    22,
                    18,
                    22,
                    18
                )

                setBackgroundColor(
                    Color.WHITE
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
                    15
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

        val original =
            TextView(this).apply {

                text =
                    "الأصلي: ${
                        formatter.format(
                            item.length
                        )
                    } × ${
                        formatter.format(
                            item.width
                        )
                    } ${item.unit}   |   العدد: ${
                        toArabicNumber(
                            item.quantity
                        )
                    }"

                textSize =
                    15f

                setPadding(
                    0,
                    10,
                    0,
                    5
                )
            }

        container.addView(
            original
        )

        val adjusted =
            TextView(this).apply {

                text =
                    "بعد التعديل: ${
                        formatter.format(
                            item.adjustedLength
                        )
                    } × ${
                        formatter.format(
                            item.adjustedWidth
                        )
                    } ${item.unit}"

                textSize =
                    15f

                setTextColor(
                    Color.parseColor(
                        "#B8860B"
                    )
                )
            }

        container.addView(
            adjusted
        )

        val originalArea =
            calculateArea(
                item.length,
                item.width,
                item.quantity,
                item.unit
            )

        val adjustedArea =
            calculateArea(
                item.adjustedLength,
                item.adjustedWidth,
                item.quantity,
                item.unit
            )

        val area =
            TextView(this).apply {

                text =
                    "المساحة الأصلية: ${
                        formatter.format(
                            originalArea
                        )
                    } م²   |   المعدلة: ${
                        formatter.format(
                            adjustedArea
                        )
                    } م²"

                textSize =
                    14f

                setPadding(
                    0,
                    8,
                    0,
                    8
                )
            }

        container.addView(
            area
        )

        val deleteButton =
            Button(this).apply {

                text =
                    "حذف العملية ${
                        toArabicNumber(
                            item.operationNumber
                        )
                    }"

                isAllCaps =
                    false

                setOnClickListener {

                    confirmDeleteOperation(
                        item
                    )
                }
            }

        container.addView(
            deleteButton
        )

        b.operationsContainer.addView(
            container
        )
    }

    // =====================================================
    // حذف عملية
    // =====================================================

    private fun confirmDeleteOperation(
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
                }؟\n\nلن تتغير أرقام العمليات الأخرى."
            )

            .setNegativeButton(
                "إلغاء",
                null
            )

            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                operations.remove(
                    item
                )

                refreshOperationsList()

                Toast.makeText(
                    this,
                    "تم حذف العملية ${
                        toArabicNumber(
                            item.operationNumber
                        )
                    }",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .show()
    }

    // =====================================================
    // حفظ المشروع
    // =====================================================

    private fun saveCurrentProject() {

        if (
            operations.isEmpty()
        ) {

            Toast.makeText(
                this,
                "أضف عملية واحدة على الأقل قبل حفظ المشروع",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        var projectName =
            b.projectNameInput.text
                .toString()
                .trim()

        if (
            projectName.isBlank()
        ) {

            projectName =
                createAutomaticProjectName()
        }

        val customerName =
            b.customerNameInput.text
                .toString()
                .trim()

        val notes =
            b.projectNotesInput.text
                .toString()
                .trim()

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

        val adjustmentType =
            if (
                b.subtractRadio.isChecked
            ) {

                "subtract"

            } else {

                "add"
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
                    operations.map {
                        it.copy()
                    }
            )

        val saved =
            ProjectStorage.saveProject(
                this,
                project
            )

        if (saved) {

            b.projectNameInput.setText(
                projectName
            )

            Toast.makeText(
                this,
                "تم حفظ المشروع بنجاح وعدد عملياته ${
                    toArabicNumber(
                        operations.size
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
    // مشروع جديد
    // =====================================================

    private fun requestNewProject() {

        if (
            operations.isEmpty() &&
            b.projectNameInput.text
                .toString()
                .isBlank() &&
            b.customerNameInput.text
                .toString()
                .isBlank()
        ) {

            resetEntireProject()
            return
        }

        AlertDialog.Builder(this)

            .setTitle(
                "مشروع جديد"
            )

            .setMessage(
                "هل تريد بدء مشروع جديد؟\n\nتأكد من حفظ المشروع الحالي أولًا إذا كنت تريد الاحتفاظ به."
            )

            .setNegativeButton(
                "إلغاء",
                null
            )

            .setPositiveButton(
                "مشروع جديد"
            ) { _, _ ->

                resetEntireProject()
            }

            .show()
    }

    // =====================================================
    // إعادة المشروع من البداية
    // =====================================================

    private fun resetEntireProject() {

        operations.clear()

        nextOperationNumber =
            1

        b.projectNameInput.text?.clear()
        b.customerNameInput.text?.clear()
        b.projectNotesInput.text?.clear()

        b.lengthAdjustmentInput.text?.clear()
        b.widthAdjustmentInput.text?.clear()

        b.subtractRadio.isChecked =
            true

        clearCurrentOperationFields(
            keepAdjustments = false
        )

        updateOperationNumber()
        refreshOperationsList()

        Toast.makeText(
            this,
            "تم فتح مشروع جديد",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // مسح حقول العملية الحالية
    // =====================================================

    private fun clearCurrentOperationFields(
        keepAdjustments: Boolean = false
    ) {

        b.lengthInput.text?.clear()
        b.widthInput.text?.clear()
        b.quantityInput.text?.clear()

        if (
            !keepAdjustments
        ) {

            b.lengthAdjustmentInput
                .text
                ?.clear()

            b.widthAdjustmentInput
                .text
                ?.clear()

            b.subtractRadio.isChecked =
                true
        }

        b.originalMeasurementText.text =
            "-"

        b.originalQuantityText.text =
            "-"

        b.originalAreaText.text =
            "-"

        b.adjustedMeasurementText.text =
            "-"

        b.adjustedQuantityText.text =
            "-"

        b.adjustedAreaText.text =
            "-"
    }

    // =====================================================
    // حساب المساحة
    // =====================================================

    private fun calculateArea(
        length: Double,
        width: Double,
        quantity: Int,
        unit: String
    ): Double {

        return convertToMeters(
            length,
            unit
        ) *
            convertToMeters(
                width,
                unit
            ) *
            quantity
    }

    // =====================================================
    // التحويل للمتر
    // =====================================================

    private fun convertToMeters(
        value: Double,
        unit: String
    ): Double {

        return when (
            unit
        ) {

            "مم" ->
                value / 1000.0

            "سم" ->
                value / 100.0

            else ->
                value
        }
    }

    // =====================================================
    // اسم تلقائي للمشروع
    // =====================================================

    private fun createAutomaticProjectName(): String {

        val date =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(
                Date()
            )

        return "مشروع $date"
    }

    // =====================================================
    // أرقام عربية
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
}
