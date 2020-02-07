package com.bitcoin.merchant.app.application

import android.app.Application
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.database.DBControllerV3
import com.bitcoin.merchant.app.util.ScanQRUtil
import com.bitcoin.merchant.app.util.Settings
import com.bitcoin.merchant.app.util.WalletUtil

class CashRegisterApplication : Application() {
    private var walletUtil: WalletUtil? = null
    lateinit var paymentProcessor: PaymentProcessor
        private set
    lateinit var db: DBControllerV3
        private set
    val qrCodeScanner by lazy { ScanQRUtil() }

    override fun onCreate() {
        super.onCreate()
        // to avoid dead-lock risk due to multi-threads access, always create it on launch
        db = DBControllerV3(this)
        paymentProcessor = PaymentProcessor(this, db)
    }

    /**
     * For performance reasons, we cache the wallet (reported in May 2019 on Lenovo Tab E8)
     */
    @get:Throws(Exception::class)
    val wallet: WalletUtil
        get() {
            val xPub = Settings.getPaymentTarget(this).target
            if (walletUtil == null || !walletUtil!!.isSameXPub(xPub)) {
                walletUtil = WalletUtil(getString(R.string.url_rest_bitcoin_com), xPub, this)
            }
            return walletUtil!!
        }
}