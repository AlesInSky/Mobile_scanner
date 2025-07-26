package com.example.diakontmobilescanner.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScanViewModel : ViewModel() {
    private val _history = MutableLiveData<MutableList<String>>(mutableListOf())
    val history: MutableLiveData<MutableList<String>> get() = _history

    fun addBarcode(code: String) {
        if (!_history.value!!.contains(code)) {
            _history.value = (_history.value!! + code).toMutableList()
        }
    }
}