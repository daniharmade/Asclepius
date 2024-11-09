package com.dicoding.asclepius.view

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.asclepius.R
import com.dicoding.asclepius.adapter.HistoryAdapter
import com.dicoding.asclepius.databinding.ActivityHistoryBinding
import com.dicoding.asclepius.viewmodel.HistoryViewModel
import com.dicoding.asclepius.viewmodel.HistoryViewModelFactory

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cancerHistoryViewModelFactory = HistoryViewModelFactory.getInstance(this)
        val HistoryViewModel: HistoryViewModel by viewModels {
            cancerHistoryViewModelFactory
        }

        setupAppBar()

        HistoryViewModel.getAllCancerHistory().observe(this) {
            if (it != null) {
                binding.rvNews.layoutManager = LinearLayoutManager(this)
                val adapter = HistoryAdapter(it, this)
                binding.rvNews.adapter = adapter
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> false
        }
    }

    private fun setupAppBar() {
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_ios_new_24)
        supportActionBar?.elevation = 0f
    }
}