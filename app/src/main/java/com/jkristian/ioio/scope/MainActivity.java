package com.jkristian.ioio.scope;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
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
    private List<ImageView> charts = new ArrayList<>();
    private List<Deque<Sample>> samples = new ArrayList<>();

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
        charts.add((ImageView) findViewById(R.id.chart1));
        charts.add((ImageView) findViewById(R.id.chart2));
        samples.add(new ArrayDeque<Sample>());
        samples.add(new ArrayDeque<Sample>());
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
        background.schedule(new DrawCharts(), 1000, 1000);
    }

    protected void onStop() {
        ioio.stop();
        super.onStop();
        background.cancel();
        for (Deque<Sample> sample : samples) {
            sample.clear();
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
            showVersions(ioio_, "IOIO firmware is incompatible");
        }

        @Override
        protected void setup() throws ConnectionLostException {
            showVersions(ioio_, "IOIO connected");
            statusLED = ioio_.openDigitalOutput(0, true);

            DigitalInput input1 = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP);
            Thread watcher = new Thread(new WatchDigitalInput(1, input1, samples.get(0)));
            watcher.setDaemon(true);
            watcher.start();

            AnalogInput input46 = ioio_.openAnalogInput(46);
            watcher = new Thread(new WatchAnalogInput(46, input46, samples.get(1)));
            watcher.setDaemon(true);
            watcher.start();
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            // Blink statusLED briefly, every 10 seconds:
            statusLED.write(status = !status);
            Thread.sleep(status ? 9900 : 100);
        }

        @Override
        public void disconnected() {
            try {
                statusLED.write(false);
            } catch (ConnectionLostException ignored) {
            }
        }

        private void showVersions(IOIO ioio, String title) {
            toast(String.format("%s\n" +
                            "IOIOLib: %s\n" +
                            "Application firmware: %s\n" +
                            "Bootloader firmware: %s\n" +
                            "Hardware: %s",
                    title,
                    ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                    ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                    ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                    ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
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

    private class DrawCharts extends TimerTask {
        @Override
        public void run() {
            toUiThread.post(new Runnable() {
                @Override
                public void run() {
                    // On the UI thread, look at the views and copy the samples:
                    final Collection<LineSet> lineSets = newLineSets();
                    try {
                        background.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // On a background thread, construct the Drawables:
                                ShowCharts show = new ShowCharts(drawCharts(lineSets));
                                // On the UI thread, show the new charts:
                                toUiThread.post(show);
                            }
                        }, 0);
                    } catch (IllegalStateException canceled) {
                    }
                }
            });
        }
    }

    private Collection<LineSet> newLineSets() {
        Collection<LineSet> into = new ArrayList<>(charts.size());
        int s = 0;
        for (View chart : charts) {
            into.add(new LineSet(chart, samples.get(s++)));
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

    private Collection<Bitmap> drawCharts(Collection<LineSet> lineSets) {
        Collection<Bitmap> into = new ArrayList<>(lineSets.size());
        for (LineSet set : lineSets) {
            into.add(drawChart(set));
        }
        return into;
    }

    private Bitmap drawChart(LineSet lines) {
        // Log.v(TAG, "draw into " + into);
        if (lines == null || lines.isEmpty() || lines.width <= 0 || lines.height <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(lines.width, lines.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(LineSet.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        // Log.v(TAG, "draw lines " + deltas(liner.lines));
        canvas.drawLines(lines.toPoints(), paint);
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

        protected final int pin;
        protected final Collection<Sample> samples;

        WatchInput(int pin, Collection<Sample> samples) {
            this.pin = pin;
            this.samples = samples;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    float value = nextSample();
                    toUiThread.post(new AddSample(samples, new Sample(System.nanoTime(), value)));
                }
            } catch (ConnectionLostException ignored) {
            } catch (Exception e) {
                Log.i(TAG, "pin " + pin + ": " + e);
            }
        }

        protected abstract float nextSample()
                throws ConnectionLostException, InterruptedException;
    }

    private class WatchAnalogInput extends WatchInput {

        private final AnalogInput input;
        private float lastValue = Float.NaN;

        public WatchAnalogInput(int pin, AnalogInput input, Collection<Sample> samples) {
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

        public WatchDigitalInput(int pin, DigitalInput input, Collection<Sample> samples) {
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
}