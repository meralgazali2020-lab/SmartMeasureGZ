package com.smartmeasure.gz

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.calculatorBtn.setOnClickListener {

            val intent =
                Intent(
                    this,
                    CalculatorActivity::class.java
                )

            startActivity(intent)
        }

        b.newProjectBtn.setOnClickListener {
            Toast.makeText(
                this,
                "إنشاء مشروع جديد",
                Toast.LENGTH_SHORT
            ).show()
        }

        b.scanMeasurementsBtn.setOnClickListener {
            Toast.makeText(
                this,
                "تصوير ورقة المقاسات",
                Toast.LENGTH_SHORT
            ).show()
        }

        b.savedProjectsBtn.setOnClickListener {
            Toast.makeText(
                this,
                "المشاريع والمقاسات المحفوظة",
                Toast.LENGTH_SHORT
            ).show()
        }

        b.notesBtn.setOnClickListener {
            Toast.makeText(
                this,
                "الملاحظات",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
