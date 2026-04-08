package com.axis.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.axis.app.data.repository.PesaRepository
import com.axis.app.datastore.UserSettings
import com.axis.app.domain.budget.BudgetEngine

class ViewModelFactory(
    private val repository: PesaRepository,
    private val application: Application,
    private val userSettings: UserSettings
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                repository,
                BudgetEngine(),
                application,
                userSettings
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
