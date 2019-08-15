package com.jkristian.ioio.scope

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import ioio.lib.api.AnalogInput
import ioio.lib.api.DigitalInput
import ioio.lib.api.DigitalOutput
import ioio.lib.api.IOIO
import ioio.lib.api.exception.ConnectionLostException
import ioio.lib.util.BaseIOIOLooper
import ioio.lib.util.IOIOLooperProvider
import ioio.lib.util.android.IOIOAndroidApplicationHelper
import java.util.*

private const val TAG = "IOIOViewModel"

class IOIOViewModel : ViewModel() {

    internal var samples: MutableList<SampleSet> = ArrayList()
    private var context: Context? = null
    private var helper: IOIOAndroidApplicationHelper? = null
    private val connectionStatus = MutableLiveData<String>()
    private val toast = MutableLiveData<String>()

    init {
        Log.v(TAG, "<init>")
        connectionStatus.value = "connecting..."
        samples.add(SampleSet())
        samples.add(SampleSet())
    }

    internal fun setContext(context: Context) {
        Log.v(TAG, "setContext")
        this.context = context
        if (helper == null) {
            helper = IOIOAndroidApplicationHelper(
                    ContextWrapper(context.applicationContext),
                    IOIOLooperProvider { connectionType, extra -> IOIOListener() })
            helper!!.create()
            helper!!.start()
        }
    }

    internal fun onNewIntent(intent: Intent) {
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            Log.v(TAG, "onNewIntent")
            helper!!.restart()
        }
    }

    override fun onCleared() {
        Log.v(TAG, "onCleared")
        super.onCleared()
        helper!!.stop()
        helper!!.destroy()
    }

    internal fun getConnectionStatus(): LiveData<String> {
        return connectionStatus
    }

    internal fun getToast(): LiveData<String> {
        return toast
    }

    /**
     * Handle interaction with one IOIO board.
     * Each time the model is created, an instance is constructed for each board.
     * For each instance, incompatible() or setup() is called once; then
     * loop() is called repeatedly and finally disconnected() is called once
     * when the model is cleared.
     */
    private inner class IOIOListener : BaseIOIOLooper() {

        private var status: Boolean = false
        private var statusLED: DigitalOutput? = null
        private var digital: DigitalInput? = null
        private var analog: AnalogInput? = null

        override fun incompatible() {
            Log.v(TAG, "incompatible")
            showStatus("IOIO firmware is incompatible", ioio_)
        }

        @Throws(ConnectionLostException::class)
        override fun setup() {
            Log.v(TAG, "setup")
            showStatus("connected", ioio_)
            try {
                statusLED = ioio_.openDigitalOutput(0, true)
                digital = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP)
                analog = ioio_.openAnalogInput(46)
                startDaemon(WatchDigitalInput(1, digital!!, samples[0]))
                startDaemon(WatchAnalogInput(46, analog!!, samples[1]))
            } catch (e: ConnectionLostException) {
                toast.postValue(e.toString() + "")
                throw e
            }

        }

        @Throws(ConnectionLostException::class, InterruptedException::class)
        override fun loop() {
            Log.v(TAG, "loop")
            // Blink statusLED briefly, every 10 seconds:
            status = !status
            if (statusLED != null) {
                statusLED!!.write(status)
            }
            Thread.sleep((if (status) 9900 else 100).toLong())
        }

        override fun disconnected() {
            Log.v(TAG, "disconnected")
            showStatus("disconnected", null)
            if (analog != null) {
                analog!!.close()
                analog = null
            }
            if (digital != null) {
                digital!!.close()
                digital = null
            }
            if (statusLED != null) {
                try {
                    statusLED!!.write(false)
                } catch (ignored: ConnectionLostException) {
                }
                statusLED!!.close()
                statusLED = null
            }
        }

        private fun showStatus(title: String, ioio: IOIO?) {
            val status = StringBuilder(title)
            if (ioio != null) {
                status.append(String.format(
                        "\nIOIOLib: %s" +
                                "\nApplication firmware: %s" +
                                "\nBootloader firmware: %s" +
                                "\nHardware: %s",
                        ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                        ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                        ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                        ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)))
            }
            connectionStatus.postValue(status.toString())
        }
    }

    private abstract inner class WatchInput internal constructor(val pin: Int, val samples: SampleSet) : Runnable {

        override fun run() {
            try {
                while (true) {
                    val value = nextSample()
                    samples.add(Sample(System.nanoTime(), value))
                }
            } catch (ignored: ConnectionLostException) {
            } catch (e: Exception) {
                toast.postValue("input $pin: $e")
            }

        }

        @Throws(ConnectionLostException::class, InterruptedException::class)
        protected abstract fun nextSample(): Float
    }

    private inner class WatchAnalogInput internal constructor(pin: Int, private val input: AnalogInput, samples: SampleSet) : WatchInput(pin, samples) {

        private var lastValue = java.lang.Float.NaN

        init {
            if (input == null) {
                toast.postValue("$pin AnalogInput is null")
            }
        }

        @Throws(ConnectionLostException::class, InterruptedException::class)
        override fun nextSample(): Float {
            var value: Float
            while (true) {
                value = input.read()
                if (value != lastValue) {
                    break
                }
                Thread.sleep(250)
            }
            lastValue = value
            return value
        }
    }

    private inner class WatchDigitalInput
    internal constructor(pin: Int, private val input: DigitalInput, samples: SampleSet)
        : WatchInput(pin, samples) {

        private var hasValue: Boolean = false
        private var lastValue: Boolean = false

        @Throws(ConnectionLostException::class, InterruptedException::class)
        override fun nextSample(): Float {
            if (!hasValue) {
                hasValue = true
                lastValue = input.read()
            } else {
                input.waitForValue(!lastValue)
                lastValue = !lastValue
                if (lastValue) {
                    val last2 = samples.getLast(2)
                    if (last2.size >= 2) {
                        val elapsed = (System.nanoTime() - last2[1].time).toDouble() / LineSet.SEC
                        val message = String.format("input %d was false for %.3f sec", pin, elapsed)
                        Log.i(TAG, message)
                        try {
                            val destination = PreferenceManager
                                    .getDefaultSharedPreferences(context)
                                    ?.getString("SMS_to", "")
                            SmsManager.getDefault().sendTextMessage(
                                    destination, null,
                                    message, null, null)
                        } catch (e: Exception) {
                            toast.postValue("" + e)
                            Log.w(TAG, e)
                        }
                    }
                }
            }
            return (if (lastValue) 1.0f else 0.0f)
        }
    }

    companion object {
        private fun startDaemon(toDo: Runnable) {
            val thread = Thread(toDo)
            thread.isDaemon = true
            thread.start()
        }
    }
}
