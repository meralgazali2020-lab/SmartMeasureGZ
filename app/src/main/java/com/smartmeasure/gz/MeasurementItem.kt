package com.smartmeasure.gz

data class MeasurementItem(
    var length: Double,
    var width: Double,
    var quantity: Int = 1,
    var unit: String = "سم",
    var adjustedLength: Double = length,
    var adjustedWidth: Double = width
)
