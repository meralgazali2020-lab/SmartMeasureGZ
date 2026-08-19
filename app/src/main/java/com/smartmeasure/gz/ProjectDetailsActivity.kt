package com.smartmeasure.gz

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class ProjectDetailsActivity : AppCompatActivity() {

    private val formatter =
        DecimalFormat("#.##")

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val projectName =
            intent.getStringExtra("projectName")
                ?: "المشروع"

        val measurements =
            intent.getStringExtra("measurements")
                ?: "لا توجد مقاسات"

        val totalArea =
            intent.getDoubleExtra(
                "totalArea",
                0.0
            )

        val textView =
            TextView(this).apply {

                textSize = 18f

                setPadding(
                    32,
                    40,
                    32,
                    40
                )

                text = """
                    Smart Measure GZ

                    $projectName

                    المقاسات:
                    $measurements

                    إجمالي المساحة:
                    ${formatter.format(totalArea)} م²


                    تصميم المهندس حسين الغزالي
                """.trimIndent()
            }

        setContentView(textView)
    }
}
