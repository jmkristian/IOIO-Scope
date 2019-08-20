package com.jkristian.android.ioio.scope

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.jkristian.ioio.scope.Chart
import com.jkristian.ioio.scope.R
import com.jkristian.ioio.scope.Sample
import com.jkristian.ioio.scope.SampleSet
import ioio.lib.api.AnalogInput
import ioio.lib.api.DigitalInput
import ioio.lib.api.DigitalOutput
import ioio.lib.api.IOIO
import ioio.lib.api.exception.ConnectionLostException
import ioio.lib.util.BaseIOIOLooper
import ioio.lib.util.IOIOLooper
import ioio.lib.util.IOIOLooperProvider
import ioio.lib.util.android.IOIOAndroidApplicationHelper

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), IOIOLooperProvider {

    private val helper = IOIOAndroidApplicationHelper(this, this)
    private var model: IOIOViewModel? = null
    private var toUiThread: Handler? = null

    init {
        Log.v(TAG, "<init>")
        showStatus("", null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        helper.create()
        setContentView(R.layout.activity_main)
        toUiThread = Handler()
        model = ViewModelProviders.of(this).get(IOIOViewModel::class.java)

        val actionBar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(actionBar)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        actionBar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val item = menu.findItem(R.id.settingsFragment)
            item?.setVisible(destination.id != R.id.settingsFragment)
        }
        return true
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        helper.destroy()
        super.onDestroy()
    }

    override fun onStart() {
        Log.v(TAG, "onStart")
        super.onStart()
        helper.start()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        helper.stop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        Log.i(TAG, "onRequestPermissionsResult("
                + requestCode
                + ", " + permissions.contentToString()
                + ", " + results + ")")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.flags and 268435456 != 0) {
            helper.restart()
        }
    }

    override fun createIOIOLooper(connectionType: String?, extra: Any?): IOIOLooper {
        Log.v(TAG, "createIOIOLooper("
                + connectionType + ", "
                + extra + ")")
        return IOIOListener()
    }

    /**
     * Handle interaction with one IOIO board.
     * Each time helper is started, an instance is constructed for each board.
     * For each instance, incompatible() or setup() is called once; then
     * loop() is called repeatedly and finally disconnected() is called once.
     */
    private inner class IOIOListener : BaseIOIOLooper() {

        private var status: Boolean = false
        private var statusLED: DigitalOutput? = null
        private var digital: DigitalInput? = null
        private var analog: AnalogInput? = null

        override fun incompatible() {
            showStatus("IOIO firmware is incompatible", ioio_)
        }

        @Throws(ConnectionLostException::class)
        override fun setup() {
            showStatus("connected", ioio_)
            try {
                statusLED = ioio_.openDigitalOutput(IOIO.LED_PIN, true)
                analog = ioio_.openAnalogInput(46)
/*
                digital = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP)
                startDaemon(WatchDigitalInput(1, digital!!, model!!.samples[0]))
                startDaemon(WatchAnalogInput(46, analog!!, samples[1]))
*/
            } catch (e: ConnectionLostException) {
                warn("" + e)
                throw e
            }

        }

        @Throws(ConnectionLostException::class, InterruptedException::class)
        override fun loop() {
            analog?.read();
            // Blink statusLED briefly, every 1 seconds:
            status = !status
            statusLED?.write(status)
            if (status) {
                Log.v(TAG, "blink")
            }
            Thread.sleep(if (status) 900L else 100L)
        }

        override fun disconnected() {
            showStatus("disconnected", null)
/*
            analog?.close()
            analog = null
            digital?.close()
            digital = null
            try {
                statusLED?.write(false)
            } catch (e: ConnectionLostException) {
                // ignored
            }
            statusLED?.close()
            statusLED = null
*/
        }
    }

    private abstract inner class WatchInput
    internal constructor(val pin: Int, val samples: SampleSet)
        : Runnable {

        override fun run() {
            try {
                while (true) {
                    val value = nextSample()
                    samples.add(Sample(System.nanoTime(), value))
                }
            } catch (e: ConnectionLostException) { // ignored
                Log.v(TAG, "input $pin: $e")
            } catch (e: InterruptedException) { // ignored
                Log.v(TAG, "input $pin: $e")
            } catch (e: IllegalStateException) { // maybe trying to use a closed resouce
                Log.w(TAG, e)
                if (!"Trying to use a closed resouce".equals(e.message)) {
                    warn("input $pin: $e")
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                warn("input $pin: $e")
            }
        }

        @Throws(ConnectionLostException::class, InterruptedException::class)
        protected abstract fun nextSample(): Float
    }

    private inner class WatchAnalogInput
    internal constructor(pin: Int, private val input: AnalogInput, samples: SampleSet)
        : WatchInput(pin, samples) {

        private var lastValue = java.lang.Float.NaN

        init {
            if (input == null) {
                warn("$pin AnalogInput is null")
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
                        val elapsed = (System.nanoTime() - last2[1].time).toDouble() / Chart.SEC
                        val message = String.format("input %d was false for %.3f sec", pin, elapsed)
                        Log.i(TAG, message)
                        try {
                            val destination = PreferenceManager
                                    .getDefaultSharedPreferences(this@MainActivity)
                                    ?.getString(SettingsFragment.SMS_TO, "")
                            if (destination != null && destination.isNotEmpty()) {
                                SmsManager.getDefault().sendTextMessage(
                                        destination, null,
                                        message, null, null)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                            warn(TAG + " send SMS " + e)
                        }
                    }
                }
            }
            return (if (lastValue) 1.0f else 0.0f)
        }
    }

    private fun showStatus(title: String, ioio: IOIO?) {
        Log.v(TAG, "showStatus " + title)
        val status = StringBuilder(title)
        if (ioio != null) {
            var versions: String = ""
            try {
                versions = String.format(
                        "\nIOIOLib: %s" +
                                "\nApplication firmware: %s" +
                                "\nBootloader firmware: %s" +
                                "\nHardware: %s",
                        ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                        ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                        ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                        ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER))
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
            status.append(versions)
        }
        model?.connectionStatus?.postValue(status.toString())
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private inner class Warn(val message: String) : Runnable {
        override fun run() {
            toast(message);
        }
    }

    private fun warn(message: String) {
        toUiThread?.post(Warn(message))
    }

    companion object {
        private fun startDaemon(toDo: Runnable) {
            val thread = Thread(toDo)
            thread.isDaemon = true
            thread.start()
        }
    }
}