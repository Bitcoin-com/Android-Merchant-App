package com.bitcoin.merchant.app.util

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.bitcoin.merchant.app.model.CountryCurrencyLocale
import com.bitcoin.merchant.app.model.PaymentTarget
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper.gson
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceStatus

object Settings {
    const val TAG = "Settings"

    // Internal Settings - non editable by the user
    fun getActiveInvoice(context: Context): InvoiceStatus? {
        val invoiceJson = PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_PERSIST_INVOICE, "")
        val invoice = if (invoiceJson.isEmpty()) null else InvoiceStatus.fromJson(invoiceJson)
        if (invoice != null) {
            Log.i(TAG, "Loading active invoice...")
        }
        return invoice
    }

    fun setActiveInvoice(context: Context, i: InvoiceStatus) {
        if (i.isInitialized) {
            Log.i(TAG, "Saving active invoice...")
            PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_PERSIST_INVOICE, gson.toJson(i))
        }
    }

    fun deleteActiveInvoice(context: Context) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_PERSIST_INVOICE, "")
    }

    fun setEulaAccepted(context: Context, accepted: Boolean) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_EULA, accepted)
    }

    fun isEulaAccepted(context: Context): Boolean {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_EULA, false)
    }

    // User Settings - visible & editable by the user
    fun getMerchantName(context: Context): String {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, "")
    }

    fun setMerchantName(context: Context, name: String) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, name)
    }

    fun getPaymentTarget(context: Context): PaymentTarget {
        val value = PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "")
        return PaymentTarget.parse(value)
    }

    fun setPaymentTarget(context: Context, target: PaymentTarget) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, target.target)
    }

    fun getCountryCurrencyLocale(context: Context): CountryCurrencyLocale {
        val p = PrefsUtil.getInstance(context)
        val currency = p.getValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "")
        val country = p.getValue(PrefsUtil.MERCHANT_KEY_COUNTRY, "")
        val locale = p.getValue(PrefsUtil.MERCHANT_KEY_LANG_LOCALE, "")
        return if (currency.isEmpty() || country.isEmpty()) {
            // detect & save to avoid further auto-detection
            setCountryCurrencyLocale(context, CountryCurrencyLocale.getFromLocale(context))
        } else CountryCurrencyLocale.get(context, currency, country, locale)
    }

    fun setCountryCurrencyLocale(ctx: Context, ccl: CountryCurrencyLocale): CountryCurrencyLocale {
        val p = PrefsUtil.getInstance(ctx)
        p.setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, ccl.currency)
        p.setValue(PrefsUtil.MERCHANT_KEY_COUNTRY, ccl.iso)
        p.setValue(PrefsUtil.MERCHANT_KEY_LANG_LOCALE, ccl.lang)
        return ccl
    }

    fun getPinCode(context: Context): String {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_PIN, "")
    }

    fun setPinCode(context: Context, pinCode: String) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_PIN, pinCode)
    }

    fun setXPubIndex(context: Context, xPub: String, newIndex: Int) {
        PrefsUtil.getInstance(context).setValue(getXPubKey(xPub), newIndex)
    }

    fun getXPubIndex(context: Context, xPub: String): Int {
        return PrefsUtil.getInstance(context).getValue(getXPubKey(xPub), 0)
    }

    private fun getXPubKey(xPub: String) = PrefsUtil.MERCHANT_KEY_XPUB_INDEX + "_" + xPub

    private class PrefsUtil private constructor() {
        companion object {
            const val MERCHANT_KEY_PIN = "pin"
            const val MERCHANT_KEY_CURRENCY = "currency"
            const val MERCHANT_KEY_COUNTRY = "country"
            const val MERCHANT_KEY_LANG_LOCALE = "locale"
            const val MERCHANT_KEY_MERCHANT_NAME = "receiving_name"
            const val MERCHANT_KEY_MERCHANT_RECEIVER = "receiving_address"
            const val MERCHANT_KEY_XPUB_INDEX = "xpub_index"
            const val MERCHANT_KEY_EULA = "eula"
            const val MERCHANT_KEY_PERSIST_INVOICE = "persist_invoice"
            private lateinit var context: Context
            private lateinit var instance: PrefsUtil
            fun getInstance(ctx: Context): PrefsUtil {
                context = ctx
                if (!::instance.isInitialized) {
                    instance = PrefsUtil()
                }
                return instance
            }
        }

        fun getValue(name: String, value: String): String {
            // TODO fix deprecation
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString(name, value) ?: ""
        }

        fun setValue(name: String, value: String): Boolean {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString(name, value)
            return editor.commit()
        }

        fun getValue(name: String, value: Int): Int {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getInt(name, 0)
        }

        fun setValue(name: String, value: Int): Boolean {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putInt(name, if (value < 0) 0 else value)
            return editor.commit()
        }

        fun getValue(name: String, value: Boolean): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(name, value)
        }

        fun setValue(name: String, value: Boolean): Boolean {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean(name, value)
            return editor.commit()
        }
    }
}
