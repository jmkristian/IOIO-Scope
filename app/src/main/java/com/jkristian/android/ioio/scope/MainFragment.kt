package com.jkristian.android.ioio.scope

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jkristian.ioio.scope.Chart
import com.jkristian.ioio.scope.R
import java.util.*

private const val TAG = "MainFragment"

class MainFragment : Fragment() {

    private var model: IOIOViewModel? = null
    private var toUiThread: Handler? = null
    private var background: Timer? = null
    private var charts: List<Chart> = Collections.emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        toUiThread = Handler()
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.v(TAG, "onCreateView")
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(layout: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(layout, savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(IOIOViewModel::class.java)
        val connectionStatus: TextView = layout.findViewById(R.id.connectionStatus)
        model!!.connectionStatus.observe(this, Observer { status ->
            connectionStatus!!.text = status
        })
        layout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            var done = false
            override fun onGlobalLayout() {
                // layout.viewTreeObserver.removeOnGlobalLayoutListener(this) requires API 16
                if (!done) {
                    charts = listOf(
                            Chart(layout.findViewById(R.id.chart1)),
                            Chart(layout.findViewById(R.id.chart46)))
                    done = true
                }
            }
        })
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        charts = Collections.emptyList()
    }

    override fun onStart() {
        Log.v(TAG, "onStart")
        super.onStart()
        background = Timer()
        background?.schedule(
                DrawCharts(ContextCompat.getColor(context!!, R.color.chartForeground)),
                0, 250)
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        super.onStop()
        background?.cancel()
        background = null
    }

    private inner class DrawCharts(@param:ColorInt @field:ColorInt private val color: Int)
        : TimerTask() {

        val samples = model!!.samples

        override fun run() {
            var s = 0
            for (chart in charts) {
                chart.setSamples(samples[s++])
            }
            for (chart in charts) {
                chart.draw(color)
            }
            // On the UI thread, show the new images:
            toUiThread?.post {
                for (chart in charts) {
                    chart.show()
                }
            }
        }
    }
}