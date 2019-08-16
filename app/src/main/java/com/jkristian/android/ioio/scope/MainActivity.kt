package com.jkristian.android.ioio.scope

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.jkristian.ioio.scope.R

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var model: IOIOViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        model = ViewModelProviders.of(this).get(IOIOViewModel::class.java!!)
        model!!.setContext(this)

        val actionBar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(actionBar)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        actionBar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            val item = menu.findItem(R.id.settingsFragment)
            item?.setVisible(destination.id != R.id.settingsFragment)
        }
        return true
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        model?.helper?.destroy()
        super.onDestroy()
    }

    override fun onStart() {
        Log.v(TAG, "onStart")
        super.onStart()
        model?.helper?.start()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
        }
    }

    override fun onStop() {
        Log.v(TAG, "onStop")
        model?.helper?.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        model?.onNewIntent(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        Log.i(TAG, "onRequestPermissionsResult("
                + requestCode
                + ", " + permissions.contentToString()
                + ", " + results + ")")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
}