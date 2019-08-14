package com.jkristian.ioio.scope

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity() {

    private var model: IOIOViewModel? = null
    private var toUiThread: Handler? = null
    private var background: Timer? = null
    private var connectionStatus: TextView? = null
    private val charts = ArrayList<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(this).get(IOIOViewModel::class.java!!)
        model!!.setContext(this)
        toUiThread = Handler()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "TBD", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        charts.add(findViewById<View>(R.id.chart1) as ImageView)
        charts.add(findViewById<View>(R.id.chart2) as ImageView)
        connectionStatus = findViewById(R.id.connectionStatus)
        connectionStatus!!.text = model!!.getConnectionStatus().value
        model!!.getConnectionStatus().observe(this, Observer { status -> connectionStatus!!.text = status })
        model!!.getToast().observe(this, Observer { message -> toast(message) })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onStart() {
        Log.v(TAG, "onStart")
        super.onStart()
        background = Timer()
        background!!.schedule(
                DrawCharts(ContextCompat.getColor(this, R.color.chartForeground)),
                0, 250)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        super.onStop()
        background!!.cancel()
        background = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (model != null) {
            model!!.onNewIntent(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        Log.i(TAG, "onRequestPermissionsResult("
                + requestCode
                + ", " + permissions.contentToString()
                + ", " + results + ")")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    private inner class DrawCharts internal constructor(@param:ColorInt @field:ColorInt
                                                        private val color: Int) : TimerTask() {

        override fun run() {
            toUiThread!!.post {
                // On the UI thread, look at the chart and sample sizes:
                val lines = newLineSets(charts)
                val data = ArrayList(model!!.samples)
                try {
                    // On a background thread, convert the data to images:
                    background!!.schedule(object : TimerTask() {
                        override fun run() {
                            val show = ShowCharts(drawCharts(lines, data, color))
                            // On the UI thread, show the new images:
                            toUiThread!!.post(show)
                        }
                    }, 0)
                } catch (canceled: IllegalStateException) {
                }
            }
        }
    }

    private fun newLineSets(charts: Collection<ImageView>): Collection<LineSet> {
        val into = ArrayList<LineSet>(charts.size)
        for (chart in charts) {
            into.add(LineSet(chart))
        }
        return into
    }

    private inner class ShowCharts internal constructor(private val images: Collection<Bitmap?>) : Runnable {

        override fun run() {
            var c = 0
            for (image in images) {
                if (image != null) {
                    charts[c].setImageDrawable(BitmapDrawable(resources, image))
                }
                ++c
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {

        private val TAG = "MainActivity"

        private fun drawCharts(
                lineSets: Collection<LineSet>, samples: List<SampleSet>, @ColorInt color: Int): Collection<Bitmap?> {
            val images = ArrayList<Bitmap?>(lineSets.size)
            var s = 0
            for (lines in lineSets) {
                images.add(drawChart(lines, samples[s++], color))
            }
            return images
        }

        private fun drawChart(lines: LineSet?, samples: SampleSet, @ColorInt color: Int): Bitmap? {
            // Log.v(TAG, "draw into " + into);
            if (lines == null || lines.width <= 0 || lines.height <= 0 || samples.isEmpty) {
                return null
            }
            val bitmap = Bitmap.createBitmap(lines.width, lines.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.color = color
            paint.strokeWidth = LineSet.STROKE.toFloat()
            paint.strokeCap = Paint.Cap.BUTT
            // Log.v(TAG, "draw lines " + deltas(liner.lines));
            canvas.drawLines(lines.toPoints(samples), paint)
            return bitmap
        }

        private fun deltas(from: Collection<Line>): Collection<Int> {
            val into = ArrayList<Int>(from.size * 2)
            var last: Line? = null
            for (f in from) {
                if (last != null) {
                    into.add(f.fromX - last.toX)
                }
                into.add(f.toX - f.fromX)
                last = f
            }
            return into
        }
    }
}