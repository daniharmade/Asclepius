package com.dicoding.asclepius.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.asclepius.model.CancerHistory
import com.dicoding.asclepius.repository.HistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel (private val cancerHistoryRepository: HistoryRepository) :
    ViewModel() {
    fun getAllCancerHistory() = cancerHistoryRepository.getAllCancerHistory()

    fun insertCancerHistory(cancerHistory: CancerHistory) {
        viewModelScope.launch {
            cancerHistoryRepository.insertCancerHistory(cancerHistory)
        }
    }
}