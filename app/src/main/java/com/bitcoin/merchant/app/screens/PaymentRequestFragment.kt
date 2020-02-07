package com.bitcoin.merchant.app.screens

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.merchant.app.MainActivity
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.model.PaymentTarget
import com.bitcoin.merchant.app.screens.dialogs.DialogHelper
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.AmountUtil
import com.bitcoin.merchant.app.util.AppUtil
import com.bitcoin.merchant.app.util.Settings
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.bitcoindotcom.bchprocessor.bip70.Bip70Manager
import org.bitcoindotcom.bchprocessor.bip70.Bip70PayService
import org.bitcoindotcom.bchprocessor.bip70.model.Bip70Action
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceRequest
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceStatus
import retrofit2.Response
import java.net.SocketTimeoutException
import java.util.*

class PaymentRequestFragment : ToolbarAwareFragment() {
    // Ensure that pressing 'BACK' button stays on the 'Payment REQUEST' screen to NOT lose the active invoice
    // unless we are exiting the screen
    private var backButtonAllowed: Boolean = false
    private lateinit var waitingLayout: LinearLayout
    private lateinit var receivedLayout: LinearLayout
    private lateinit var tvConnectionStatus: ImageView
    private lateinit var tvFiatAmount: TextView
    private lateinit var tvBtcAmount: TextView
    private lateinit var tvExpiryTimer: TextView
    private lateinit var ivReceivingQr: ImageView
    private lateinit var progressLayout: LinearLayout
    private lateinit var ivCancel: Button
    private lateinit var ivDone: Button
    private lateinit var bip70Manager: Bip70Manager
    private lateinit var bip70PayService: Bip70PayService
    private var lastProcessedInvoicePaymentId: String? = null
    private var qrCodeUri: String? = null
    private var fiatFormatted: String? = null
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Bip70Action.INVOICE_PAYMENT_ACKNOWLEDGED == intent.action) {
                acknowledgePayment(InvoiceStatus.fromJson(intent.getStringExtra(Bip70Action.PARAM_INVOICE_STATUS)))
            }
            if (Bip70Action.INVOICE_PAYMENT_EXPIRED == intent.action) {
                expirePayment(InvoiceStatus.fromJson(intent.getStringExtra(Bip70Action.PARAM_INVOICE_STATUS)))
            }
            if (Bip70Action.UPDATE_CONNECTION_STATUS == intent.action) {
                updateConnectionStatus(intent.getBooleanExtra(Bip70Action.PARAM_CONNECTION_STATUS_ENABLED, false))
            }
            if (Bip70Action.NETWORK_RECONNECT == intent.action) {
                reconnectIfNecessary()
            }
            // TODO fix this
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
                if (intent.extras != null) {
                    reconnectIfNecessary()
                }
            }
        }

        private fun reconnectIfNecessary() {
            bip70Manager.reconnectIfNecessary()
        }
    }

    private fun expirePayment(invoiceStatus: InvoiceStatus) {
        if (markInvoiceAsProcessed(invoiceStatus)) {
            return
        }
        exitScreen()
    }

    private fun updateConnectionStatus(enabled: Boolean) {
        Log.d(MainActivity.TAG, "Socket " + if (enabled) "connected" else "disconnected")
        tvConnectionStatus.setImageResource(if (enabled) R.drawable.connected else R.drawable.disconnected)
    }

    private fun acknowledgePayment(i: InvoiceStatus) {
        if (markInvoiceAsProcessed(i)) {
            return
        }
        Log.i(MainActivity.TAG, "record new Tx:$i")
        app.paymentProcessor.recordInDatabase(i, fiatFormatted)
        showCheckMark()
        soundAlert()
    }

    /**
     * @return true if it was already processed, false otherwise
     */
    private fun markInvoiceAsProcessed(invoiceStatus: InvoiceStatus): Boolean {
        Settings.deleteActiveInvoice(activity)
        // Check that it has not yet been processed to avoid redundant processing
        if (lastProcessedInvoicePaymentId == invoiceStatus.paymentId) {
            Log.i(MainActivity.TAG, "Already processed invoice:$invoiceStatus")
            return true
        }
        lastProcessedInvoicePaymentId = invoiceStatus.paymentId
        return false
    }

    private fun soundAlert() {
        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null && audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val mp = MediaPlayer.create(activity, R.raw.alert)
            mp.setOnCompletionListener { player: MediaPlayer ->
                player.reset()
                player.release()
            }
            mp.start()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.fragment_request_payment, container, false)
        initViews(v)
        setToolbarVisible(false)
        registerReceiver()
        bip70PayService = Bip70PayService.create(resources.getString(R.string.bip70_bitcoin_com_host))
        bip70Manager = Bip70Manager(app)
        val args = arguments
        val f = AmountUtil(activity)
        val amountFiat = args?.getDouble(PaymentInputFragment.AMOUNT_PAYABLE_FIAT, 0.0)
                ?: 0.0
        if (amountFiat > 0.0) {
            val invoiceRequest = createInvoice(amountFiat, Settings.getCountryCurrencyLocale(activity).currency)
            if (invoiceRequest == null) {
                unableToDisplayInvoice()
            } else {
                // do NOT delete active invoice too early
                // because this Fragment is always instantiated below the PaymentRequest
                // when resuming from a crash on the PaymentRequest
                Settings.deleteActiveInvoice(activity)
                fiatFormatted = f.formatFiat(amountFiat)
                tvFiatAmount.text = fiatFormatted
                generateInvoiceAndWaitForPayment(invoiceRequest)
            }
        } else {
            val activeInvoice = Settings.getActiveInvoice(activity)
            if (activeInvoice == null) {
                unableToDisplayInvoice()
            } else {
                generateQrCodeAndWaitForPayment(activeInvoice)
            }
        }
        return v
    }

    private fun unableToDisplayInvoice() {
        DialogHelper.show(activity, getString(R.string.error), getString(R.string.unable_to_generate_address)) {
            exitScreen()
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION) // TODO fix
        filter.addAction(Bip70Action.INVOICE_PAYMENT_ACKNOWLEDGED)
        filter.addAction(Bip70Action.INVOICE_PAYMENT_EXPIRED)
        filter.addAction(Bip70Action.UPDATE_CONNECTION_STATUS)
        filter.addAction(Bip70Action.NETWORK_RECONNECT)
        LocalBroadcastManager.getInstance(activity).registerReceiver(receiver, filter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::bip70Manager.isInitialized) {
            bip70Manager.stopSocket()
        }
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver)
    }

    private fun initViews(v: View) {
        tvConnectionStatus = v.findViewById(R.id.tv_connection_status)
        tvFiatAmount = v.findViewById(R.id.tv_fiat_amount)
        tvBtcAmount = v.findViewById(R.id.tv_btc_amount)
        tvExpiryTimer = v.findViewById(R.id.bip70_timer_tv)
        ivReceivingQr = v.findViewById(R.id.qr)
        progressLayout = v.findViewById(R.id.progressLayout)
        waitingLayout = v.findViewById(R.id.layout_waiting)
        receivedLayout = v.findViewById(R.id.layout_complete)
        ivCancel = v.findViewById(R.id.iv_cancel)
        ivDone = v.findViewById(R.id.iv_done)
        showGeneratingQrCodeProgress(true)
        ivCancel.setOnClickListener { deleteActiveInvoiceAndExitScreen() }
        ivReceivingQr.setOnClickListener { copyQrCodeToClipboard() }
        waitingLayout.visibility = View.VISIBLE
        receivedLayout.visibility = View.GONE
    }

    private fun showGeneratingQrCodeProgress(enabled: Boolean) {
        progressLayout.visibility = if (enabled) View.VISIBLE else View.GONE
        ivReceivingQr.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun deleteActiveInvoiceAndExitScreen() {
        Settings.deleteActiveInvoice(activity)
        exitScreen()
    }

    private fun exitScreen() {
        backButtonAllowed = true
        activity.onBackPressed()
        backButtonAllowed = false
    }

    private fun copyQrCodeToClipboard() {
        try {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(qrCodeUri, qrCodeUri)
            clipboard.setPrimaryClip(clip)
            Log.i(MainActivity.TAG, "Copied to clipboard: $qrCodeUri")
        } catch (e: Exception) {
            Log.i(MainActivity.TAG, "Failed to copy to clipboard: $qrCodeUri")
        }
    }

    private fun createInvoice(amountFiat: Double, currency: String): InvoiceRequest? {
        val paymentTarget = Settings.getPaymentTarget(activity)
        val i = InvoiceRequest("" + amountFiat, currency)
        when (paymentTarget.type) {
            PaymentTarget.Type.INVALID -> return null
            PaymentTarget.Type.API_KEY -> i.apiKey = paymentTarget.target
            PaymentTarget.Type.ADDRESS -> i.address = paymentTarget.legacyAddress
            PaymentTarget.Type.XPUB -> try {
                i.address = app.wallet.generateAddressFromXPub()
                Log.i(MainActivity.TAG, "BCH-address(xPub) to receive: " + i.address)
            } catch (e: Exception) {
                Log.e(MainActivity.TAG, "", e)
                return null
            }
        }
        return i
    }

    @SuppressLint("StaticFieldLeak")
    private fun generateInvoiceAndWaitForPayment(invoiceRequest: InvoiceRequest) {
        object : AsyncTask<InvoiceRequest?, Void?, Pair<InvoiceStatus?, Bitmap?>>() {
            override fun onPreExecute() {
                super.onPreExecute()
                showGeneratingQrCodeProgress(true)
            }

            override fun doInBackground(vararg requests: InvoiceRequest?): Pair<InvoiceStatus?, Bitmap?> {
                val request = requests[0]!!
                var invoice: InvoiceStatus? = null
                var bitmap: Bitmap? = null
                try {
                    val response: Response<InvoiceStatus?> = bip70PayService.createInvoice(request).execute()
                    invoice = response.body()
                    if (invoice == null) {
                        throw Exception("HTTP status:" + response.code() + " message:" + response.message())
                    } else {
                        Settings.setActiveInvoice(activity, invoice)
                    }
                    qrCodeUri = invoice.walletUri
                    // connect the socket first before showing the bitmap
                    bip70Manager.startWebsockets(invoice.paymentId)
                    bitmap = getQrCodeBitmap(invoice.walletUri)


                } catch (e: Exception) {
                    if (e !is SocketTimeoutException) {
                        Log.e(MainActivity.TAG, "", e)
                    }
                    DialogHelper.show(activity, activity.getString(R.string.error), e.message) { exitScreen() }
                }
                return Pair(invoice, bitmap)
            }

            override fun onPostExecute(pair: Pair<InvoiceStatus?, Bitmap?>) {
                super.onPostExecute(pair)
                showGeneratingQrCodeProgress(false)
                showQrCodeAndAmountFields(pair)
            }
        }.execute(invoiceRequest)
    }

    @Throws(Exception::class)
    private fun getQrCodeBitmap(url: String): Bitmap? {
        Log.d(MainActivity.TAG, "paymentUrl:$url")
        return encodeAsBitmap(url, 260)
    }

    @Throws(Exception::class)
    private fun encodeAsBitmap(text: String, width: Int) : Bitmap? {
        val result: BitMatrix = try {
            MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, width, null);
        } catch (e : Exception ) {
             return null // Unsupported format
        }
        val w = result.getWidth();
        val h = result.getHeight();
        val pixels = IntArray(w * h);
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) BLACK else WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, w, h);
        return bitmap;
    }

    private fun showQrCodeAndAmountFields(pair: Pair<InvoiceStatus?, Bitmap?>) {
        val i = pair.first
        val bitmap = pair.second
        if (i != null && bitmap != null) {
            val f = AmountUtil(activity)
            tvFiatAmount.text = f.formatFiat(i.fiatTotal)
            tvBtcAmount.text = f.formatBch(i.totalBchAmount.toDouble())
            ivReceivingQr.setImageBitmap(bitmap)
            initiateCountdown(i)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun generateQrCodeAndWaitForPayment(invoiceStatus: InvoiceStatus) {
        object : AsyncTask<InvoiceStatus?, Void?, Pair<InvoiceStatus?, Bitmap?>>() {
            override fun onPreExecute() {
                super.onPreExecute()
                showGeneratingQrCodeProgress(true)
            }

            override fun doInBackground(vararg invoices: InvoiceStatus?): Pair<InvoiceStatus?, Bitmap?> {
                val invoice = invoices[0]!!
                var bitmap: Bitmap? = null
                try {
                    qrCodeUri = invoice.walletUri
                    // connect the socket first before showing the bitmap
                    bip70Manager.startWebsockets(invoice.paymentId)
                    bitmap = getQrCodeBitmap(invoice.walletUri)
                } catch (e: Exception) {
                    if (e !is SocketTimeoutException) {
                        Log.e(MainActivity.TAG, "", e)
                    }
                    DialogHelper.show(activity, activity.getString(R.string.error), e.message) { exitScreen() }
                }
                return Pair(invoice, bitmap)
            }

            override fun onPostExecute(pair: Pair<InvoiceStatus?, Bitmap?>) {
                super.onPostExecute(pair)
                showGeneratingQrCodeProgress(false)
                showQrCodeAndAmountFields(pair)
            }
        }.execute(invoiceStatus)
    }

    private fun getTimeLimit(invoiceStatus: InvoiceStatus): Long {
        // Do NOT use invoiceStatus.getTime() because it won't reflect the current time
        // when a persisted invoice is restored
        val expireGmt = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        expireGmt.time = invoiceStatus.expires
        val currentTimeInUtcMillis = System.currentTimeMillis() - TimeZone.getDefault().rawOffset
        return expireGmt.timeInMillis - currentTimeInUtcMillis
    }

    private fun initiateCountdown(invoiceStatus: InvoiceStatus) {
        val timeLimit = getTimeLimit(invoiceStatus)
        object : CountDownTimer(timeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (isAdded) {
                    val secondsLeft = millisUntilFinished / 1000L
                    val locale = Locale.getDefault()
                    tvExpiryTimer.text = String.format(locale, "%02d:%02d", secondsLeft / 60, secondsLeft % 60)
                    updateConnectionStatus(bip70Manager.socketHandler?.isConnected ?: false)
                }
            }

            override fun onFinish() {}
        }.start()
    }

    private fun showCheckMark() {
        waitingLayout.visibility = View.GONE
        receivedLayout.visibility = View.VISIBLE
        AppUtil.setStatusBarColor(activity, R.color.bitcoindotcom_green)
        Settings.deleteActiveInvoice(activity)
        ivDone.setOnClickListener {
            AppUtil.setStatusBarColor(activity, R.color.gray)
            exitScreen()
        }
    }

    override val isBackAllowed: Boolean
        get() {
            return backButtonAllowed
        }
}