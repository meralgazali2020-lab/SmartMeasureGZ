package com.smartmeasure.gz

import android.content.Intent
import android.os.Bundle
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

        b.newProjectBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ScanMeasurementsActivity::class.java
                )
            )
        }

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

        b.notesBtn.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    NotesActivity::class.java
                )
            )
        }
    }
}
