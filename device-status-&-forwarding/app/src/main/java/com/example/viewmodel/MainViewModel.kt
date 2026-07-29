package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseClientManager
import com.example.data.models.DeviceStatus
import com.example.data.models.ForwardingCommand
import com.example.data.preferences.NotificationPreferences
import com.example.data.repository.DeviceRepository
import com.example.monitoring.DeviceMonitoringService
import com.example.monitoring.DeviceMonitorWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)
    val preferences = NotificationPreferences(application)

    val devices: StateFlow<List<DeviceStatus>> = repository.devices
    val commands: StateFlow<List<ForwardingCommand>> = repository.commands
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error

    private val _isPasscodeAuthenticated = MutableStateFlow(false)
    val isPasscodeAuthenticated: StateFlow<Boolean> = _isPasscodeAuthenticated.asStateFlow()

    private val _passcodeError = MutableStateFlow<String?>(null)
    val passcodeError: StateFlow<String?> = _passcodeError.asStateFlow()

    private val _isSubmittingCommand = MutableStateFlow(false)
    val isSubmittingCommand: StateFlow<Boolean> = _isSubmittingCommand.asStateFlow()

    init {
        // Start background service and schedule periodic worker
        try {
            DeviceMonitoringService.start(application)
            DeviceMonitorWorker.schedule(application)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun authenticatePasscode(inputPin: String): Boolean {
        val expected = SupabaseClientManager.forwardingPasscode
        if (inputPin.trim() == expected.trim()) {
            _isPasscodeAuthenticated.value = true
            _passcodeError.value = null
            return true
        } else {
            _passcodeError.value = "Incorrect passcode. Access denied."
            return false
        }
    }

    fun submitCommand(
        targetNumber: String,
        deviceId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmittingCommand.value = true
            val result = repository.sendForwardingCommand(targetNumber, deviceId)
            _isSubmittingCommand.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to insert command")
            }
        }
    }

    fun refreshData() {
        repository.refreshAllData()
    }
}
