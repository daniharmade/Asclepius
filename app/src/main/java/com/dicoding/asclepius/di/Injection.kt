package com.dicoding.asclepius.di

import android.content.Context
import com.dicoding.asclepius.data.local.room.HistoryDatabase
import com.dicoding.asclepius.repository.HistoryRepository

object Injection {
    fun provideCancerHistoryRepository(context: Context): HistoryRepository {
        val database = HistoryDatabase.getInstance(context)
        val dao = database.cancerHistoryDao()
        return HistoryRepository.getInstance(dao)
    }
}