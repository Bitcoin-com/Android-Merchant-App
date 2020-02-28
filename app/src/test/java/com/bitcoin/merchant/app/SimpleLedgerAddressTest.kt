package com.bitcoin.merchant.app

import com.bitcoin.merchant.app.util.AddressUtil
import org.bitcoinj.core.SlpAddress
import org.bitcoinj.params.MainNetParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SimpleLedgerAddressTest {
    @Test
    fun validateSlpAddr() {
        val address = "simpleledger:qqrxa0h9jqnc7v4wmj9ysetsp3y7w9l36utncg8up7"
        val cashAddr = AddressUtil.fromSimpleLedgerAddress(address)
        assert(AddressUtil.isValidCashAddr(cashAddr))
    }

    @Test
    fun cashAddrToSlpAddr() {
        val address = "bitcoincash:qqrxa0h9jqnc7v4wmj9ysetsp3y7w9l36u8gnnjulq"
        val slpAddr = AddressUtil.toSimpleLedgerAddress(address)
        assertEquals(slpAddr, "simpleledger:qqrxa0h9jqnc7v4wmj9ysetsp3y7w9l36utncg8up7")
    }

    @Test
    fun slpAddrToCashAddr() {
        val address = "simpleledger:qqrxa0h9jqnc7v4wmj9ysetsp3y7w9l36utncg8up7"
        val cashAddr = AddressUtil.fromSimpleLedgerAddress(address)
        assertEquals(cashAddr, "bitcoincash:qqrxa0h9jqnc7v4wmj9ysetsp3y7w9l36u8gnnjulq")
    }
}