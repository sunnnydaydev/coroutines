package com.example.coroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Create by SunnyDay /12/28 21:36:44
 */
class MainViewModel : ViewModel() {

    private val _event: MutableSharedFlow<Int> = MutableSharedFlow(1)

    val event: SharedFlow<Int> = _event

    init {
        viewModelScope.launch {
            _event.emit(1)
            _event.emit(2)
            _event.emit(3)
        }
    }
}