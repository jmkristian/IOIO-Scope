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

    enum class State {
        DEAD, STOPPED, PAUSED, ACTIVE
    }

    private var state = State.DEAD

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        Log.v(TAG, member("onCreate"))
        state = State.STOPPED
        try {
            helper.create()
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(member(helper, "create ") + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.v(TAG, member("onDestroy"))
        state = State.DEAD
        try {
            helper.destroy()
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(member(helper, "destroy ") + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.v(TAG, member("onStart"))
        state = State.PAUSED
        try {
            helper.start()
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(member(helper, "start ") + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.v(TAG, member("onStop"))
        state = State.STOPPED
        try {
            helper.stop();
        } catch (e: Exception) {
            Log.w(TAG, e);
            warning?.postValue(member(helper, "stop ") + e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Log.v(TAG, member("onResume"))
        state = State.ACTIVE
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.v(TAG, member("onPause"))
        state = State.PAUSED
    }

    private fun member(thing: Any, name: String) = "" + thing.hashCode() + ".$name"

    private fun member(name: String) = member(this, name)
}