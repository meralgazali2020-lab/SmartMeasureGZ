package com.smartmeasure.gz

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartmeasure.gz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.newProjectBtn.setOnClickListener {
            Toast.makeText(
                this,
                "مشروع جديد",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.savedProjectsBtn.setOnClickListener {
            Toast.makeText(
                this,
                "المشاريع المحفوظة",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.scanMeasurementsBtn.setOnClickListener {
            Toast.makeText(
                this,
                "تصوير ورقة المقاسات",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.calculatorBtn.setOnClickListener {
            Toast.makeText(
                this,
                "حاسبة المقاسات",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.notesBtn.setOnClickListener {
            Toast.makeText(
                this,
                "الملاحظات",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
