package com.bitcoin.merchant.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bitcoin.merchant.app.application.CashRegisterApplication
import com.bitcoin.merchant.app.application.NetworkStateReceiver
import com.bitcoin.merchant.app.model.Analytics
import com.bitcoin.merchant.app.network.PaymentReceived
import com.bitcoin.merchant.app.network.websocket.TxWebSocketHandler
import com.bitcoin.merchant.app.network.websocket.WebSocketListener
import com.bitcoin.merchant.app.network.websocket.impl.blockchaininfo.BlockchainInfoSocketSocketHandler
import com.bitcoin.merchant.app.network.websocket.impl.poller.PollerSocket
import com.bitcoin.merchant.app.screens.dialogs.DialogHelper
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.AmountUtil
import com.bitcoin.merchant.app.util.AppUtil
import com.bitcoin.merchant.app.util.ScanQRUtil
import com.bitcoin.merchant.app.util.Settings
import com.google.android.material.navigation.NavigationView
import okhttp3.OkHttpClient

open class MainActivity : AppCompatActivity(), WebSocketListener {
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var networkStateReceiver: NetworkStateReceiver
    var toolbar: Toolbar? = null
    lateinit var rootView: ViewGroup
        private set

    private val nav: NavController
        get() = getNav(this)

    val app: CashRegisterApplication
        get() = application as CashRegisterApplication

    lateinit var blockchainDotInfoSocket: TxWebSocketHandler

    lateinit var pollerSocket: PollerSocket

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Action.SUBSCRIBE_TO_ADDRESS == intent.action) {
                println("Subscribed to address!")
                val address = intent.getStringExtra("address")
                blockchainDotInfoSocket.subscribeToAddress(address)
                pollerSocket.subscribeToAddress(address)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (resources.getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Analytics.configure(application, this)
        setContentView(R.layout.activity_main)
        rootView = findViewById<LinearLayout>(R.id.content_frame)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to avoid overlapping with system bars
            view.updatePadding(
                top = systemBarsInsets.top,
                bottom = systemBarsInsets.bottom,
                left = systemBarsInsets.left,
                right = systemBarsInsets.right
            )

            // Return the insets so children can also consume them if needed
            insets
        }


        setToolbar()
        setNavigationDrawer()
        title = "" // clear "Bitcoin Cash Register" from toolBar when opens on Payment Input screen
        listenToConnectivityChanges()
        Log.d(TAG, "Stored " + Settings.getPaymentTarget(this))
        val filter = IntentFilter()
        filter.addAction(Action.SUBSCRIBE_TO_ADDRESS)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
        restartSocketsWhenNeeded()
    }

    fun restartSocketsWhenNeeded() {
        if (!this::blockchainDotInfoSocket.isInitialized || !blockchainDotInfoSocket.isConnected) {
            if (this::blockchainDotInfoSocket.isInitialized)
                blockchainDotInfoSocket.stop()
            blockchainDotInfoSocket = BlockchainInfoSocketSocketHandler()
            blockchainDotInfoSocket.setListener(this)
            blockchainDotInfoSocket.start()
        }
        if (!this::pollerSocket.isInitialized || !pollerSocket.isConnected) {
            if (this::pollerSocket.isInitialized)
                pollerSocket.stop()
            pollerSocket = PollerSocket(this, OkHttpClient())
            pollerSocket.start()
        }

    }

    private fun listenToConnectivityChanges() {
        networkStateReceiver = NetworkStateReceiver()
        registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onDestroy() {
        unregisterReceiver(networkStateReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
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
        setMerchantName()
        mDrawerLayout.openDrawer(GravityCompat.START)
    }

    private fun setMerchantName() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val headerView = navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.drawer_title)
        tvName.text = Settings.getMerchantName(this)
    }

    private fun setToolbar() {
        toolbar = findViewById<Toolbar>(R.id.toolbar)
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

    private val visibleFragment: ToolbarAwareFragment?
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

    private val isNavDrawerOpen: Boolean
        get() = mDrawerLayout.isDrawerOpen(GravityCompat.START)

    private fun closeNavDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun menuButtonPressed(menuItem: MenuItem) {
        // allow some time after closing the drawer before performing real navigation
        // so the user can see what is happening
        mDrawerLayout.closeDrawer(GravityCompat.START)
        val mDrawerActionHandler = Handler()
        mDrawerActionHandler.postDelayed({
            when (menuItem.itemId) {
                R.id.action_transactions -> {
                    Analytics.tap_transactions.send()
                    nav.navigate(R.id.nav_to_transactions_screen)
                }
                R.id.action_settings -> {
                    Analytics.tap_settings.send()
                    if (nav.currentDestination?.id != R.id.pin_code_screen)
                        nav.navigate(R.id.nav_to_settings_screen)
                }
                R.id.action_about -> {
                    Analytics.tap_about.send()
                    nav.navigate(R.id.nav_to_about_screen)
                }
                R.id.action_terms_of_use -> {
                    Analytics.tap_termsofuse.send()
                    nav.navigate(R.id.nav_to_terms_of_use)
                }
                R.id.action_service_terms -> {
                    Analytics.tap_serviceterms.send()
                    nav.navigate(R.id.nav_to_service_terms)
                }
                R.id.action_privacy_policy -> {
                    Analytics.tap_privacypolicy.send()
                    nav.navigate(R.id.nav_to_privacy_policy)
                }
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
        const val TAG = "BCR-MainActivity"
        const val APP_PACKAGE = "com.bitcoin.merchant.app"
        fun getNav(activity: Activity): NavController {
            return Navigation.findNavController(activity, R.id.main_nav_controller)
        }
    }

    override fun onIncomingPayment(payment: PaymentReceived?) {
        if (payment != null) {
            if (app.paymentProcessor.paymentAlreadyRecorded(payment.txHash))  {
                return;
            }
            if (payment.bchExpected != 0L && payment.fiatExpected != null) {
                if (!payment.isUnderpayment && !payment.isOverpayment) {
                    Log.d(TAG, "${payment.txHash} has been received.")
                    val i = Intent(Action.ACKNOWLEDGE_BIP21_PAYMENT)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(i)
                } else {
                    if (payment.isUnderpayment) {
                        DialogHelper.show(this, this.getString(R.string.removed_by_bip70_insufficient_payment), "") {}
                    } else {
                        DialogHelper.show(this, this.getString(R.string.removed_by_bip70_overpaid_amount), "") {}
                    }
                }

                val fiatFormatted = AmountUtil(this).formatFiat(payment.fiatExpected.toDouble())

                if (!app.paymentProcessor.paymentAlreadyRecorded(payment.txHash)) {
                    app.paymentProcessor.recordInDatabase(payment, fiatFormatted)
                } else {
                    Log.d(TAG, "${payment.txHash} has already been recorded.")
                }
            }
        }
    }
}