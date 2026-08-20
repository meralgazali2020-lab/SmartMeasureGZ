package com.smartmeasure.gz

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityQuickCalculatorBinding
import java.text.DecimalFormat

class QuickCalculatorActivity : AppCompatActivity() {

    private lateinit var b: ActivityQuickCalculatorBinding

    private val formatter =
        DecimalFormat("#.########")

    private var firstNumber: Double? =
        null

    private var currentOperator: String? =
        null

    private var startNewNumber =
        true

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivityQuickCalculatorBinding.inflate(
                layoutInflater
            )

        setContentView(
            b.root
        )

        setupButtons()
    }

    private fun setupButtons() {

        b.btn0.setOnClickListener { addDigit("0") }
        b.btn1.setOnClickListener { addDigit("1") }
        b.btn2.setOnClickListener { addDigit("2") }
        b.btn3.setOnClickListener { addDigit("3") }
        b.btn4.setOnClickListener { addDigit("4") }
        b.btn5.setOnClickListener { addDigit("5") }
        b.btn6.setOnClickListener { addDigit("6") }
        b.btn7.setOnClickListener { addDigit("7") }
        b.btn8.setOnClickListener { addDigit("8") }
        b.btn9.setOnClickListener { addDigit("9") }

        b.decimalBtn.setOnClickListener {
            addDecimal()
        }

        b.addBtn.setOnClickListener {
            chooseOperator("+")
        }

        b.subtractBtn.setOnClickListener {
            chooseOperator("-")
        }

        b.multiplyBtn.setOnClickListener {
            chooseOperator("×")
        }

        b.divideBtn.setOnClickListener {
            chooseOperator("÷")
        }

        b.equalsBtn.setOnClickListener {
            calculateResult()
        }

        b.squareMeterBtn.setOnClickListener {
            calculateSquareMeters()
        }

        b.clearBtn.setOnClickListener {
            clearCalculator()
        }

        b.backspaceBtn.setOnClickListener {
            backspace()
        }
    }

    // =====================================================
    // إدخال رقم
    // =====================================================

    private fun addDigit(
        digit: String
    ) {

        if (startNewNumber) {

            b.displayText.text =
                digit

            startNewNumber =
                false

            return
        }

        val current =
            b.displayText.text
                .toString()

        if (
            current == "0"
        ) {

            b.displayText.text =
                digit

        } else {

            b.displayText.text =
                current + digit
        }
    }

    // =====================================================
    // فاصلة عشرية
    // =====================================================

    private fun addDecimal() {

        if (startNewNumber) {

            b.displayText.text =
                "0."

            startNewNumber =
                false

            return
        }

        val current =
            b.displayText.text
                .toString()

        if (
            !current.contains(".")
        ) {

            b.displayText.text =
                "$current."
        }
    }

    // =====================================================
    // اختيار العملية
    // =====================================================

    private fun chooseOperator(
        operator: String
    ) {

        val current =
            getDisplayNumber()
                ?: return

        if (
            firstNumber != null &&
            currentOperator != null &&
            !startNewNumber
        ) {

            val result =
                performCalculation(
                    firstNumber!!,
                    current,
                    currentOperator!!
                ) ?: return

            firstNumber =
                result

            b.displayText.text =
                formatter.format(
                    result
                )

        } else {

            firstNumber =
                current
        }

        currentOperator =
            operator

        b.operationText.text =
            "${formatter.format(firstNumber)} $operator"

        startNewNumber =
            true
    }

    // =====================================================
    // =
    // =====================================================

    private fun calculateResult() {

        val first =
            firstNumber
                ?: return

        val operator =
            currentOperator
                ?: return

        val second =
            getDisplayNumber()
                ?: return

        val result =
            performCalculation(
                first,
                second,
                operator
            ) ?: return

        b.operationText.text =
            "${formatter.format(first)} $operator " +
                "${formatter.format(second)} ="

        b.displayText.text =
            formatter.format(
                result
            )

        firstNumber =
            null

        currentOperator =
            null

        startNewNumber =
            true
    }

    // =====================================================
    // المتر المربع
    //
    // مثال:
    // 120 × 80 ثم م²
    // النتيجة = 0.96 م²
    //
    // المقاسات هنا محسوبة بالسنتيمتر
    // =====================================================

    private fun calculateSquareMeters() {

        val first =
            firstNumber

        val operator =
            currentOperator

        val second =
            getDisplayNumber()

        if (
            first == null ||
            second == null ||
            operator != "×"
        ) {

            Toast.makeText(
                this,
                "لحساب م² أدخل: الطول × العرض ثم اضغط م²",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val squareMeters =
            (first * second) /
                10000.0

        b.operationText.text =
            "${formatter.format(first)} × " +
                "${formatter.format(second)} سم"

        b.displayText.text =
            "${formatter.format(squareMeters)} م²"

        firstNumber =
            null

        currentOperator =
            null

        startNewNumber =
            true
    }

    // =====================================================
    // تنفيذ العملية
    // =====================================================

    private fun performCalculation(
        first: Double,
        second: Double,
        operator: String
    ): Double? {

        return when (
            operator
        ) {

            "+" ->
                first + second

            "-" ->
                first - second

            "×" ->
                first * second

            "÷" -> {

                if (
                    second == 0.0
                ) {

                    Toast.makeText(
                        this,
                        "لا يمكن القسمة على صفر",
                        Toast.LENGTH_SHORT
                    ).show()

                    null

                } else {

                    first / second
                }
            }

            else ->
                null
        }
    }

    // =====================================================
    // مسح كامل
    // =====================================================

    private fun clearCalculator() {

        firstNumber =
            null

        currentOperator =
            null

        startNewNumber =
            true

        b.displayText.text =
            "0"

        b.operationText.text =
            ""
    }

    // =====================================================
    // حذف رقم
    // =====================================================

    private fun backspace() {

        if (startNewNumber) {
            return
        }

        val current =
            b.displayText.text
                .toString()

        if (
            current.length <= 1
        ) {

            b.displayText.text =
                "0"

            startNewNumber =
                true

        } else {

            b.displayText.text =
                current.dropLast(1)
        }
    }

    // =====================================================
    // الرقم الموجود على الشاشة
    // =====================================================

    private fun getDisplayNumber(): Double? {

        return b.displayText.text
            .toString()
            .replace(
                " م²",
                ""
            )
            .trim()
            .toDoubleOrNull()
    }
}
