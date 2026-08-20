package com.smartmeasure.gz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ProjectDraftStorage {

    private const val PREFS_NAME =
        "smart_measure_draft"

    private const val KEY_DRAFT =
        "current_project_draft"

    // =====================================================
    // حفظ المشروع الجاري تلقائياً
    // =====================================================

    fun saveDraft(
        context: Context,
        project: SavedProject
    ): Boolean {

        return try {

            val json =
                projectToJson(
                    project
                )

            context
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                    KEY_DRAFT,
                    json.toString()
                )
                .apply()

            true

        } catch (_: Exception) {

            false
        }
    }

    // =====================================================
    // قراءة المشروع الجاري
    // =====================================================

    fun getDraft(
        context: Context
    ): SavedProject? {

        return try {

            val jsonText =
                context
                    .getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
                    .getString(
                        KEY_DRAFT,
                        null
                    )
                    ?: return null

            val json =
                JSONObject(
                    jsonText
                )

            jsonToProject(
                json
            )

        } catch (_: Exception) {

            null
        }
    }

    // =====================================================
    // هل يوجد مشروع غير محفوظ؟
    // =====================================================

    fun hasDraft(
        context: Context
    ): Boolean {

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .contains(
                KEY_DRAFT
            )
    }

    // =====================================================
    // حذف المسودة
    // =====================================================

    fun clearDraft(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_DRAFT
            )
            .apply()
    }

    // =====================================================
    // تحويل المشروع إلى JSON
    // =====================================================

    private fun projectToJson(
        project: SavedProject
    ): JSONObject {

        val json =
            JSONObject()

        json.put(
            "id",
            project.id
        )

        json.put(
            "projectName",
            project.projectName
        )

        json.put(
            "customerName",
            project.customerName
        )

        json.put(
            "notes",
            project.notes
        )

        json.put(
            "createdAt",
            project.createdAt
        )

        json.put(
            "adjustmentType",
            project.adjustmentType
        )

        json.put(
            "lengthAdjustment",
            project.lengthAdjustment
        )

        json.put(
            "widthAdjustment",
            project.widthAdjustment
        )

        val measurementsArray =
            JSONArray()

        for (
            item in
            project.measurements
        ) {

            val itemJson =
                JSONObject()

            itemJson.put(
                "operationNumber",
                item.operationNumber
            )

            itemJson.put(
                "length",
                item.length
            )

            itemJson.put(
                "width",
                item.width
            )

            itemJson.put(
                "quantity",
                item.quantity
            )

            itemJson.put(
                "unit",
                item.unit
            )

            itemJson.put(
                "adjustedLength",
                item.adjustedLength
            )

            itemJson.put(
                "adjustedWidth",
                item.adjustedWidth
            )

            measurementsArray.put(
                itemJson
            )
        }

        json.put(
            "measurements",
            measurementsArray
        )

        return json
    }

    // =====================================================
    // تحويل JSON إلى مشروع
    // =====================================================

    private fun jsonToProject(
        json: JSONObject
    ): SavedProject {

        val measurements =
            mutableListOf<MeasurementItem>()

        val measurementsArray =
            json.optJSONArray(
                "measurements"
            ) ?: JSONArray()

        for (
            index in
            0 until measurementsArray.length()
        ) {

            val itemJson =
                measurementsArray
                    .getJSONObject(
                        index
                    )

            val length =
                itemJson.optDouble(
                    "length",
                    0.0
                )

            val width =
                itemJson.optDouble(
                    "width",
                    0.0
                )

            measurements.add(
                MeasurementItem(

                    length =
                        length,

                    width =
                        width,

                    quantity =
                        itemJson.optInt(
                            "quantity",
                            1
                        ),

                    unit =
                        itemJson.optString(
                            "unit",
                            "سم"
                        ),

                    adjustedLength =
                        itemJson.optDouble(
                            "adjustedLength",
                            length
                        ),

                    adjustedWidth =
                        itemJson.optDouble(
                            "adjustedWidth",
                            width
                        ),

                    operationNumber =
                        itemJson.optInt(
                            "operationNumber",
                            index + 1
                        )
                )
            )
        }

        return SavedProject(

            id =
                json.optLong(
                    "id",
                    System.currentTimeMillis()
                ),

            projectName =
                json.optString(
                    "projectName",
                    ""
                ),

            customerName =
                json.optString(
                    "customerName",
                    ""
                ),

            notes =
                json.optString(
                    "notes",
                    ""
                ),

            createdAt =
                json.optLong(
                    "createdAt",
                    System.currentTimeMillis()
                ),

            adjustmentType =
                json.optString(
                    "adjustmentType",
                    "subtract"
                ),

            lengthAdjustment =
                json.optDouble(
                    "lengthAdjustment",
                    0.0
                ),

            widthAdjustment =
                json.optDouble(
                    "widthAdjustment",
                    0.0
                ),

            measurements =
                measurements
        )
    }
}
