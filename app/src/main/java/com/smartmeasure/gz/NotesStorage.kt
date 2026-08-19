package com.smartmeasure.gz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NotesStorage {

    private const val PREFS_NAME =
        "smart_measure_notes"

    private const val KEY_NOTES =
        "saved_notes"

    fun saveNote(
        context: Context,
        note: NoteItem
    ): Boolean {

        return try {

            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val oldJson =
                prefs.getString(
                    KEY_NOTES,
                    "[]"
                ) ?: "[]"

            val array =
                JSONArray(oldJson)

            array.put(
                noteToJson(
                    note
                )
            )

            prefs.edit()
                .putString(
                    KEY_NOTES,
                    array.toString()
                )
                .apply()

            true

        } catch (e: Exception) {

            false
        }
    }

    fun getNotes(
        context: Context
    ): MutableList<NoteItem> {

        val result =
            mutableListOf<NoteItem>()

        try {

            val prefs =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val json =
                prefs.getString(
                    KEY_NOTES,
                    "[]"
                ) ?: "[]"

            val array =
                JSONArray(json)

            for (
                index in
                0 until array.length()
            ) {

                result.add(
                    jsonToNote(
                        array.getJSONObject(index)
                    )
                )
            }

        } catch (_: Exception) {

        }

        return result
    }

    fun deleteNote(
        context: Context,
        noteId: Long
    ): Boolean {

        return try {

            val notes =
                getNotes(context)

            notes.removeAll {
                it.id == noteId
            }

            saveAll(
                context,
                notes
            )

            true

        } catch (e: Exception) {

            false
        }
    }

    private fun saveAll(
        context: Context,
        notes: List<NoteItem>
    ) {

        val array =
            JSONArray()

        for (note in notes) {

            array.put(
                noteToJson(
                    note
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
                KEY_NOTES,
                array.toString()
            )
            .apply()
    }

    private fun noteToJson(
        note: NoteItem
    ): JSONObject {

        return JSONObject().apply {

            put(
                "id",
                note.id
            )

            put(
                "title",
                note.title
            )

            put(
                "content",
                note.content
            )

            put(
                "createdAt",
                note.createdAt
            )
        }
    }

    private fun jsonToNote(
        value: JSONObject
    ): NoteItem {

        return NoteItem(
            id =
                value.optLong(
                    "id",
                    System.currentTimeMillis()
                ),

            title =
                value.optString(
                    "title",
                    "ملاحظة"
                ),

            content =
                value.optString(
                    "content",
                    ""
                ),

            createdAt =
                value.optLong(
                    "createdAt",
                    System.currentTimeMillis()
                )
        )
    }
}
