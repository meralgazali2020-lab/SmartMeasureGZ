package com.smartmeasure.gz

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityNotesBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesActivity : AppCompatActivity() {

    private lateinit var b: ActivityNotesBinding

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivityNotesBinding.inflate(
                layoutInflater
            )

        setContentView(b.root)

        b.addNoteBtn.setOnClickListener {
            showAddNoteDialog()
        }

        loadNotes()
    }

    override fun onResume() {
        super.onResume()

        loadNotes()
    }

    private fun loadNotes() {

        b.notesContainer.removeAllViews()

        val notes =
            NotesStorage
                .getNotes(this)
                .sortedByDescending {
                    it.createdAt
                }

        b.emptyText.visibility =
            if (notes.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

        for (note in notes) {
            addNoteCard(note)
        }
    }

    private fun addNoteCard(
        note: NoteItem
    ) {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18,
                    18,
                    18,
                    18
                )

                setBackgroundColor(
                    android.graphics.Color.WHITE
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
                    18
                )

                layoutParams =
                    params
            }

        val title =
            TextView(this).apply {

                text =
                    note.title

                textSize =
                    19f

                setTextColor(
                    android.graphics.Color.parseColor(
                        "#0B2341"
                    )
                )

                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
            }

        val content =
            TextView(this).apply {

                text =
                    note.content

                textSize =
                    16f

                setPadding(
                    0,
                    10,
                    0,
                    0
                )
            }

        val date =
            TextView(this).apply {

                text =
                    formatDate(
                        note.createdAt
                    )

                textSize =
                    13f

                setTextColor(
                    android.graphics.Color.GRAY
                )

                setPadding(
                    0,
                    12,
                    0,
                    0
                )
            }

        card.addView(title)
        card.addView(content)
        card.addView(date)

        card.setOnLongClickListener {

            showDeleteDialog(note)

            true
        }

        b.notesContainer.addView(card)
    }

    private fun showAddNoteDialog() {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    35,
                    15,
                    35,
                    0
                )
            }

        val titleInput =
            EditText(this).apply {

                hint =
                    "عنوان الملاحظة"
            }

        val contentInput =
            EditText(this).apply {

                hint =
                    "اكتب الملاحظة هنا"

                minLines =
                    6

                gravity =
                    android.view.Gravity.TOP

                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }

        container.addView(titleInput)
        container.addView(contentInput)

        AlertDialog.Builder(this)
            .setTitle(
                "ملاحظة جديدة"
            )
            .setView(container)
            .setPositiveButton(
                "حفظ"
            ) { _, _ ->

                val title =
                    titleInput.text
                        .toString()
                        .trim()

                val content =
                    contentInput.text
                        .toString()
                        .trim()

                if (content.isBlank()) {

                    Toast.makeText(
                        this,
                        "اكتب الملاحظة أولًا",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val note =
                    NoteItem(
                        id =
                            System.currentTimeMillis(),

                        title =
                            if (title.isBlank()) {
                                "ملاحظة"
                            } else {
                                title
                            },

                        content =
                            content,

                        createdAt =
                            System.currentTimeMillis()
                    )

                NotesStorage.saveNote(
                    this,
                    note
                )

                loadNotes()
            }
            .setNegativeButton(
                "إلغاء",
                null
            )
            .show()
    }

    private fun showDeleteDialog(
        note: NoteItem
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "حذف الملاحظة"
            )
            .setMessage(
                "هل تريد حذف هذه الملاحظة؟"
            )
            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                NotesStorage.deleteNote(
                    this,
                    note.id
                )

                loadNotes()
            }
            .setNegativeButton(
                "إلغاء",
                null
            )
            .show()
    }

    private fun formatDate(
        value: Long
    ): String {

        return SimpleDateFormat(
            "yyyy/MM/dd - HH:mm",
            Locale.getDefault()
        ).format(
            Date(value)
        )
    }
}
