package com.hanto.dragndrop.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hanto.dragndrop.R
import com.hanto.dragndrop.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_main, HomeFragment())
                .commit()
        }
    }
}
