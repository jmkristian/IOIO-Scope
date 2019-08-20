package com.jkristian.android.ioio.scope

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jkristian.ioio.scope.SampleSet

private const val TAG = "IOIOViewModel"

class IOIOViewModel : ViewModel() {

    val samples = listOf(SampleSet(), SampleSet())
    val connectionStatus = MutableLiveData<String>()
}