package com.jkristian.ioio.scope;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.Fragment;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

public class IOIOFragment extends Fragment {

    static final String TAG = "IOIOFragment";

    private IOIOAndroidApplicationHelper helper;
    private IOIOLooper listener;
    private IOIO ioio;

    public IOIOFragment() {
        Log.v(TAG, "<constructed>");
    }

    @Override
    public void onAttach(Context to) {
        super.onAttach(to);
        setRetainInstance(true);
        if (helper != null) {
            Log.v(TAG, "onAttach retained");
        } else {
            Log.v(TAG, "onAttach");
            helper = new IOIOAndroidApplicationHelper(
                    new ContextWrapper(to.getApplicationContext()),
                    new IOIOLooperProvider() {
                        @Override
                        public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
                            return new Looper();
                        }
                    });
            helper.create();
            helper.start();
        }
    }

    @Override
    public void onDetach() {
        Log.v(TAG, "onDetach");
        super.onDetach();
        setListener(null);
/*
        helper.stop();
        helper.destroy();
*/
    }

    void setListener(IOIOLooper listener) {
        if (this.listener != listener) {
            if (this.listener != null) {
                this.listener.disconnected();
            }
            if (listener != null && ioio != null) {
                try {
                    listener.setup(ioio);
                } catch (Exception ignored) {
                }
            }
            this.listener = listener;
        }
    }

    void onNewIntent(Intent intent) {
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            Log.v(TAG, "onNewIntent");
            helper.restart();
        }
    }

    private class Looper implements IOIOLooper {

        @Override
        @Deprecated
        public void incompatible() {
            if (listener != null) {
                listener.incompatible();
            }
        }

        @Override
        public void incompatible(IOIO ioio) {
            if (listener != null) {
                listener.incompatible(ioio);
            }
        }

        @Override
        public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
            if (ioio != null) {
                IOIOFragment.this.ioio = ioio;
                if (listener != null) {
                    listener.setup(ioio);
                }
            }
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if (listener != null) {
                listener.loop();
            }
        }

        @Override
        public void disconnected() {
            if (listener != null) {
                listener.disconnected();
            }
            IOIOFragment.this.ioio = null;
        }
    }
}