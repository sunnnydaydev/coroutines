package com.example.coroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Create by SunnyDay /12/28 21:36:44
 */
class MainViewModel:ViewModel() {
    fun login() = viewModelScope.launch {
        delay(1000)
    }
}