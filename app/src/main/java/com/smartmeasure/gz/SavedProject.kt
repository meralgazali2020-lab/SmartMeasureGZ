package com.smartmeasure.gz

data class SavedProject(

    val id: Long,

    val projectName: String,

    val customerName: String,

    val notes: String,

    val createdAt: Long,

    val adjustmentType: String,

    val lengthAdjustment: Double,

    val widthAdjustment: Double,

    val measurements: List<MeasurementItem>
)
