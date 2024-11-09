package com.dicoding.asclepius.repository

import androidx.lifecycle.LiveData
import com.dicoding.asclepius.data.local.room.HistoryDao
import com.dicoding.asclepius.data.local.room.HistoryDatabase
import com.dicoding.asclepius.model.CancerHistory

class HistoryRepository private constructor(
    private val cancerHistoryDao: HistoryDao,
) {
    fun getAllCancerHistory(): LiveData<List<CancerHistory>> {
        return cancerHistoryDao.getAllCancerHistory()
    }

    suspend fun insertCancerHistory(cancerHistory: CancerHistory) {
        return cancerHistoryDao.insertCancerHistory(cancerHistory)
    }

    companion object {
        @Volatile
        private var instance: HistoryRepository? = null
        fun getInstance(
            cancerHistoryDao: HistoryDao,
        ): HistoryRepository = instance ?: synchronized(this) {
            instance ?: HistoryRepository(cancerHistoryDao).also {
                instance = it
            }
        }
    }
}