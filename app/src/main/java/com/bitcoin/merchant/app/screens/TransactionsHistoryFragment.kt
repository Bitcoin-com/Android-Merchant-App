package com.bitcoin.merchant.app.screens

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.database.PaymentRecord
import com.bitcoin.merchant.app.database.toPaymentRecord
import com.bitcoin.merchant.app.model.Analytics
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.DateUtil
import com.bitcoin.merchant.app.util.MonetaryUtil
import com.bitcoin.merchant.app.util.ReceiptUtil
import com.bitcoin.merchant.app.util.Settings
import kotlinx.android.synthetic.main.fragment_transaction.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import org.bitcoindotcom.bchprocessor.bip70.model.Bip70Action
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs

class TransactionsHistoryFragment : ToolbarAwareFragment() {
    private lateinit var adapter: TransactionAdapter
    private lateinit var listView: ListView
    private lateinit var noTxHistoryLv: LinearLayout
    private lateinit var dateFilterArea: ViewGroup
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var datePresetArea: ViewGroup
    private lateinit var todayButton: Button
    private lateinit var yesterdayButton: Button
    private lateinit var allButton: Button
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var dateTimeSaveButton: Button
    private lateinit var swipeLayout: SwipeRefreshLayout
    private val fragmentBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Bip70Action.QUERY_ALL_TX_FROM_BITCOIN_COM_PAY == intent.action) {
                swipeLayout.isRefreshing = false
            }
        }
    }
    @Volatile
    private var ready = false
    private var startDateTime: Long = 0
    private var endDateTime: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        ready = true
        val rootView = inflater.inflate(resources.getLayout(R.layout.fragment_transaction), container, false)
        initListView(rootView)
        setToolbarAsBackButton()
        setToolbarTitle(R.string.menu_transactions)
        swipeLayout = rootView.findViewById(R.id.swipe_container)
        swipeLayout.setProgressViewEndTarget(false, (resources.displayMetrics.density * (72 + 20)).toInt())
        swipeLayout.setOnRefreshListener { loadTxFromDatabaseTask(true) }
        swipeLayout.setColorSchemeResources(
                R.color.bitcoindotcom_darker_green,
                R.color.bitcoindotcom_green,
                R.color.bitcoindotcom_darkest_green)
        val filter = IntentFilter()
        filter.addAction(Bip70Action.QUERY_ALL_TX_FROM_BITCOIN_COM_PAY)
        LocalBroadcastManager.getInstance(activity).registerReceiver(fragmentBroadcastReceiver, filter)
        return rootView
    }

    override fun onDestroyView() {
        ready = false
        super.onDestroyView()
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(fragmentBroadcastReceiver)
    }

    private fun initListView(rootView: View) {
        adapter = TransactionAdapter()
        listView = rootView.findViewById(R.id.txList)
        noTxHistoryLv = rootView.findViewById(R.id.no_tx_history_lv)
        dateFilterArea = rootView.findViewById(R.id.dateFilterArea)
        startDateButton = rootView.findViewById(R.id.startDateButton)
        endDateButton = rootView.findViewById(R.id.endDateButton)
        datePresetArea = rootView.findViewById(R.id.datePresetArea)
        todayButton = rootView.findViewById(R.id.todayButton)
        yesterdayButton = rootView.findViewById(R.id.yesterdayButton)
        allButton = rootView.findViewById(R.id.allButton)
        datePicker = rootView.findViewById(R.id.datePicker)
        timePicker = rootView.findViewById(R.id.timePicker)
        dateTimeSaveButton = rootView.findViewById(R.id.dateTimeSaveButton)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, _, id -> showTransactionMenu(id) }
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                listView.getChildAt(0)?.also {
                    swipeLayout.isEnabled = listView.firstVisiblePosition == 0 && it.top == 0
                }
            }
        })
        setTxListVisibility(adapter.count > 0)
        startDateButton.setOnClickListener {
            getDateTime(0, 0, 0, startDateTime, object : DateTimeCallback {
                override fun invoke(timeInMillis: Long) {
                    startDateTime = timeInMillis
                    startDateButton.text = DateUtil.instance.format(timeInMillis)
                    setSelectedDatePreset(null)
                    adapter.filter()
                }
            })
        }
        endDateButton.setOnClickListener {
            getDateTime(23, 59, 59, endDateTime, object : DateTimeCallback {
                override fun invoke(timeInMillis: Long) {
                    endDateTime = timeInMillis
                    endDateButton.text = DateUtil.instance.format(timeInMillis)
                    setSelectedDatePreset(null)
                    adapter.filter()
                }
            })
        }
        todayButton.setOnClickListener {
            val time = Calendar.getInstance() // today
            time.set(Calendar.HOUR_OF_DAY, 0)
            time.set(Calendar.MINUTE, 0)
            time.set(Calendar.SECOND, 0)
            val startTime = time.timeInMillis
            time.set(Calendar.HOUR_OF_DAY, 23)
            time.set(Calendar.MINUTE, 59)
            time.set(Calendar.SECOND, 59)
            val endTime = time.timeInMillis
            selectDatePresent(todayButton, startTime, endTime)
        }
        yesterdayButton.setOnClickListener {
            val time = Calendar.getInstance() // today
            time.add(Calendar.DAY_OF_MONTH, -1) // yesterday
            time.set(Calendar.HOUR_OF_DAY, 0)
            time.set(Calendar.MINUTE, 0)
            time.set(Calendar.SECOND, 0)
            val startTime = time.timeInMillis
            time.set(Calendar.HOUR_OF_DAY, 23)
            time.set(Calendar.MINUTE, 59)
            time.set(Calendar.SECOND, 59)
            val endTime = time.timeInMillis
            selectDatePresent(yesterdayButton, startTime, endTime)
        }
        allButton.setOnClickListener {
            selectDatePresent(allButton, 0L, 0L)
        }
    }

    private fun selectDatePresent(button: Button, newStartTime: Long, newEndTime: Long) {
        setSelectedDatePreset(button)

        startDateTime = newStartTime
        val startString = if (newStartTime == 0L) getString(R.string.empty_date_string) else DateUtil.instance.format(newStartTime)
        startDateButton.text = startString

        endDateTime = newEndTime
        val endString = if (newEndTime == 0L) getString(R.string.empty_date_string) else DateUtil.instance.format(newEndTime)
        endDateButton.text = endString

        adapter.filter()
    }

    private fun setSelectedDatePreset(selectedButton: Button?) {
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        val green = ContextCompat.getColor(requireContext(), R.color.bitcoindotcom_green)
        val black = Color.BLACK

        todayButton.setBackgroundColor(white)
        todayButton.setTextColor(black)
        yesterdayButton.setBackgroundColor(white)
        yesterdayButton.setTextColor(black)
        allButton.setBackgroundColor(white)
        allButton.setTextColor(black)

        if (selectedButton != null) {
            selectedButton.setBackgroundColor(green)
            selectedButton.setTextColor(white)
        }
    }

    private fun setTxListVisibility(enabled: Boolean) {
        val listVisibility = if (enabled) View.VISIBLE else View.GONE
        listView.visibility = listVisibility
        dateFilterArea.visibility = listVisibility
        noTxHistoryLv.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadTxFromDatabaseTask(false)
    }

    private fun showTransactionMenu(item: Long) {
        val paymentRecord = adapter.mListItems[item.toInt()].toPaymentRecord()
        val txId = paymentRecord.tx ?: ""
        val emojiLink = String(Character.toChars(0x1F517))
        val emojiReceipt = String(Character.toChars(0x1F9FE))
        val emojiClipboard = String(Character.toChars(0x1F4CB))
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("${paymentRecord.fiatAmount} - ${Date(paymentRecord.timeInSec*1000)}")
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setItems(arrayOf<CharSequence>(
                "$emojiLink ${getString(R.string.inspect_tx_link_view_transaction)}",
                "$emojiClipboard ${getString(R.string.inspect_tx_copy_transaction)}",
                "$emojiReceipt ${getString(R.string.inspect_tx_print_receipt)}")
        ) { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            when (which) {
                0 -> Analytics.tx_id_explorer_launched.send()
                1 -> Analytics.tx_id_copied.send()
            }
            when (which) {
                0 -> openExplorer(getString(R.string.url_explorer_bitcoin_com) + "/bch/tx/$txId")
                1 -> copyToClipboard(txId)
                2 -> printReceipt(paymentRecord)
            }
        }
        builder.create().show()
    }

    private fun printReceipt(paymentRecord: PaymentRecord) {
        val receiptHtml = ReceiptUtil.createReceiptHtml(activity, paymentRecord)
        ReceiptUtil.printReceipt(activity, receiptHtml)
    }

    private fun openExplorer(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Source Text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private interface DateTimeCallback {
        fun invoke(timeInMillis: Long)
    }

    private fun getDateTime(initialHour: Int, initialMinute: Int, seconds: Int, currentTime: Long, callback: DateTimeCallback) {
        val now = Calendar.getInstance()
        var defaultYear = now.get(Calendar.YEAR)
        var defaultMonth = now.get(Calendar.MONTH)
        var defaultDay = now.get(Calendar.DAY_OF_MONTH)
        var defaultHour = initialHour
        var defaultMinute = initialMinute
        if (currentTime != 0L) {
            val calendar = Calendar.getInstance()
            calendar.setTime(Date(currentTime))
            defaultYear = calendar.get(Calendar.YEAR)
            defaultMonth = calendar.get(Calendar.MONTH)
            defaultDay = calendar.get(Calendar.DAY_OF_MONTH)
            defaultHour = calendar.get(Calendar.HOUR)
            defaultMinute = calendar.get(Calendar.MINUTE)
        }

        datePicker.visibility = View.VISIBLE
        timePicker.visibility = View.GONE
        dateTimeSaveButton.visibility = View.VISIBLE

        datePicker.init(defaultYear, defaultMonth, defaultDay, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.hour = defaultHour
            timePicker.minute = defaultMinute
        }
        else {
            timePicker.currentHour = defaultHour
            timePicker.currentMinute = defaultMinute
        }

        dateTimeSaveButton.setOnClickListener {
            // switch from date to time
            timePicker.visibility = View.VISIBLE
            datePicker.visibility = View.GONE
            dateTimeSaveButton.setOnClickListener {
                val newTime = Calendar.getInstance()
                newTime.set(Calendar.YEAR, datePicker.year)
                newTime.set(Calendar.MONTH, datePicker.month)
                newTime.set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    newTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    newTime.set(Calendar.MINUTE, timePicker.minute)
                }
                else {
                    newTime.set(Calendar.HOUR_OF_DAY, timePicker.currentHour)
                    newTime.set(Calendar.MINUTE, timePicker.currentMinute)
                }
                newTime.set(Calendar.SECOND, seconds)
                timePicker.visibility = View.GONE
                dateTimeSaveButton.visibility = View.GONE
                callback.invoke(newTime.timeInMillis)
            }
        }
    }

    // view not yet created
    protected val isSafe: Boolean
        get() = ready && !this.isRemoving && !this.isDetached && this.isAdded && this.view != null

    private fun requestToDownloadAllTx() { // TODO use merchant server to query all TX
        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(Bip70Action.QUERY_ALL_TX_FROM_BITCOIN_COM_PAY))
    }

    private fun loadTxFromDatabaseTask(queryServer: Boolean) {
        if (!isSafe || !Settings.getPaymentTarget(activity).isValid) return
        viewLifecycleOwner.lifecycleScope.launch {
            swipeLayout.isRefreshing = true
            val txs: ArrayList<ContentValues>? = withContext(Dispatchers.IO) {
                try {
                    app.db.allPayments
                } catch (e: Exception) {
                    Analytics.error_db_read_tx.sendError(e)
                    Log.e(TAG, "getAllPayments", e)
                    null
                }
            }
            swipeLayout.isRefreshing = false
            if (!isSafe) return@launch
            txs?.also {
                adapter.reset(txs)
                if (txs.size != 0) {
                    setTxListVisibility(true)
                }
            }
            if (queryServer) {
                requestToDownloadAllTx()
            }
        }
    }

    private inner class TransactionAdapter internal constructor() : BaseAdapter() {
        val mListItems: MutableList<ContentValues> = ArrayList()
        val mFilteredItems: MutableList<ContentValues> = ArrayList()
        private val inflater: LayoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        fun reset(values: ArrayList<ContentValues>) {
            mListItems.clear()
            mListItems.addAll(values)

            filterByDateRange()
            notifyDataSetChanged()
        }

        fun filter() {
            filterByDateRange()
            notifyDataSetChanged()
        }

        private fun filterByDateRange() {
            mFilteredItems.clear()
            for (item in mListItems) {
                val timeInSec = item.toPaymentRecord().timeInSec
                val timeInMillis = TimeUnit.SECONDS.toMillis(timeInSec)
                val shouldHide = (startDateTime != 0L && timeInMillis < startDateTime) || (endDateTime != 0L && timeInMillis > endDateTime)
                if (!shouldHide) {
                    mFilteredItems.add(item)
                }
            }
        }

        override fun getCount(): Int {
            return mFilteredItems.size
        }

        override fun getItem(position: Int): String {
            return mFilteredItems[position].getAsString("iad")
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                    ?: inflater.inflate(R.layout.list_item_transaction, parent, false)
            try {
                val r = mFilteredItems[position].toPaymentRecord()
                setupView(view, r.bchAmount, r.fiatAmount, r.timeInSec, r.confirmations)
            } catch (e: Exception) {
                Analytics.error_rendering.sendError(e)
                Log.e(TAG, "getView", e)
            }
            return view
        }

        private fun setupView(view: View, bch: Long, fiat: String?, timeInSec: Long, confirmations: Int) {
            var confirmations = confirmations
            val date: String = DateUtil.instance.format(TimeUnit.SECONDS.toMillis(timeInSec))
            val ds = SpannableStringBuilder(date)
            val idx = date.indexOf("@")
            if (idx != -1) {
                ds.setSpan(StyleSpan(Typeface.NORMAL), 0, idx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                ds.setSpan(RelativeSizeSpan(0.75f), idx, date.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val tvDate = view.findViewById<TextView>(R.id.tv_date)
            tvDate.text = ds
            tvDate.alpha = 0.7f
            // display coin amount
            val amount = abs(bch)
            val coinSpan = SpannableStringBuilder("BCH")
            coinSpan.setSpan(RelativeSizeSpan(0.75.toFloat()), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val coinView = view.findViewById<TextView>(R.id.tv_amount_coin)
            val displayValue = MonetaryUtil.instance.getDisplayAmountWithFormatting(amount)
            coinView.text = "$displayValue $coinSpan"
            // display fiat amount
            val fiatView = view.findViewById<TextView>(R.id.tv_amount_fiat)
            fiatView.text = if (fiat != null && fiat.length > 1) {
                val fiatSpan = SpannableStringBuilder(fiat.subSequence(0, 1))
                fiatSpan.setSpan(RelativeSizeSpan(0.75.toFloat()), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                "$fiatSpan ${fiat.substring(1)}"
            } else {
                ""
            }
            val secs = System.currentTimeMillis() / 1000f
            if (secs - timeInSec > TimeUnit.HOURS.toSeconds(3)) {
                confirmations = 2
            }
            val ivStatus = view.findViewById<ImageView>(R.id.iv_status)
            ivStatus.setImageResource(getIcon(bch, confirmations))
            ivStatus.alpha = getAlpha(bch, confirmations)
        }

        private fun getIcon(bch: Long, confirmations: Int): Int {
            return when {
                confirmations < 0 -> R.drawable.ic_warning_black_18dp // under/over payment, legacy: can't happen anymore with BIP-70
                confirmations in 0..1 -> R.drawable.ic_done_white_24dp
                else -> R.drawable.ic_doublecheck_white_24dp // 2 or more confirmations
            }
        }

        private fun getAlpha(bch: Long, confirmations: Int): Float {
            return when {
                confirmations < 0 -> 1.0f // under/over payment, legacy: can't happen anymore with BIP-70
                confirmations in 0..1 -> 0f
                else -> 1.0f
            }
        }
    }

    override fun canFragmentBeDiscardedWhenInBackground(): Boolean {
        return true
    }

    companion object {
        private const val TAG = "BCR-TransactionsHistory"
    }
}