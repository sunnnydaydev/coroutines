package com.example.coroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Create by SunnyDay /12/28 21:36:44
 */
class MainViewModel : ViewModel() {

    private var count = 0

    private val _uiState: MutableStateFlow<Int> = MutableStateFlow(0)

    val uiState: StateFlow<Int> = _uiState

    init {
        viewModelScope.launch {
            while (count <= 100) {
                _uiState.value = count
                count += 2
            }
        }
    }

    fun updateValue() {
        viewModelScope.launch {
            for (i in 0..3) {
                _uiState.value = _uiState.value + i
            }
        }
    }
}