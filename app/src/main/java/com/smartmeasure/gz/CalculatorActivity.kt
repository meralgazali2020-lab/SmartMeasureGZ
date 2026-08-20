package com.smartmeasure.gz

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
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

    private val operations =
        mutableListOf<MeasurementItem>()

    private var nextOperationNumber =
        1

    private var editingProjectId: Long =
        -1L

    private var originalCreatedAt: Long =
        0L

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

        editingProjectId =
            intent.getLongExtra(
                "projectId",
                -1L
            )

        if (
            editingProjectId != -1L
        ) {

            loadExistingProject(
                editingProjectId
            )

        } else {

            checkForSavedDraft()
        }
    }

    // =====================================================
    // إعداد الوحدات
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
            saveAutomaticDraft()
        }

        b.addOperationBtn.setOnClickListener {

            addCurrentOperation()
        }

        b.repeatLastOperationBtn.setOnClickListener {

            repeatLastOperation()
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

            focusLengthInput()
        }
    }

    // =====================================================
    // فحص وجود مسودة قديمة
    // =====================================================

    private fun checkForSavedDraft() {

        if (
            !ProjectDraftStorage.hasDraft(
                this
            )
        ) {

            startEmptyProject()
            return
        }

        val draft =
            ProjectDraftStorage.getDraft(
                this
            )

        if (
            draft == null ||
            draft.measurements.isEmpty()
        ) {

            ProjectDraftStorage.clearDraft(
                this
            )

            startEmptyProject()
            return
        }

        AlertDialog.Builder(this)

            .setTitle(
                "عمل سابق غير مكتمل"
            )

            .setMessage(
                "يوجد مشروع سابق يحتوي على ${
                    toArabicNumber(
                        draft.measurements.size
                    )
                } عملية.\n\nهل تريد متابعة العمل السابق؟"
            )

            .setNegativeButton(
                "مشروع جديد"
            ) { _, _ ->

                ProjectDraftStorage.clearDraft(
                    this
                )

                startEmptyProject()
            }

            .setPositiveButton(
                "متابعة العمل السابق"
            ) { _, _ ->

                loadDraft(
                    draft
                )
            }

            .setCancelable(
                false
            )

            .show()
    }

    // =====================================================
    // بدء مشروع فارغ
    // =====================================================

    private fun startEmptyProject() {

        operations.clear()

        nextOperationNumber =
            1

        editingProjectId =
            -1L

        originalCreatedAt =
            0L

        b.quantityInput.setText(
            "1"
        )

        updateOperationNumber()
        refreshOperationsList()
        focusLengthInput()
    }

    // =====================================================
    // تحميل المسودة
    // =====================================================

    private fun loadDraft(
        draft: SavedProject
    ) {

        editingProjectId =
            -1L

        originalCreatedAt =
            draft.createdAt

        b.projectNameInput.setText(
            draft.projectName
        )

        b.customerNameInput.setText(
            draft.customerName
        )

        b.projectNotesInput.setText(
            draft.notes
        )

        b.lengthAdjustmentInput.setText(
            if (
                draft.lengthAdjustment == 0.0
            ) {
                ""
            } else {
                formatter.format(
                    draft.lengthAdjustment
                )
            }
        )

        b.widthAdjustmentInput.setText(
            if (
                draft.widthAdjustment == 0.0
            ) {
                ""
            } else {
                formatter.format(
                    draft.widthAdjustment
                )
            }
        )

        if (
            draft.adjustmentType == "add"
        ) {

            b.addRadio.isChecked =
                true

        } else {

            b.subtractRadio.isChecked =
                true
        }

        operations.clear()

        draft.measurements
            .forEachIndexed {
                    index,
                    item ->

                operations.add(
                    item.copy(
                        operationNumber =
                            if (
                                item.operationNumber > 0
                            ) {
                                item.operationNumber
                            } else {
                                index + 1
                            }
                    )
                )
            }

        nextOperationNumber =
            if (
                operations.isEmpty()
            ) {

                1

            } else {

                operations.maxOf {
                    it.operationNumber
                } + 1
            }

        b.quantityInput.setText(
            "1"
        )

        updateOperationNumber()
        refreshOperationsList()
        focusLengthInput()

        Toast.makeText(
            this,
            "تم استرجاع العمل السابق",
            Toast.LENGTH_LONG
        ).show()
    }

    // =====================================================
    // تحميل مشروع محفوظ
    // =====================================================

    private fun loadExistingProject(
        projectId: Long
    ) {

        val project =
            ProjectStorage
                .getProjects(this)
                .firstOrNull {
                    it.id == projectId
                }

        if (
            project == null
        ) {

            Toast.makeText(
                this,
                "تعذر تحميل المشروع",
                Toast.LENGTH_LONG
            ).show()

            editingProjectId =
                -1L

            startEmptyProject()

            return
        }

        originalCreatedAt =
            project.createdAt

        b.projectNameInput.setText(
            project.projectName
        )

        b.customerNameInput.setText(
            project.customerName
        )

        b.projectNotesInput.setText(
            project.notes
        )

        b.lengthAdjustmentInput.setText(
            if (
                project.lengthAdjustment == 0.0
            ) {

                ""

            } else {

                formatter.format(
                    project.lengthAdjustment
                )
            }
        )

        b.widthAdjustmentInput.setText(
            if (
                project.widthAdjustment == 0.0
            ) {

                ""

            } else {

                formatter.format(
                    project.widthAdjustment
                )
            }
        )

        if (
            project.adjustmentType == "add"
        ) {

            b.addRadio.isChecked =
                true

        } else {

            b.subtractRadio.isChecked =
                true
        }

        operations.clear()

        project.measurements
            .forEachIndexed {
                    index,
                    item ->

                val safeOperationNumber =
                    if (
                        item.operationNumber > 0
                    ) {

                        item.operationNumber

                    } else {

                        index + 1
                    }

                operations.add(
                    item.copy(
                        operationNumber =
                            safeOperationNumber
                    )
                )
            }

        nextOperationNumber =
            if (
                operations.isEmpty()
            ) {

                1

            } else {

                operations.maxOf {
                    it.operationNumber
                } + 1
            }

        b.quantityInput.setText(
            "1"
        )

        updateOperationNumber()
        refreshOperationsList()
        focusLengthInput()

        saveAutomaticDraft()

        Toast.makeText(
            this,
            "تم فتح المشروع للمتابعة",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // حساب المقاس
    // =====================================================

    private fun calculateMeasurement(): Boolean {

        val length =
            normalizedDouble(
                b.lengthInput.text
                    .toString()
            )

        val width =
            normalizedDouble(
                b.widthInput.text
                    .toString()
            )

        val quantity =
            normalizedInt(
                b.quantityInput.text
                    .toString()
            )

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

        val totalArea =
            calculateArea(
                length,
                width,
                quantity,
                unit
            )

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
            normalizedDouble(
                b.lengthInput.text
                    .toString()
            )

        val originalWidth =
            normalizedDouble(
                b.widthInput.text
                    .toString()
            )

        val quantity =
            normalizedInt(
                b.quantityInput.text
                    .toString()
            )

        if (
            originalLength == null ||
            originalWidth == null ||
            quantity == null
        ) {

            return false
        }

        val lengthAdjustment =
            normalizedDouble(
                b.lengthAdjustmentInput.text
                    .toString()
            ) ?: 0.0

        val widthAdjustment =
            normalizedDouble(
                b.widthAdjustmentInput.text
                    .toString()
            ) ?: 0.0

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

        val totalArea =
            calculateArea(
                adjustedLength,
                adjustedWidth,
                quantity,
                unit
            )

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
    // إضافة عملية
    // =====================================================

    private fun addCurrentOperation() {

        if (
            !calculateMeasurement()
        ) {

            return
        }

        val length =
            normalizedDouble(
                b.lengthInput.text
                    .toString()
            ) ?: return

        val width =
            normalizedDouble(
                b.widthInput.text
                    .toString()
            ) ?: return

        val quantity =
            normalizedInt(
                b.quantityInput.text
                    .toString()
            ) ?: return

        val lengthAdjustment =
            normalizedDouble(
                b.lengthAdjustmentInput.text
                    .toString()
            ) ?: 0.0

        val widthAdjustment =
            normalizedDouble(
                b.widthAdjustmentInput.text
                    .toString()
            ) ?: 0.0

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

            return
        }

        operations.add(
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

        saveAutomaticDraft()

        clearCurrentOperationFields(
            keepAdjustments = true
        )

        focusLengthInput()
    }

    // =====================================================
    // تكرار العملية السابقة
    // =====================================================

    private fun repeatLastOperation() {

        val lastOperation =
            operations.maxByOrNull {
                it.operationNumber
            }

        if (
            lastOperation == null
        ) {

            Toast.makeText(
                this,
                "لا توجد عملية سابقة لتكرارها",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        b.lengthInput.setText(
            formatter.format(
                lastOperation.length
            )
        )

        b.widthInput.setText(
            formatter.format(
                lastOperation.width
            )
        )

        b.quantityInput.setText(
            lastOperation.quantity
                .toString()
        )

        val unitPosition =
            when (
                lastOperation.unit
            ) {

                "مم" ->
                    1

                "متر" ->
                    2

                else ->
                    0
            }

        b.unitSpinner.setSelection(
            unitPosition
        )

        calculateMeasurement()

        b.lengthInput.requestFocus()

        b.lengthInput.setSelection(
            b.lengthInput.text
                .length
        )

        showKeyboard()
    }

    // =====================================================
    // رقم العملية
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
    // عرض العمليات
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
                    } ${item.unit} | العدد: ${
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
                }؟\nلن تتغير أرقام العمليات الأخرى."
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

                saveAutomaticDraft()
            }

            .show()
    }

    // =====================================================
    // الحفظ التلقائي
    // =====================================================

    private fun saveAutomaticDraft() {

        if (
            operations.isEmpty()
        ) {

            ProjectDraftStorage.clearDraft(
                this
            )

            return
        }

        val draft =
            SavedProject(
                id =
                    if (
                        editingProjectId != -1L
                    ) {
                        editingProjectId
                    } else {
                        System.currentTimeMillis()
                    },

                projectName =
                    b.projectNameInput.text
                        .toString()
                        .trim(),

                customerName =
                    b.customerNameInput.text
                        .toString()
                        .trim(),

                notes =
                    b.projectNotesInput.text
                        .toString()
                        .trim(),

                createdAt =
                    if (
                        originalCreatedAt > 0L
                    ) {
                        originalCreatedAt
                    } else {
                        System.currentTimeMillis()
                    },

                adjustmentType =
                    if (
                        b.subtractRadio.isChecked
                    ) {
                        "subtract"
                    } else {
                        "add"
                    },

                lengthAdjustment =
                    normalizedDouble(
                        b.lengthAdjustmentInput.text
                            .toString()
                    ) ?: 0.0,

                widthAdjustment =
                    normalizedDouble(
                        b.widthAdjustmentInput.text
                            .toString()
                    ) ?: 0.0,

                measurements =
                    operations.map {
                        it.copy()
                    }
            )

        ProjectDraftStorage.saveDraft(
            this,
            draft
        )
    }

    // =====================================================
    // حفظ المشروع النهائي
    // =====================================================

    private fun saveCurrentProject() {

        if (
            operations.isEmpty()
        ) {

            Toast.makeText(
                this,
                "أضف عملية واحدة على الأقل قبل الحفظ",
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

        val project =
            SavedProject(
                id =
                    if (
                        editingProjectId != -1L
                    ) {
                        editingProjectId
                    } else {
                        System.currentTimeMillis()
                    },

                projectName =
                    projectName,

                customerName =
                    b.customerNameInput.text
                        .toString()
                        .trim(),

                notes =
                    b.projectNotesInput.text
                        .toString()
                        .trim(),

                createdAt =
                    if (
                        originalCreatedAt > 0L
                    ) {
                        originalCreatedAt
                    } else {
                        System.currentTimeMillis()
                    },

                adjustmentType =
                    if (
                        b.subtractRadio.isChecked
                    ) {
                        "subtract"
                    } else {
                        "add"
                    },

                lengthAdjustment =
                    normalizedDouble(
                        b.lengthAdjustmentInput.text
                            .toString()
                    ) ?: 0.0,

                widthAdjustment =
                    normalizedDouble(
                        b.widthAdjustmentInput.text
                            .toString()
                    ) ?: 0.0,

                measurements =
                    operations.map {
                        it.copy()
                    }
            )

        val success =
            if (
                editingProjectId == -1L
            ) {

                ProjectStorage.saveProject(
                    this,
                    project
                )

            } else {

                ProjectStorage.updateProject(
                    this,
                    project
                )
            }

        if (
            success
        ) {

            editingProjectId =
                project.id

            originalCreatedAt =
                project.createdAt

            b.projectNameInput.setText(
                projectName
            )

            ProjectDraftStorage.clearDraft(
                this
            )

            Toast.makeText(
                this,
                "تم حفظ المشروع وعدد عملياته ${
                    toArabicNumber(
                        operations.size
                    )
                }",
                Toast.LENGTH_LONG
            ).show()

        } else {

            Toast.makeText(
                this,
                "تعذر حفظ المشروع",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // مشروع جديد
    // =====================================================

    private fun requestNewProject() {

        AlertDialog.Builder(this)

            .setTitle(
                "مشروع جديد"
            )

            .setMessage(
                "هل تريد بدء مشروع جديد؟ تأكد من حفظ المشروع الحالي أولًا."
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

    private fun resetEntireProject() {

        operations.clear()

        nextOperationNumber =
            1

        editingProjectId =
            -1L

        originalCreatedAt =
            0L

        ProjectDraftStorage.clearDraft(
            this
        )

        b.projectNameInput.text?.clear()
        b.customerNameInput.text?.clear()
        b.projectNotesInput.text?.clear()

        clearCurrentOperationFields(
            keepAdjustments = false
        )

        updateOperationNumber()
        refreshOperationsList()
        focusLengthInput()

        Toast.makeText(
            this,
            "تم فتح مشروع جديد",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // مسح العملية الحالية
    // =====================================================

    private fun clearCurrentOperationFields(
        keepAdjustments: Boolean = false
    ) {

        b.lengthInput.text?.clear()

        b.widthInput.text?.clear()

        b.quantityInput.setText(
            "1"
        )

        if (
            !keepAdjustments
        ) {

            b.lengthAdjustmentInput.text?.clear()
            b.widthAdjustmentInput.text?.clear()

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
    // التركيز على الطول
    // =====================================================

    private fun focusLengthInput() {

        b.lengthInput.requestFocus()

        b.lengthInput.postDelayed(
            {
                showKeyboard()
            },
            150
        )
    }

    private fun showKeyboard() {

        val manager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        manager.showSoftInput(
            b.lengthInput,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    // =====================================================
    // المساحة
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

    private fun convertToMeters(
        value: Double,
        unit: String
    ): Double {

        return when (unit) {

            "مم" ->
                value / 1000.0

            "سم" ->
                value / 100.0

            else ->
                value
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
        value: String
    ): String {

        return value
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
    // اسم تلقائي
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
    // تحويل الرقم للعربي
    // =====================================================

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
    // حفظ المسودة عند مغادرة الشاشة
    // =====================================================

    override fun onPause() {

        super.onPause()

        saveAutomaticDraft()
    }
}
