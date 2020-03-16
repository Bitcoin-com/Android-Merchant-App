package com.bitcoin.merchant.app.screens

import android.content.*
import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bitcoin.merchant.app.MainActivity
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.model.PaymentTarget
import com.bitcoin.merchant.app.screens.dialogs.DialogHelper
import com.bitcoin.merchant.app.screens.dialogs.SnackHelper
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.AmountUtil
import com.bitcoin.merchant.app.util.AppUtil
import com.bitcoin.merchant.app.util.MonetaryUtil
import com.bitcoin.merchant.app.util.Settings
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoindotcom.bchprocessor.bip70.Bip70Manager
import org.bitcoindotcom.bchprocessor.bip70.Bip70PayService
import org.bitcoindotcom.bchprocessor.bip70.model.Bip70Action
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceRequest
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceStatus
import retrofit2.Response
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.NullPointerException
import java.util.*

class PaymentRequestFragment : ToolbarAwareFragment() {
    // Ensure that pressing 'BACK' button stays on the 'Payment REQUEST' screen to NOT lose the active invoice
    // unless we are exiting the screen
    private var backButtonAllowed: Boolean = false
    private lateinit var fabShare: FloatingActionButton
    private lateinit var waitingLayout: LinearLayout
    private lateinit var receivedLayout: LinearLayout
    private lateinit var tvConnectionStatus: ImageView
    private lateinit var tvFiatAmount: TextView
    private lateinit var tvCoinAmount: TextView
    private lateinit var tvExpiryTimer: TextView
    private lateinit var ivReceivingQr: ImageView
    private lateinit var progressLayout: LinearLayout
    private lateinit var ivCancel: ImageView
    private lateinit var ivDone: Button
    private lateinit var bip70Manager: Bip70Manager
    private lateinit var bip70PayService: Bip70PayService
    private var lastProcessedInvoicePaymentId: String? = null
    private var qrCodeUri: String? = null
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
                bip70Manager.reconnectIfNecessary()
            }
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
        val fiatFormatted = AmountUtil(activity).formatFiat(i.fiatTotal)
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
        val amountFiat = args?.getDouble(PaymentInputFragment.AMOUNT_PAYABLE_FIAT, 0.0) ?: 0.0
        if (amountFiat > 0.0) {
            createNewInvoice(amountFiat)
        } else {
            resumeExistingInvoice()
        }
        return v
    }

    private fun createNewInvoice(amountFiat: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            val invoiceRequest = withContext(Dispatchers.IO) {
                createInvoice(amountFiat, Settings.getCountryCurrencyLocale(activity).currency)
            }
            if (invoiceRequest == null) {
                unableToDisplayInvoice()
            } else {
                // do NOT delete active invoice too early
                // because this Fragment is always instantiated below the PaymentRequest
                // when resuming from a crash on the PaymentRequest
                Settings.deleteActiveInvoice(activity)
                tvFiatAmount.text = AmountUtil(activity).formatFiat(amountFiat)
                tvCoinAmount.visibility = View.INVISIBLE  // default values are incorrect
                setWorkInProgress(true)
                val invoice: InvoiceStatus? = downloadInvoice(invoiceRequest)
                invoice?.let {
                    connectToSocketAndGenerateQrCode(invoice)?.also { showQrCodeAndAmountFields(invoice, it) }
                }
                setWorkInProgress(false)
            }
        }
    }

    private fun resumeExistingInvoice() {
        val invoice = Settings.getActiveInvoice(activity)
        if (invoice == null) {
            unableToDisplayInvoice()
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                setWorkInProgress(true)
                tvFiatAmount.visibility = View.INVISIBLE  // default values are incorrect
                tvCoinAmount.visibility = View.INVISIBLE  // default values are incorrect
                connectToSocketAndGenerateQrCode(invoice)?.also { showQrCodeAndAmountFields(invoice, it) }
                setWorkInProgress(false)
            }
        }
    }

    private fun unableToDisplayInvoice() {
        DialogHelper.show(activity, getString(R.string.error), getString(R.string.unable_to_generate_address)) {
            exitScreen()
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter()
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
        tvCoinAmount = v.findViewById(R.id.tv_btc_amount)
        tvExpiryTimer = v.findViewById(R.id.bip70_timer_tv)
        ivReceivingQr = v.findViewById(R.id.qr)
        progressLayout = v.findViewById(R.id.progressLayout)
        waitingLayout = v.findViewById(R.id.layout_waiting)
        receivedLayout = v.findViewById(R.id.layout_complete)
        ivCancel = v.findViewById(R.id.iv_cancel)
        ivDone = v.findViewById(R.id.iv_done)
        fabShare = v.findViewById(R.id.fab_share)
        setWorkInProgress(true)
        ivCancel.setOnClickListener { deleteActiveInvoiceAndExitScreen() }
        ivReceivingQr.setOnClickListener { copyQrCodeToClipboard() }
        fabShare.setOnClickListener { startShareIntent(qrCodeUri) }
        waitingLayout.visibility = View.VISIBLE
        receivedLayout.visibility = View.GONE
    }

    private fun setWorkInProgress(enabled: Boolean) {
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
            val emojiClipboard = String(Character.toChars(0x1F4CB))
            SnackHelper.show(activity, emojiClipboard + " ${qrCodeUri}");
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
                // known limitation: we only check for used addresses when setting the xPub
                // as a consequence if the same xPubKey is used on multiple cashiers/terminals
                // then addresses can be reused. Address reuse is not an issue
                // because the BIP-70 server is the one only broadcasting the TX to that address
                // and thus it is aware of which invoice is being paid without possible confusion
                i.address = app.wallet.getAddressFromXPubAndMoveToNext()
                Log.i(MainActivity.TAG, "BCH-address(xPub) to receive: " + i.address)
            } catch (e: Exception) {
                Log.e(MainActivity.TAG, "", e)
                return null
            }
        }
        return i
    }

    private suspend fun downloadInvoice(request: InvoiceRequest): InvoiceStatus? {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<InvoiceStatus?> = bip70PayService.createInvoice(request).execute()
                val invoice = response.body()
                        ?: throw Exception("HTTP status:" + response.code() + " message:" + response.message())
                Settings.setActiveInvoice(activity, invoice)
                invoice
            } catch (e: Exception) {
                DialogHelper.show(activity, activity.getString(R.string.error), e.message) { exitScreen() }
                null
            }
        }
    }

    private suspend fun connectToSocketAndGenerateQrCode(invoice: InvoiceStatus): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // connect the socket first before showing the bitmap as connection takes longer
                bip70Manager.startWebsockets(invoice.paymentId)
                qrCodeUri = invoice.walletUri
                Log.d(MainActivity.TAG, "paymentUrl:${invoice.walletUri}")
                val width = activity.resources.getInteger(R.integer.qr_code_width)
                getQrCodeAsBitmap(invoice.walletUri, width)
            } catch (e: Exception) {
                DialogHelper.show(activity, activity.getString(R.string.error), e.message) { exitScreen() }
                null
            }
        }
    }

    @Throws(Exception::class)
    private fun getQrCodeAsBitmap(text: String, width: Int): Bitmap {
        val result: BitMatrix = try {
            MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, width, null)
        } catch (e: Exception) {
            throw Exception("Unsupported format", e)
        }
        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) BLACK else WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, w, h)
        return bitmap
    }

    private fun showQrCodeAndAmountFields(i: InvoiceStatus, bitmap: Bitmap) {
        val f = AmountUtil(activity)
        tvFiatAmount.text = f.formatFiat(i.fiatTotal)
        tvFiatAmount.visibility = View.VISIBLE
        tvCoinAmount.text = MonetaryUtil.instance.getDisplayAmountWithFormatting(i.totalAmountInSatoshi) + " BCH"
        tvCoinAmount.visibility = View.VISIBLE
        ivReceivingQr.setImageBitmap(bitmap)
        initiateCountdown(i)
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
        tvConnectionStatus.visibility = View.GONE // hide it white top bar on green background
        waitingLayout.visibility = View.GONE
        receivedLayout.visibility = View.VISIBLE
        AppUtil.setStatusBarColor(activity, R.color.bitcoindotcom_green)
        Settings.deleteActiveInvoice(activity)
        ivDone.setOnClickListener {
            AppUtil.setStatusBarColor(activity, R.color.gray)
            exitScreen()
        }
    }

    private fun startShareIntent(paymentUrl: String?) {
        if(paymentUrl != null) {
            try {
                //TODO remove hardcoded and put them in strings.xml
                val urlWithoutPrefix = paymentUrl.replace("bitcoincash:?r=", "")
                val invoiceId = urlWithoutPrefix.replace("https://pay.bitcoin.com/i/", "")
                val bitmap = ivReceivingQr.drawable.toBitmap(220, 220)
                val file = File(this@PaymentRequestFragment.app.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "invoice.png")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, out)
                out.close();
                val bitmapUri = FileProvider.getUriForFile(this@PaymentRequestFragment.context!!, context?.applicationContext?.packageName + ".provider", file)
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    //TODO extract string resource
                    putExtra(Intent.EXTRA_TEXT, "Please pay your invoice here: $urlWithoutPrefix")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    putExtra(Intent.EXTRA_STREAM, bitmapUri)
                    type = "image/*"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            } catch (e: Exception) {

            }
        } else {
            SnackHelper.show(activity, activity.getString(R.string.share_invoice_npe), error = true)
        }
    }

    override val isBackAllowed: Boolean
        get() {
            return backButtonAllowed
        }
}