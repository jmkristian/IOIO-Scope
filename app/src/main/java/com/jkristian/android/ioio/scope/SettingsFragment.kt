package com.jkristian.android.ioio.scope

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.jkristian.ioio.scope.R

private const val TAG = "SettingsFragment"
// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
// private const val ARG_PARAM1 = "param1"

class SettingsFragment : PreferenceFragmentCompat() {
    // TODO: Rename and change types of parameters
    // private var param1: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.v(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
/*
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
*/
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
                SettingsFragment().apply {
                    arguments = Bundle().apply {
                        // putString(ARG_PARAM1, param1)
                    }
                }
    }
}
