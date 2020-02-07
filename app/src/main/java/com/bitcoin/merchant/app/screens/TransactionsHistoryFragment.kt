package com.bitcoin.merchant.app.screens

import android.app.AlertDialog
import android.content.*
import android.graphics.Typeface
import android.net.Uri
import android.os.AsyncTask
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.database.PaymentRecord
import com.bitcoin.merchant.app.screens.features.ToolbarAwareFragment
import com.bitcoin.merchant.app.util.DateUtil
import com.bitcoin.merchant.app.util.MonetaryUtil
import com.bitcoin.merchant.app.util.Settings
import com.crashlytics.android.Crashlytics
import org.bitcoindotcom.bchprocessor.bip70.model.Bip70Action
import java.util.*
import java.util.concurrent.TimeUnit

class TransactionsHistoryFragment : ToolbarAwareFragment() {
    private lateinit var adapter: TransactionAdapter
    private lateinit var listView: ListView
    private lateinit var noTxHistoryLv: LinearLayout
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        ready = true
        val rootView = inflater.inflate(resources.getLayout(R.layout.fragment_transaction), container, false)
        initListView(rootView)
        setToolbarAsBackButton()
        setToolbarTitle(R.string.menu_transactions)
        swipeLayout = rootView.findViewById(R.id.swipe_container)
        swipeLayout.setProgressViewEndTarget(false, (resources.displayMetrics.density * (72 + 20)).toInt())
        swipeLayout.setOnRefreshListener(OnRefreshListener { LoadTxFromDatabaseTask(true).execute() })
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
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, _, id -> showTransactionMenu(id) }
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (listView.getChildAt(0) != null) {
                    swipeLayout.isEnabled = listView.firstVisiblePosition == 0 && listView.getChildAt(0).top == 0
                }
            }
        })
        setTxListVisibility(adapter.count > 0)
    }

    private fun setTxListVisibility(enabled: Boolean) {
        listView.visibility = if (enabled) View.VISIBLE else View.GONE
        noTxHistoryLv.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        LoadTxFromDatabaseTask(false).execute()
    }

    private fun showTransactionMenu(item: Long) {
        val tx = adapter.mListItems[item.toInt()]
        val builder = AlertDialog.Builder(activity)
        val txId = tx.getAsString("tx")
        val address = tx.getAsString("iad")
        builder.setTitle(txId)
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setItems(arrayOf<CharSequence>(
                "View transaction",
                "View all TX with this address",
                "Copy transaction",
                "Copy address")
        ) { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            when (which) {
                0 -> openExplorer(activity.getString(R.string.url_explorer_bitcoin_com) + "/bch/tx/$txId")
                1 -> openExplorer(activity.getString(R.string.url_explorer_bitcoin_com) + "/bch/address/$address")
                2 -> copyToClipboard(txId)
                3 -> copyToClipboard(address)
            }
        }
        builder.create().show()
    }

    private fun openExplorer(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Source Text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    // view not yet created
    protected val isSafe: Boolean
        protected get() = ready && !this.isRemoving && !this.isDetached && this.isAdded && this.view != null

    private fun findAllPotentialMissingTx() { // TODO use merchant server to query all TX
        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(Bip70Action.QUERY_ALL_TX_FROM_BITCOIN_COM_PAY))
    }

    private inner class LoadTxFromDatabaseTask(private val queryServer: Boolean)
        : AsyncTask<Void?, Void?, ArrayList<ContentValues>?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            swipeLayout.isRefreshing = true
        }

        override fun doInBackground(vararg params: Void?): ArrayList<ContentValues>? {
            if (!isSafe) return null
            val address = Settings.getPaymentTarget(activity)
            if (address.isValid) {
                try {
                    return app.db.allPayments
                } catch (e: Exception) {
                    Log.e(TAG, "getAllPayments", e)
                    Crashlytics.logException(e)
                }
            }
            return null
        }

        override fun onPostExecute(result: ArrayList<ContentValues>?) {
            super.onPostExecute(result)
            if (!isSafe) return
            if (result != null) {
                adapter.reset(result)
                if (result.size != 0) {
                    setTxListVisibility(true)
                }
            }
            if (queryServer) {
                findAllPotentialMissingTx()
            } else {
                if (result != null && result.size != 0) {
                    setTxListVisibility(true)
                }
                swipeLayout.isRefreshing = false
            }
        }
    }

    private inner class TransactionAdapter internal constructor() : BaseAdapter() {
        val mListItems: MutableList<ContentValues> = ArrayList()
        private val inflater: LayoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        fun reset(values: ArrayList<ContentValues>) {
            mListItems.clear()
            mListItems.addAll(values)
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return mListItems.size
        }

        override fun getItem(position: Int): String {
            return mListItems[position].getAsString("iad")
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                    ?: inflater.inflate(R.layout.list_item_transaction, parent, false)
            try {
                val r = PaymentRecord(mListItems[position])
                setupView(view, r.bchAmount, r.fiatAmount, r.timeInSec, r.confirmations)
            } catch (e: Exception) {
                Log.e(TAG, "getView", e)
            }
            return view
        }

        private fun setupView(view: View, bch: Long, fiat: String?, timeInSec: Long, confirmations: Int) {
            var confirmations = confirmations
            val date: String = DateUtil.instance.format(timeInSec)
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
            val amount = Math.abs(bch)
            val coinSpan = SpannableStringBuilder("BCH")
            coinSpan.setSpan(RelativeSizeSpan(0.75.toFloat()), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val coinView = view.findViewById<TextView>(R.id.tv_amount_coin)
            val displayValue: String = MonetaryUtil.instance.getDisplayAmountWithFormatting(amount)
            coinView.text = "$displayValue $coinSpan"
            // display fiat amount
            val fiatView = view.findViewById<TextView>(R.id.tv_amount_fiat)
            if (fiat != null && fiat.length > 1) {
                val fiatSpan = SpannableStringBuilder(fiat.subSequence(0, 1))
                fiatSpan.setSpan(RelativeSizeSpan(0.75.toFloat()), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                fiatView.text = fiatSpan.toString() + " " + fiat.substring(1)
            } else {
                fiatView.text = ""
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
            return if (bch < 0L) { // under/over payment, legacy: can't happen anymore with BIP-70
                R.drawable.ic_warning_black_18dp
            } else if (confirmations <= 0) {
                R.drawable.ic_done_white_24dp
            } else if (confirmations == 1) {
                R.drawable.ic_done_white_24dp
            } else { // 2 or more confirmations
                R.drawable.ic_doublecheck_white_24dp
            }
        }

        private fun getAlpha(bch: Long, confirmations: Int): Float {
            return if (bch < 0L) { // under/over payment, legacy: can't happen anymore with BIP-70
                1.0f
            } else if (confirmations <= 0) {
                0f
            } else {
                1.0f
            }
        }
    }

    override fun canFragmentBeDiscardedWhenInBackground(): Boolean {
        return true
    }

    companion object {
        private const val TAG = "TransactionsHistory"
    }
}