package com.bitcoin.merchant.app.util

import org.bitcoinj.core.Address
import org.bitcoinj.core.CashAddressFactory
import org.bitcoinj.params.MainNetParams

object AddressUtil {
    fun isValidCashAddr(address: String?): Boolean {
        return Address.isValidCashAddr(MainNetParams.get(), address)
    }

    fun isValidLegacy(address: String): Boolean {
        return Address.isValidLegacyAddress(MainNetParams.get(), address)
    }

    fun toCashAddress(legacy: String): String {
        return CashAddressFactory.create().getFromBase58(MainNetParams.get(), legacy).toString()
    }
}