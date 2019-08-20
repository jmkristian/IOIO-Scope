package com.jkristian.android.ioio.scope

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import com.jkristian.ioio.scope.R

private const val TAG = "SettingsFragment"

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.v(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)
    }

    override fun onDetach() {
        Log.v(TAG, "onDetach")
        super.onDetach()
    }

    companion object {
        const val SMS_TO = "SMS_to"
    }
}