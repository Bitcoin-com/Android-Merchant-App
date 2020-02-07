package com.bitcoin.merchant.app

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bitcoin.merchant.app.application.CashRegisterApplication
import com.bitcoin.merchant.app.application.NetworkStateReceiver
import com.bitcoin.merchant.app.screens.dialogs.DialogHelper
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.AppUtil
import com.bitcoin.merchant.app.util.ScanQRUtil
import com.bitcoin.merchant.app.util.Settings
import com.crashlytics.android.Crashlytics
import com.google.android.material.navigation.NavigationView
import io.fabric.sdk.android.Fabric

open class MainActivity : AppCompatActivity() {
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var networkStateReceiver: NetworkStateReceiver
    lateinit var toolbar: Toolbar
        private set
    lateinit var rootView: ViewGroup
        private set

    private val nav: NavController
        get() = getNav(this)

    val app: CashRegisterApplication
        get() = application as CashRegisterApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AppUtil.isEmulator) {
            Fabric.with(this, Crashlytics())
        }
        setContentView(R.layout.activity_main)
        rootView = findViewById(R.id.content_frame)
        setToolbar()
        setNavigationDrawer()
        title = "" // clear "Bitcoin Cash Register" from toolBar when opens on Payment Input screen
        listenToConnectivityChanges()
        Log.d(TAG, "Stored " + Settings.getPaymentTarget(this))
        if (!Settings.isEulaAccepted(this)) {
            DialogHelper.showEndUserLegalAgreement(this)
        }
    }

    private fun listenToConnectivityChanges() {
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        networkStateReceiver = NetworkStateReceiver()
        registerReceiver(networkStateReceiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(networkStateReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        setMerchantName()
        val fragment = visibleFragment
        // Note: The navigation starts the payment_input_screen due to config in navigation.xml
        if (fragment != null && !fragment.canFragmentBeDiscardedWhenInBackground()) {
            return  // keep current screen, do not pop any screen
        }
        // Remove all screens from the stack until we reach the payment_input_screen
        nav.popBackStack(R.id.payment_input_screen, false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    fun openMenuDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START)
    }

    private fun setMerchantName() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val headerView = navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.drawer_title)
        tvName.text = Settings.getMerchantName(this)
    }

    fun setToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout)
        setMerchantName()
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            menuButtonPressed(menuItem)
            false
        }
        mDrawerLayout.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(view: View, v: Float) {
                val color = if (v > 0) R.color.bitcoindotcom_green else R.color.gray
                AppUtil.setStatusBarColor(this@MainActivity, color)
            }

            override fun onDrawerOpened(view: View) {
                AppUtil.setStatusBarColor(this@MainActivity, R.color.bitcoindotcom_green)
            }

            override fun onDrawerClosed(view: View) {
                AppUtil.setStatusBarColor(this@MainActivity, R.color.gray)
            }

            override fun onDrawerStateChanged(i: Int) {}
        })
    }

    val visibleFragment: ToolbarAwareFragment?
        get() {
            val navHostFragment = supportFragmentManager.primaryNavigationFragment ?: return null
            for (fragment in navHostFragment.childFragmentManager.fragments) {
                if (fragment is ToolbarAwareFragment && fragment.isVisible()) {
                    return fragment
                }
            }
            return null
        }

    override fun onBackPressed() {
        if (isNavDrawerOpen) {
            closeNavDrawer()
        }
        val fragment = visibleFragment
        if (fragment != null && !fragment.isBackAllowed) {
            return
        }
        super.onBackPressed()
    }

    protected val isNavDrawerOpen: Boolean
        get() = mDrawerLayout.isDrawerOpen(GravityCompat.START)

    protected fun closeNavDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun menuButtonPressed(menuItem: MenuItem) {
        // allow some time after closing the drawer before performing real navigation
        // so the user can see what is happening
        mDrawerLayout.closeDrawer(GravityCompat.START)
        val mDrawerActionHandler = Handler()
        mDrawerActionHandler.postDelayed({
            when (menuItem.itemId) {
                R.id.action_transactions -> nav.navigate(R.id.nav_to_transactions_screen)
                R.id.action_settings -> nav.navigate(R.id.nav_to_settings_screen)
                R.id.action_about -> nav.navigate(R.id.nav_to_about_screen)
                R.id.action_terms_of_use -> nav.navigate(R.id.nav_to_terms_of_use)
                R.id.action_service_terms -> nav.navigate(R.id.nav_to_service_terms)
                R.id.action_privacy_policy -> nav.navigate(R.id.nav_to_privacy_policy)
            }
        }, 250)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        app.qrCodeScanner.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, result: Int, data: Intent?) {
        super.onActivityResult(requestCode, result, data)
        if (result == Activity.RESULT_OK && requestCode == ScanQRUtil.ZXING_SCAN_REQUEST && data != null) {
            val paymentTarget = data.getStringExtra(ScanQRUtil.ZXING_SCAN_RESULT)
            Log.v(TAG, "requestCode:$requestCode, resultCode:$result, paymentTarget:$paymentTarget")
            val i = Intent(Action.SET_PAYMENT_TARGET)
            i.putExtra(Action.PARAM_PAYMENT_TARGET, paymentTarget)
            LocalBroadcastManager.getInstance(this).sendBroadcast(i)
        } else {
            Log.v(TAG, "requestCode:$requestCode, resultCode:$result")
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val APP_PACKAGE = "com.bitcoin.merchant.app"
        fun getNav(activity: Activity): NavController {
            return Navigation.findNavController(activity, R.id.main_nav_controller)
        }
    }
}