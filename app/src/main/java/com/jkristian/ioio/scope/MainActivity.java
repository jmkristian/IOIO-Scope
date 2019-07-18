package com.jkristian.ioio.scope;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private IOIOAndroidApplicationHelper ioio;
    private Handler toUiThread;
    private Timer background;
    private TextView connectionStatus;
    private List<ImageView> charts = new ArrayList<>();
    private List<SampleSet> samples = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toUiThread = new Handler();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ioio = new IOIOAndroidApplicationHelper(this, new IOIOLooperProvider() {
            @Override
            public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
                return new IOIOListener();
            }
        });
        ioio.create();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "TBD", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        connectionStatus = findViewById(R.id.connectionStatus);
        charts.add((ImageView) findViewById(R.id.chart1));
        charts.add((ImageView) findViewById(R.id.chart2));
        samples.add(new SampleSet());
        samples.add(new SampleSet());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected void onDestroy() {
        ioio.destroy();
        super.onDestroy();
    }

    protected void onStart() {
        super.onStart();
        ioio.start();
        background = new Timer();
        background.schedule(
                new DrawCharts(ContextCompat.getColor(this, R.color.chartForeground)),
                0, 250);
    }

    protected void onStop() {
        ioio.stop();
        super.onStop();
        background.cancel();
        for (SampleSet set : samples) {
            set.clear();
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() & 268435456) != 0) {
            ioio.restart();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle interaction with one IOIO board.
     * Each time the activity starts, an instance is constructed for each board.
     * For each instance, incompatible() or setup() is called once; then
     * loop() is called repeatedly and finally disconnected() is called once
     * when the activity stops.
     */
    private class IOIOListener extends BaseIOIOLooper {

        private boolean status;
        private DigitalOutput statusLED;

        public void incompatible() {
            showStatus("IOIO firmware is incompatible", ioio_);
        }

        @Override
        protected void setup() throws ConnectionLostException {
            showStatus("connected", ioio_);
            statusLED = ioio_.openDigitalOutput(0, true);
            startDaemon(new WatchDigitalInput(
                    1, ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP),
                    samples.get(0)));
            startDaemon(new WatchAnalogInput(
                    46, ioio_.openAnalogInput(46),
                    samples.get(1)));
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            // Blink statusLED briefly, every 10 seconds:
            statusLED.write(status = !status);
            Thread.sleep(status ? 9900 : 100);
        }

        @Override
        public void disconnected() {
            showStatus("disconnected", null);
            try {
                if (statusLED != null) {
                    statusLED.write(false);
                }
            } catch (ConnectionLostException ignored) {
            }
        }

        private void showStatus(String title, IOIO ioio) {
            final StringBuilder status = new StringBuilder(title);
            if (ioio != null) {
                status.append(String.format(
                        "\nIOIOLib: %s" +
                                "\nApplication firmware: %s" +
                                "\nBootloader firmware: %s" +
                                "\nHardware: %s",
                        ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                        ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                        ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                        ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
            }
            toUiThread.post(new Runnable() {
                @Override
                public void run() {
                    connectionStatus.setText(status.toString());
                }
            });
        }
    }

    private class DrawCharts extends TimerTask {

        @ColorInt
        private final int color;

        public DrawCharts(@ColorInt int color) {
            this.color = color;
        }

        @Override
        public void run() {
            toUiThread.post(new Runnable() {
                @Override
                public void run() {
                    // On the UI thread, look at the chart and sample sizes:
                    final Collection<LineSet> lines = newLineSets(charts);
                    final List<SampleSet> data = new ArrayList<>(samples);
                    try {
                        // On a background thread, convert the data to images:
                        background.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ShowCharts show = new ShowCharts(drawCharts(lines, data, color));
                                // On the UI thread, show the new images:
                                toUiThread.post(show);
                            }
                        }, 0);
                    } catch (IllegalStateException canceled) {
                    }
                }
            });
        }
    }

    private Collection<LineSet> newLineSets(Collection<ImageView> charts) {
        Collection<LineSet> into = new ArrayList<>(charts.size());
        for (View chart : charts) {
            into.add(new LineSet(chart));
        }
        return into;
    }

    private class ShowCharts implements Runnable {

        private final Collection<Bitmap> images;

        public ShowCharts(Collection<Bitmap> images) {
            this.images = images;
        }

        @Override
        public void run() {
            int c = 0;
            for (Bitmap image : images) {
                if (image != null) {
                    charts.get(c).setImageDrawable(new BitmapDrawable(getResources(), image));
                }
                ++c;
            }
        }
    }

    private static Collection<Bitmap> drawCharts(
            Collection<LineSet> lineSets, List<SampleSet> samples, @ColorInt int color) {
        Collection<Bitmap> images = new ArrayList<>(lineSets.size());
        int s = 0;
        for (LineSet lines : lineSets) {
            images.add(drawChart(lines, samples.get(s++), color));
        }
        return images;
    }

    private static Bitmap drawChart(LineSet lines, SampleSet samples, @ColorInt int color) {
        // Log.v(TAG, "draw into " + into);
        if (lines == null || lines.width <= 0 || lines.height <= 0 || samples.isEmpty()) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(lines.width, lines.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(LineSet.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        // Log.v(TAG, "draw lines " + deltas(liner.lines));
        canvas.drawLines(lines.toPoints(samples), paint);
        return bitmap;
    }

    private static Collection<Integer> deltas(Collection<Line> from) {
        Collection<Integer> into = new ArrayList<>(from.size() * 2);
        Line last = null;
        for (Line f : from) {
            if (last != null) {
                into.add(f.fromX - last.toX);
            }
            into.add(f.toX - f.fromX);
            last = f;
        }
        return into;
    }

    private abstract class WatchInput implements Runnable {

        private final int pin;
        private final SampleSet samples;

        WatchInput(int pin, SampleSet samples) {
            this.pin = pin;
            this.samples = samples;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    float value = nextSample();
                    samples.add(new Sample(System.nanoTime(), value));
                }
            } catch (ConnectionLostException ignored) {
            } catch (Exception e) {
                toast("input " + pin + ": " + e);
            }
        }

        protected abstract float nextSample()
                throws ConnectionLostException, InterruptedException;
    }

    private class WatchAnalogInput extends WatchInput {

        private final AnalogInput input;
        private float lastValue = Float.NaN;

        public WatchAnalogInput(int pin, AnalogInput input, SampleSet samples) {
            super(pin, samples);
            this.input = input;
        }

        @Override
        protected float nextSample()
                throws ConnectionLostException, InterruptedException {
            float value;
            while ((value = input.read()) == lastValue) {
                Thread.sleep(250);
            }
            return (lastValue = value);
        }
    }

    private class WatchDigitalInput extends WatchInput {

        private final DigitalInput input;
        private Boolean lastValue = null;

        public WatchDigitalInput(int pin, DigitalInput input, SampleSet samples) {
            super(pin, samples);
            this.input = input;
        }

        @Override
        protected float nextSample()
                throws ConnectionLostException, InterruptedException {
            if (lastValue == null) {
                lastValue = Boolean.valueOf(input.read());
            } else {
                input.waitForValue(!lastValue);
                lastValue = Boolean.valueOf(!lastValue);
            }
            return (lastValue ? 1 : 0);
        }
    }

    private void toast(final String message) {
        final Context context = this;
        toUiThread.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void startDaemon(Runnable toDo) {
        Thread thread = new Thread(toDo);
        thread.setDaemon(true);
        thread.start();
    }
}