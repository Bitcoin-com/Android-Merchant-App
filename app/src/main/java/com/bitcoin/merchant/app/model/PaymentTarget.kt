package com.bitcoin.merchant.app.model

import android.util.Log
import com.bitcoin.merchant.app.util.AddressUtil
import info.blockchain.wallet.util.FormatsUtil
import org.apache.commons.lang3.StringUtils

data class PaymentTarget(val type: Type, val target: String) {
    val TAG = "PaymentTarget"
    val isValid = type != Type.INVALID
    val isXPub = type == Type.XPUB
    val isApiKey = type == Type.API_KEY
    val isAddress = type == Type.ADDRESS
    /**
     * Get legacy format if it's an address, otherwise returns default target.
     */
    val legacyAddress: String = target
    /**
     * Converts legacy to BCH address, otherwise returns default target.
     */
    val bchAddress: String
        get() {
            if (type == Type.ADDRESS) {
                try {
                    return AddressUtil.toCashAddress(target)
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "", e)
                }
            }
            return target
        }

    enum class Type {
        INVALID, XPUB, ADDRESS, API_KEY
    }


    companion object {
        private fun isApiKey(value: String): Boolean {
            return !StringUtils.isEmpty(value)
                    && value.matches(Regex.fromLiteral("[a-z]{40}"))
        }

        private fun isXPub(value: String): Boolean {
            return FormatsUtil.getInstance().isValidXpub(value)
        }

        @JvmStatic
        fun parse(value: String?): PaymentTarget {
            if (value == null || StringUtils.isEmpty(value))
                return PaymentTarget(Type.INVALID, "")
            if (isXPub(value))
                return PaymentTarget(Type.XPUB, value)
            if (AddressUtil.isValidLegacy(value))
                return PaymentTarget(Type.ADDRESS, value)
            if (AddressUtil.isValidCashAddr(value))
                return PaymentTarget(Type.ADDRESS, AddressUtil.toLegacyAddress(value))
            if (isApiKey(value))
                return PaymentTarget(Type.API_KEY, value)
            return PaymentTarget(Type.INVALID, "")
        }
    }
}
