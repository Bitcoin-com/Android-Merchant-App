package com.bitcoin.merchant.app.screens.features

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.bitcoin.merchant.app.MainActivity
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.application.CashRegisterApplication
import com.bitcoin.merchant.app.util.AppUtil

abstract class ToolbarAwareFragment : Fragment() {
    lateinit var activity: MainActivity
    val isActivityInitialized get() = this::activity.isInitialized
    var toolbar: Toolbar? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = getActivity() as MainActivity
        toolbar = activity.toolbar
        if (AppUtil.isEmulator) {
            printFragmentStack()
        }
        return null
    }

    private fun printFragmentStack() {
        val navHostFragment = activity.supportFragmentManager.primaryNavigationFragment
        val stackCount = navHostFragment?.childFragmentManager?.backStackEntryCount
        Log.i("Nav", "Current=" + javaClass.simpleName + ", Stack=" + stackCount)
    }

    fun setToolbarVisible(enabled: Boolean) {
        toolbar?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    fun setToolbarAsMenuButton() {
        setToolbarVisible(true)
        toolbar?.setTitleTextColor(Color.BLACK)
        toolbar?.setNavigationIcon(R.drawable.ic_menu_black_24dp)
        toolbar?.setNavigationOnClickListener { activity.openMenuDrawer() }
    }

    fun setToolbarAsBackButton() {
        setToolbarVisible(true)
        toolbar?.setTitleTextColor(Color.BLACK)
        toolbar?.setNavigationIcon(R.drawable.ic_back_black_24dp)
        toolbar?.setNavigationOnClickListener { activity.onBackPressed() }
    }

    fun clearToolbarTitle() {
        setToolbarVisible(true)
        toolbar?.title = ""
    }

    fun setToolbarTitle(titleResourceId: Int) {
        setToolbarVisible(true)
        toolbar?.setTitle(titleResourceId)
    }

    val app: CashRegisterApplication
        get() = activity.app

    protected val nav: NavController
        get() = MainActivity.getNav(activity)

    /**
     * @return true if already managed
     */
    open val isBackAllowed: Boolean
        get() {
            if (AppUtil.isEmulator) {
                printFragmentStack()
            }
            return true
        }

    open fun canFragmentBeDiscardedWhenInBackground(): Boolean {
        return false
    }
}