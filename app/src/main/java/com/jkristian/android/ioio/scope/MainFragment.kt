package com.jkristian.android.ioio.scope

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jkristian.ioio.scope.Chart
import com.jkristian.ioio.scope.R
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
    private var charts: List<Chart> = Collections.emptyList()

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
        connectionStatus!!.text = model!!.getConnectionStatus().value
        model!!.getConnectionStatus().observe(this, Observer { status ->
            connectionStatus!!.text = status
        })
        model!!.getToast().observe(this, Observer { message ->
            toast(message)
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

    private fun toast(message: String) {
        Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
                MainFragment().apply {
                    arguments = Bundle().apply {
                        // putString(ARG_PARAM1, param1)
                    }
                }

    }
}