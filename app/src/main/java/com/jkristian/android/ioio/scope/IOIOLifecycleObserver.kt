package com.jkristian.android.ioio.scope

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import ioio.lib.util.android.IOIOAndroidApplicationHelper

private const val TAG = "IOIOLifecycleObserver"

class IOIOLifecycleObserver(
        private val helper: IOIOAndroidApplicationHelper,
        private val warning: MutableLiveData<String>?)
    : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        Log.v(TAG, "create")
        try {
            helper.create()
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(TAG + ".create " + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        Log.v(TAG, "destroy")
        try {
            helper.destroy()
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(TAG + ".destroy " + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        Log.v(TAG, "start")
        try {
            helper.start()
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(TAG + ".start " + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        Log.v(TAG, "stop")
        try {
            helper.stop();
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(TAG + ".stop " + e);
        }
    }
}