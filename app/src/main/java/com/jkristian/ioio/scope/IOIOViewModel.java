package com.jkristian.ioio.scope;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

public class IOIOViewModel extends ViewModel {

    private static final String TAG = "IOIOViewModel";

    List<SampleSet> samples = new ArrayList<>();
    private IOIOAndroidApplicationHelper helper;
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<String> toast = new MutableLiveData<>();

    public IOIOViewModel() {
        Log.v(TAG, "<init>");
        connectionStatus.setValue("connecting...");
        samples.add(new SampleSet());
        samples.add(new SampleSet());
    }

    void setContext(Context context) {
        Log.v(TAG, "setContext");
        if (helper == null) {
            helper = new IOIOAndroidApplicationHelper(
                    new ContextWrapper(context.getApplicationContext()),
                    new IOIOLooperProvider() {
                        @Override
                        public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
                            return new IOIOListener();
                        }
                    });
            helper.create();
            helper.start();
        }
    }

    void onNewIntent(Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            Log.v(TAG, "onNewIntent");
            helper.restart();
        }
    }

    @Override
    protected void onCleared() {
        Log.v(TAG, "onCleared");
        super.onCleared();
        helper.stop();
        helper.destroy();
    }

    LiveData<String> getConnectionStatus() {
        return connectionStatus;
    }

    LiveData<String> getToast() {
        return toast;
    }

    /**
     * Handle interaction with one IOIO board.
     * Each time the model is created, an instance is constructed for each board.
     * For each instance, incompatible() or setup() is called once; then
     * loop() is called repeatedly and finally disconnected() is called once
     * when the model is cleared.
     */
    private class IOIOListener extends BaseIOIOLooper {

        private boolean status;
        private DigitalOutput statusLED;
        private DigitalInput digital;
        private AnalogInput analog;

        public void incompatible() {
            Log.v(TAG, "incompatible");
            showStatus("IOIO firmware is incompatible", ioio_);
        }

        @Override
        protected void setup() throws ConnectionLostException {
            Log.v(TAG, "setup");
            showStatus("connected", ioio_);
            try {
                statusLED = ioio_.openDigitalOutput(0, true);
                digital = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP);
                analog = ioio_.openAnalogInput(46);
                startDaemon(new WatchDigitalInput(1, digital, samples.get(0)));
                startDaemon(new WatchAnalogInput(46, analog, samples.get(1)));
            } catch (ConnectionLostException e) {
                toast.postValue(e + "");
                throw e;
            }
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            Log.v(TAG, "loop");
            // Blink statusLED briefly, every 10 seconds:
            status = !status;
            if (statusLED != null) {
                statusLED.write(status);
            }
            Thread.sleep(status ? 9900 : 100);
        }

        @Override
        public void disconnected() {
            Log.v(TAG, "disconnected");
            showStatus("disconnected", null);
            if (analog != null) {
                analog.close();
                analog = null;
            }
            if (digital != null) {
                digital.close();
                digital = null;
            }
            if (statusLED != null) {
                try {
                    statusLED.write(false);
                } catch (ConnectionLostException ignored) {
                }
                statusLED.close();
                statusLED = null;
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
            connectionStatus.postValue(status.toString());
        }
    }

    private static void startDaemon(Runnable toDo) {
        Thread thread = new Thread(toDo);
        thread.setDaemon(true);
        thread.start();
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
                toast.postValue("input " + pin + ": " + e);
            }
        }

        protected abstract float nextSample()
                throws ConnectionLostException, InterruptedException;
    }

    private class WatchAnalogInput extends WatchInput {

        private final AnalogInput input;
        private float lastValue = Float.NaN;

        WatchAnalogInput(int pin, AnalogInput input, SampleSet samples) {
            super(pin, samples);
            if (input == null) {
                toast.postValue(pin + " AnalogInput is null");
            }
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
        private Boolean lastValue;

        WatchDigitalInput(int pin, DigitalInput input, SampleSet samples) {
            super(pin, samples);
            if (input == null) {
                toast.postValue(pin + " DigitalInput is null");
            }
            this.input = input;
        }

        @Override
        protected float nextSample()
                throws ConnectionLostException, InterruptedException {
            if (lastValue == null) {
                lastValue = input.read();
            } else {
                input.waitForValue(!lastValue);
                lastValue = !lastValue;
            }
            return (lastValue ? 1 : 0);
        }
    }
}
