package com.smartmeasure.gz

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityCalculatorBinding
import java.text.DecimalFormat

class CalculatorActivity : AppCompatActivity() {

    private lateinit var b: ActivityCalculatorBinding

    private val formatter = DecimalFormat("#.###")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUnitSpinner()
        setupButtons()
    }

    private fun setupUnitSpinner() {
        val units = listOf(
            "سم",
            "مم",
            "متر"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            units
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        b.unitSpinner.adapter = adapter
    }

    private fun setupButtons() {

        b.calculateBtn.setOnClickListener {
            calculateMeasurement()
        }

        b.applyAdjustmentBtn.setOnClickListener {
            applyAdjustment()
        }

        b.clearBtn.setOnClickListener {
            clearFields()
        }
    }

    private fun calculateMeasurement() {

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

            return
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

            return
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
            lengthMeters * widthMeters

        val totalArea =
            areaPerPiece * quantity

        b.originalMeasurementText.text =
            "${formatter.format(length)} × " +
            "${formatter.format(width)} $unit"

        b.originalQuantityText.text =
            quantity.toString()

        b.originalAreaText.text =
            "${formatter.format(totalArea)} م²"

        applyAdjustment()
    }

    private fun applyAdjustment() {

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

        val adjustedLength =
            if (b.subtractRadio.isChecked) {
                originalLength -
                    lengthAdjustment
            } else {
                originalLength +
                    lengthAdjustment
            }

        val adjustedWidth =
            if (b.subtractRadio.isChecked) {
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

            return
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

    private fun clearFields() {

        b.lengthInput.text?.clear()
        b.widthInput.text?.clear()
        b.quantityInput.text?.clear()

        b.lengthAdjustmentInput.text?.clear()
        b.widthAdjustmentInput.text?.clear()

        b.subtractRadio.isChecked =
            true

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
}
