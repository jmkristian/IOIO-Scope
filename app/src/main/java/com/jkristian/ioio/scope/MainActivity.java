package com.jkristian.ioio.scope;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private IOIOViewModel model;
    private Handler toUiThread;
    private Timer background;
    private TextView connectionStatus;
    private List<ImageView> charts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        model = ViewModelProviders.of(this).get(IOIOViewModel.class);
        model.setContext(this);
        toUiThread = new Handler();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        connectionStatus = findViewById(R.id.connectionStatus);
        connectionStatus.setText(model.getConnectionStatus().getValue());
        model.getConnectionStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String status) {
                connectionStatus.setText(status);
            }
        });
        model.getToast().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                toast(message);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
        background = new Timer();
        background.schedule(
                new DrawCharts(ContextCompat.getColor(this, R.color.chartForeground)),
                0, 250);
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
        background.cancel();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (model != null) {
            model.onNewIntent(intent);
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

    private class DrawCharts extends TimerTask {

        @ColorInt
        private final int color;

        DrawCharts(@ColorInt int color) {
            this.color = color;
        }

        @Override
        public void run() {
            toUiThread.post(new Runnable() {
                @Override
                public void run() {
                    // On the UI thread, look at the chart and sample sizes:
                    final Collection<LineSet> lines = newLineSets(charts);
                    final List<SampleSet> data = new ArrayList<>(model.samples);
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

        ShowCharts(Collection<Bitmap> images) {
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

    private void toast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}