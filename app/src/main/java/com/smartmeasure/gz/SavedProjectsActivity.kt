package com.smartmeasure.gz

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivitySavedProjectsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedProjectsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySavedProjectsBinding

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        b =
            ActivitySavedProjectsBinding.inflate(
                layoutInflater
            )

        setContentView(b.root)

        loadProjects()
    }

    override fun onResume() {
        super.onResume()

        loadProjects()
    }

    private fun loadProjects() {

        b.projectsContainer.removeAllViews()

        val projects =
            ProjectStorage
                .getProjects(this)
                .sortedByDescending {
                    it.createdAt
                }

        if (projects.isEmpty()) {

            b.emptyText.visibility =
                android.view.View.VISIBLE

            return
        }

        b.emptyText.visibility =
            android.view.View.GONE

        for (project in projects) {

            addProjectCard(
                project
            )
        }
    }

    private fun addProjectCard(
        project: SavedProject
    ) {

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18,
                    18,
                    18,
                    18
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
                    20
                )

                layoutParams =
                    params

                setBackgroundColor(
                    android.graphics.Color.WHITE
                )

                isClickable =
                    true

                isFocusable =
                    true
            }

        val title =
            TextView(this).apply {

                text =
                    project.projectName

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

        container.addView(title)

        if (
            project.customerName
                .isNotBlank()
        ) {

            val customer =
                TextView(this).apply {

                    text =
                        "الزبون: ${project.customerName}"

                    textSize =
                        15f

                    setPadding(
                        0,
                        8,
                        0,
                        0
                    )
                }

            container.addView(
                customer
            )
        }

        val count =
            TextView(this).apply {

                text =
                    "عدد المقاسات: ${project.measurements.size}"

                textSize =
                    15f

                setPadding(
                    0,
                    8,
                    0,
                    0
                )
            }

        container.addView(
            count
        )

        val date =
            TextView(this).apply {

                text =
                    "التاريخ: ${formatDate(project.createdAt)}"

                textSize =
                    14f

                setPadding(
                    0,
                    8,
                    0,
                    0
                )

                setTextColor(
                    android.graphics.Color.DKGRAY
                )
            }

        container.addView(
            date
        )

        container.setOnClickListener {

            val intent =
                Intent(
                    this,
                    ProjectDetailsActivity::class.java
                )

            intent.putExtra(
                "projectId",
                project.id
            )

            startActivity(
                intent
            )
        }

        b.projectsContainer.addView(
            container
        )
    }

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
}
