package com.personal.moneytracker.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.moneytracker.data.repository.TrackedPackageRepository
import com.personal.moneytracker.updater.AppUpdater
import com.personal.moneytracker.updater.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val trackedPackages: TrackedPackageRepository,
    private val appUpdater: AppUpdater
) : ViewModel() {
    
    val packages = trackedPackages.packages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    fun addPackage(value: String) { viewModelScope.launch { trackedPackages.add(value) } }
    fun removePackage(value: String) { viewModelScope.launch { trackedPackages.remove(value) } }
    
    fun checkForUpdate(context: Context, jsonUrl: String) {
        viewModelScope.launch {
            val info = appUpdater.checkForUpdate(jsonUrl, context)
            if (info != null) {
                _updateInfo.value = info
            }
        }
    }
    
    fun downloadUpdate(context: Context, title: String) {
        val info = _updateInfo.value ?: return
        appUpdater.downloadAndInstall(context, info.apkUrl)
        _updateInfo.value = null // Dismiss dialog after starting download
    }
    
    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
