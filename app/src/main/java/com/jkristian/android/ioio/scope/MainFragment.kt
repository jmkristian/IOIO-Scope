package com.jkristian.android.ioio.scope

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jkristian.ioio.scope.*
import com.jkristian.ioio.scope.Line
import com.jkristian.ioio.scope.LineSet
import com.jkristian.ioio.scope.SampleSet
import java.util.*

private const val TAG = "MainFragment"
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
// private const val ARG_PARAM1 = "param1"

class MainFragment : Fragment() {
    // TODO: Rename and change types of parameters
    // private var param1: String? = null

    private var background: Timer? = null
    private var toUiThread: Handler? = null
    private var model: IOIOViewModel? = null
    private val charts = ArrayList<ImageView>()
    private var connectionStatus: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
/*
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
*/
        toUiThread = Handler()
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)
        background = Timer()
        background!!.schedule(
                DrawCharts(ContextCompat.getColor(context!!, R.color.chartForeground)),
                0, 250)
    }

    override fun onDetach() {
        Log.v(TAG, "onDetach")
        super.onDetach()
        background!!.cancel()
        background = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.v(TAG, "onCreateView")
        var layout = inflater.inflate(R.layout.fragment_main, container, false)
        charts.clear()
        charts.add(layout.findViewById<View>(R.id.chart1) as ImageView)
        charts.add(layout.findViewById<View>(R.id.chart46) as ImageView)
        model = ViewModelProviders.of(activity!!).get(IOIOViewModel::class.java)
        connectionStatus = layout.findViewById(R.id.connectionStatus)
        connectionStatus!!.text = model!!.getConnectionStatus().value
        model!!.getConnectionStatus().observe(this, Observer { status -> connectionStatus!!.text = status })
        model!!.getToast().observe(this, Observer { message -> toast(message) })
        return layout
    }

    private inner class DrawCharts internal
    constructor(@param:ColorInt @field:ColorInt private val color: Int) : TimerTask() {

        override fun run() {
            toUiThread!!.post {
                // On the UI thread, look at the chart and sample sizes:
                val lines = newLineSets(charts)
                val data = ArrayList(model!!.samples)
                try {
                    // On a background thread, convert the data to images:
                    background!!.schedule(object : TimerTask() {
                        override fun run() {
                            val show = ShowCharts(MainFragment.drawCharts(lines, data, color))
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
        Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
                MainFragment().apply {
                    arguments = Bundle().apply {
                        // putString(ARG_PARAM1, param1)
                    }
                }

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
