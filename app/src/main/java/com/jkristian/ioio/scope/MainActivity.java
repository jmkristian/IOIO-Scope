package com.jkristian.ioio.scope;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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
    private static final long SEC = 1000000000L; // nsec

    private IOIOAndroidApplicationHelper helper;
    private Looper looper;
    private ImageView chart1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        helper = new IOIOAndroidApplicationHelper(this, new IOIOLooperProvider() {
            @Override
            public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
                return looper = new Looper();
            }
        });
        helper.create();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        enableUi(true);
        chart1 = findViewById(R.id.chart1);
        chart1.getViewTreeObserver().addOnGlobalLayoutListener(new LayoutChart1());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected void onDestroy() {
        helper.destroy();
        super.onDestroy();
    }

    protected void onStart() {
        super.onStart();
        helper.start();
    }

    protected void onStop() {
        helper.stop();
        super.onStop();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() & 268435456) != 0) {
            helper.restart();
        }
    }

    private class LayoutChart1 implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            Bitmap newBitmap = Bitmap.createBitmap(chart1.getWidth(), chart1.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawColor(Color.BLUE);
            chart1.setImageDrawable(new BitmapDrawable(getResources(), newBitmap));
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

    private int numConnected_ = 0;

    private void enableUi(final boolean enable) {
        // This is slightly trickier than expected to support a multi-IOIO use-case.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (enable) {
                    if (numConnected_++ == 0) {
                        // button_.setEnabled(true);
                    }
                } else {
                    if (--numConnected_ == 0) {
                        // button_.setEnabled(false);
                    }
                }
            }
        });
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

    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This is the thread on which all the IOIO activity happens. It will be run
     * every time the application is resumed and aborted when it is paused. The
     * method setup() will be called right after a connection with the IOIO has
     * been established (which might happen several times!). Then, loop() will
     * be called repetitively until the IOIO gets disconnected.
     */
    private class Looper extends BaseIOIOLooper {

        private DigitalOutput statusLED;
        private boolean status;

        public void incompatible() {
            showVersions(ioio_, "IOIO firmware is incompatible");
        }

        @Override
        protected void setup() throws ConnectionLostException {
            showVersions(ioio_, "IOIO connected");
            statusLED = ioio_.openDigitalOutput(0, true);
            enableUi(true);
            try {
                DigitalInput pin1 = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP);
                Thread watcher = new Thread(new WatchDigitalInput(pin1, 1));
                watcher.setDaemon(true);
                watcher.start();
            } catch (ConnectionLostException e) {
                toast(e + "");
            }
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            statusLED.write(status = !status);
            Thread.sleep(500);
        }

        @Override
        public void disconnected() {
            enableUi(false);
            toast("IOIO disconnected");
        }
    }

    private class WatchDigitalInput implements Runnable {

        private static final long DEBOUNCE = SEC / 10;
        private final DigitalInput input;
        private final int number;

        WatchDigitalInput(DigitalInput input, int number) {
            this.input = input;
            this.number = number;
        }

        @Override
        public void run() {
            int changes = 0;
            int bounces = 0;
            boolean lastValue = true;
            long lastChange = System.nanoTime() - DEBOUNCE - 1;
            try {
                while (true) {
                    input.waitForValue(!lastValue);
                    lastValue = !lastValue;
                    if (System.nanoTime() - lastChange < DEBOUNCE) {
                        ++bounces;
                    } else {
                        if (bounces > 0) {
                            Log.i(TAG, bounces + " bounces on input " + number);
                        }
                        bounces = 0;
                        lastChange = System.nanoTime();
                        Log.i(TAG, (++changes) + " changes on input " + number);
                    }
                }
            } catch (Exception e) {
                toast("pin " + number + ": " + e);
            }
        }
    }
}
