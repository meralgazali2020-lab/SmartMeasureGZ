package com.smartmeasure.gz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ProjectStorage {

    private const val PREFS_NAME =
        "smart_measure_projects"

    private const val KEY_PROJECTS =
        "saved_projects"

    // =====================================================
    // حفظ مشروع جديد
    // =====================================================

    fun saveProject(
        context: Context,
        project: SavedProject
    ): Boolean {

        return try {

            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val oldJson =
                prefs.getString(
                    KEY_PROJECTS,
                    "[]"
                ) ?: "[]"

            val projectsArray =
                JSONArray(oldJson)

            val projectObject =
                projectToJson(
                    project
                )

            projectsArray.put(
                projectObject
            )

            prefs.edit()
                .putString(
                    KEY_PROJECTS,
                    projectsArray.toString()
                )
                .apply()

            true

        } catch (e: Exception) {

            false
        }
    }

    // =====================================================
    // قراءة جميع المشاريع
    // =====================================================

    fun getProjects(
        context: Context
    ): MutableList<SavedProject> {

        val result =
            mutableListOf<SavedProject>()

        try {

            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val json =
                prefs.getString(
                    KEY_PROJECTS,
                    "[]"
                ) ?: "[]"

            val array =
                JSONArray(json)

            for (
                index in
                0 until array.length()
            ) {

                val projectObject =
                    array.getJSONObject(
                        index
                    )

                result.add(
                    jsonToProject(
                        projectObject
                    )
                )
            }

        } catch (_: Exception) {

        }

        return result
    }

    // =====================================================
    // حذف مشروع
    // =====================================================

    fun deleteProject(
        context: Context,
        projectId: Long
    ): Boolean {

        return try {

            val projects =
                getProjects(context)

            projects.removeAll {
                it.id == projectId
            }

            saveAllProjects(
                context,
                projects
            )

            true

        } catch (e: Exception) {

            false
        }
    }

    // =====================================================
    // تحديث مشروع موجود
    // =====================================================

    fun updateProject(
        context: Context,
        updatedProject: SavedProject
    ): Boolean {

        return try {

            val projects =
                getProjects(context)

            val index =
                projects.indexOfFirst {
                    it.id ==
                        updatedProject.id
                }

            if (index == -1) {

                return false
            }

            projects[index] =
                updatedProject

            saveAllProjects(
                context,
                projects
            )

            true

        } catch (e: Exception) {

            false
        }
    }

    // =====================================================
    // حفظ قائمة المشاريع كاملة
    // =====================================================

    private fun saveAllProjects(
        context: Context,
        projects: List<SavedProject>
    ) {

        val array =
            JSONArray()

        for (project in projects) {

            array.put(
                projectToJson(
                    project
                )
            )
        }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_PROJECTS,
                array.toString()
            )
            .apply()
    }

    // =====================================================
    // تحويل المشروع إلى JSON
    // =====================================================

    private fun projectToJson(
        project: SavedProject
    ): JSONObject {

        val objectValue =
            JSONObject()

        objectValue.put(
            "id",
            project.id
        )

        objectValue.put(
            "projectName",
            project.projectName
        )

        objectValue.put(
            "customerName",
            project.customerName
        )

        objectValue.put(
            "notes",
            project.notes
        )

        objectValue.put(
            "createdAt",
            project.createdAt
        )

        objectValue.put(
            "adjustmentType",
            project.adjustmentType
        )

        objectValue.put(
            "lengthAdjustment",
            project.lengthAdjustment
        )

        objectValue.put(
            "widthAdjustment",
            project.widthAdjustment
        )

        val measurementsArray =
            JSONArray()

        for (
            item in
            project.measurements
        ) {

            val measurementObject =
                JSONObject()

            measurementObject.put(
                "operationNumber",
                item.operationNumber
            )

            measurementObject.put(
                "length",
                item.length
            )

            measurementObject.put(
                "width",
                item.width
            )

            measurementObject.put(
                "quantity",
                item.quantity
            )

            measurementObject.put(
                "unit",
                item.unit
            )

            measurementObject.put(
                "adjustedLength",
                item.adjustedLength
            )

            measurementObject.put(
                "adjustedWidth",
                item.adjustedWidth
            )

            measurementsArray.put(
                measurementObject
            )
        }

        objectValue.put(
            "measurements",
            measurementsArray
        )

        return objectValue
    }

    // =====================================================
    // تحويل JSON إلى مشروع
    // =====================================================

    private fun jsonToProject(
        objectValue: JSONObject
    ): SavedProject {

        val measurements =
            mutableListOf<MeasurementItem>()

        val measurementsArray =
            objectValue.optJSONArray(
                "measurements"
            ) ?: JSONArray()

        for (
            index in
            0 until measurementsArray.length()
        ) {

            val measurementObject =
                measurementsArray
                    .getJSONObject(index)

            val operationNumber =
                measurementObject.optInt(
                    "operationNumber",
                    index + 1
                )

            measurements.add(
                MeasurementItem(
                    length =
                        measurementObject
                            .optDouble(
                                "length",
                                0.0
                            ),

                    width =
                        measurementObject
                            .optDouble(
                                "width",
                                0.0
                            ),

                    quantity =
                        measurementObject
                            .optInt(
                                "quantity",
                                1
                            ),

                    unit =
                        measurementObject
                            .optString(
                                "unit",
                                "سم"
                            ),

                    adjustedLength =
                        measurementObject
                            .optDouble(
                                "adjustedLength",
                                measurementObject
                                    .optDouble(
                                        "length",
                                        0.0
                                    )
                            ),

                    adjustedWidth =
                        measurementObject
                            .optDouble(
                                "adjustedWidth",
                                measurementObject
                                    .optDouble(
                                        "width",
                                        0.0
                                    )
                            ),

                    operationNumber =
                        operationNumber
                )
            )
        }

        return SavedProject(

            id =
                objectValue.optLong(
                    "id",
                    System.currentTimeMillis()
                ),

            projectName =
                objectValue.optString(
                    "projectName",
                    "مشروع"
                ),

            customerName =
                objectValue.optString(
                    "customerName",
                    ""
                ),

            notes =
                objectValue.optString(
                    "notes",
                    ""
                ),

            createdAt =
                objectValue.optLong(
                    "createdAt",
                    System.currentTimeMillis()
                ),

            adjustmentType =
                objectValue.optString(
                    "adjustmentType",
                    "subtract"
                ),

            lengthAdjustment =
                objectValue.optDouble(
                    "lengthAdjustment",
                    0.0
                ),

            widthAdjustment =
                objectValue.optDouble(
                    "widthAdjustment",
                    0.0
                ),

            measurements =
                measurements
        )
    }
}
