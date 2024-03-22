package com.example.onactivityresulttest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.onactivityresulttest.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val filePicker: FilePicker = FilePicker(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.activityButton.setOnClickListener {
            filePicker.launch(object: FilePickerResultCallback{
                override fun result(file: File) {

                }

            })
        }

    }
}