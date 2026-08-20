package com.smartmeasure.gz

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityProjectDetailsBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectDetailsActivity : AppCompatActivity() {

    private lateinit var b: ActivityProjectDetailsBinding

    private var projectId: Long = -1L

    private var currentProject: SavedProject? = null

    private val formatter =
        DecimalFormat("#.###")

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivityProjectDetailsBinding.inflate(
                layoutInflater
            )

        setContentView(b.root)

        projectId =
            intent.getLongExtra(
                "projectId",
                -1L
            )

        if (projectId == -1L) {

            Toast.makeText(
                this,
                "تعذر فتح المشروع",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        setupButtons()

        loadProject()
    }

    override fun onResume() {
        super.onResume()

        if (projectId != -1L) {
            loadProject()
        }
    }

    // =====================================================
    // الأزرار
    // =====================================================

    private fun setupButtons() {

        b.deleteProjectBtn.setOnClickListener {

            showDeleteProjectDialog()
        }
    }

    // =====================================================
    // تحميل المشروع
    // =====================================================

    private fun loadProject() {

        val project =
            ProjectStorage
                .getProjects(this)
                .firstOrNull {
                    it.id == projectId
                }

        if (project == null) {

            Toast.makeText(
                this,
                "المشروع غير موجود",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        currentProject =
            project

        showProjectInformation(
            project
        )

        showMeasurements(
            project
        )
    }

    // =====================================================
    // معلومات المشروع
    // =====================================================

    private fun showProjectInformation(
        project: SavedProject
    ) {

        b.projectNameText.text =
            if (
                project.projectName
                    .isBlank()
            ) {
                "مشروع بدون اسم"
            } else {
                project.projectName
            }

        b.customerNameText.text =
            if (
                project.customerName
                    .isBlank()
            ) {

                "الزبون: غير محدد"

            } else {

                "الزبون: ${project.customerName}"
            }

        b.dateText.text =
            "التاريخ: ${
                formatDate(
                    project.createdAt
                )
            }"

        val operationType =
            when (
                project.adjustmentType
            ) {

                "add" ->
                    "زيادة"

                else ->
                    "تنقيص"
            }

        b.adjustmentText.text =
            buildString {

                append(
                    "الإجراء: "
                )

                append(
                    operationType
                )

                if (
                    project.lengthAdjustment != 0.0 ||
                    project.widthAdjustment != 0.0
                ) {

                    append(
                        " | الطول: "
                    )

                    append(
                        formatter.format(
                            project.lengthAdjustment
                        )
                    )

                    append(
                        " | العرض: "
                    )

                    append(
                        formatter.format(
                            project.widthAdjustment
                        )
                    )
                }
            }

        b.notesText.text =
            if (
                project.notes
                    .isBlank()
            ) {

                "لا توجد ملاحظات"

            } else {

                project.notes
            }
    }

    // =====================================================
    // عرض جميع العمليات
    // =====================================================

    private fun showMeasurements(
        project: SavedProject
    ) {

        clearOldRows()

        var originalTotalArea =
            0.0

        var adjustedTotalArea =
            0.0

        project.measurements
            .sortedBy {
                it.operationNumber
            }
            .forEachIndexed {
                    index,
                    item ->

                val operationNumber =
                    if (
                        item.operationNumber > 0
                    ) {

                        item.operationNumber

                    } else {

                        index + 1
                    }

                val originalArea =
                    calculateArea(
                        length = item.length,
                        width = item.width,
                        quantity = item.quantity,
                        unit = item.unit
                    )

                val adjustedArea =
                    calculateArea(
                        length =
                            item.adjustedLength,
                        width =
                            item.adjustedWidth,
                        quantity =
                            item.quantity,
                        unit =
                            item.unit
                    )

                originalTotalArea +=
                    originalArea

                adjustedTotalArea +=
                    adjustedArea

                addMeasurementRow(
                    operationNumber =
                        operationNumber,

                    length =
                        item.length,

                    width =
                        item.width,

                    quantity =
                        item.quantity,

                    area =
                        originalArea,

                    unit =
                        item.unit,

                    adjusted =
                        false
                )

                addMeasurementRow(
                    operationNumber =
                        operationNumber,

                    length =
                        item.adjustedLength,

                    width =
                        item.adjustedWidth,

                    quantity =
                        item.quantity,

                    area =
                        adjustedArea,

                    unit =
                        item.unit,

                    adjusted =
                        true
                )
            }

        b.originalTotalText.text =
            "إجمالي الأصلي: ${
                formatter.format(
                    originalTotalArea
                )
            } م²"

        b.adjustedTotalText.text =
            "إجمالي المعدل: ${
                formatter.format(
                    adjustedTotalArea
                )
            } م²"
    }

    // =====================================================
    // إضافة صف إلى الجدول
    // =====================================================

    private fun addMeasurementRow(
        operationNumber: Int,
        length: Double,
        width: Double,
        quantity: Int,
        area: Double,
        unit: String,
        adjusted: Boolean
    ) {

        val row =
            TableRow(this)

        row.setBackgroundColor(
            Color.WHITE
        )

        row.addView(
            createTableCell(
                convertToArabicNumbers(
                    operationNumber
                        .toString()
                ),
                bold = true
            )
        )

        row.addView(
            createTableCell(
                "${
                    formatter.format(
                        length
                    )
                } $unit"
            )
        )

        row.addView(
            createTableCell(
                "${
                    formatter.format(
                        width
                    )
                } $unit"
            )
        )

        row.addView(
            createTableCell(
                convertToArabicNumbers(
                    quantity
                        .toString()
                )
            )
        )

        row.addView(
            createTableCell(
                formatter.format(
                    area
                )
            )
        )

        if (adjusted) {

            b.adjustedTable.addView(
                row
            )

        } else {

            b.originalTable.addView(
                row
            )
        }
    }

    // =====================================================
    // إنشاء خلية
    // =====================================================

    private fun createTableCell(
        value: String,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {

            text =
                value

            textSize =
                15f

            gravity =
                Gravity.CENTER

            setPadding(
                16,
                16,
                16,
                16
            )

            minWidth =
                90

            setTextColor(
                Color.parseColor(
                    "#222222"
                )
            )

            if (bold) {

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
        }
    }

    // =====================================================
    // حذف الصفوف القديمة
    // مع إبقاء صف العناوين
    // =====================================================

    private fun clearOldRows() {

        if (
            b.originalTable.childCount > 1
        ) {

            b.originalTable.removeViews(
                1,
                b.originalTable.childCount - 1
            )
        }

        if (
            b.adjustedTable.childCount > 1
        ) {

            b.adjustedTable.removeViews(
                1,
                b.adjustedTable.childCount - 1
            )
        }
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

        return lengthMeters *
            widthMeters *
            quantity
    }

    // =====================================================
    // تحويل إلى متر
    // =====================================================

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
    // حذف المشروع
    // =====================================================

    private fun showDeleteProjectDialog() {

        val project =
            currentProject
                ?: return

        AlertDialog.Builder(this)

            .setTitle(
                "حذف المشروع"
            )

            .setMessage(
                "هل تريد حذف مشروع \"${project.projectName}\" وجميع عملياته؟"
            )

            .setNegativeButton(
                "إلغاء",
                null
            )

            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                deleteProject(
                    project.id
                )
            }

            .show()
    }

    private fun deleteProject(
        id: Long
    ) {

        val deleted =
            ProjectStorage.deleteProject(
                this,
                id
            )

        if (deleted) {

            Toast.makeText(
                this,
                "تم حذف المشروع",
                Toast.LENGTH_SHORT
            ).show()

            finish()

        } else {

            Toast.makeText(
                this,
                "تعذر حذف المشروع",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =====================================================
    // تنسيق التاريخ
    // =====================================================

    private fun formatDate(
        value: Long
    ): String {

        val formatter =
            SimpleDateFormat(
                "yyyy/MM/dd - HH:mm",
                Locale.getDefault()
            )

        return formatter.format(
            Date(value)
        )
    }

    // =====================================================
    // تحويل الأرقام إلى العربية
    // 1 -> ١
    // =====================================================

    private fun convertToArabicNumbers(
        value: String
    ): String {

        return value
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
