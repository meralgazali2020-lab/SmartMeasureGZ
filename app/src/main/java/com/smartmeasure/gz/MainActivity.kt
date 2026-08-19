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

        b =
            ActivityMainBinding.inflate(
                layoutInflater
            )

        setContentView(b.root)

        b.calculatorBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CalculatorActivity::class.java
                )
            )
        }

        b.scanMeasurementsBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ScanMeasurementsActivity::class.java
                )
            )
        }

        b.savedProjectsBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SavedProjectsActivity::class.java
                )
            )
        }

        b.newProjectBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ScanMeasurementsActivity::class.java
                )
            )
        }

        b.notesBtn.setOnClickListener {

            Toast.makeText(
                this,
                "سنضيف دفتر الملاحظات في الخطوة التالية",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
