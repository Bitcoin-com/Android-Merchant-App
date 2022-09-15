package com.bitcoin.merchant.app.util

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.database.PaymentRecord
import com.bitcoin.merchant.app.network.PaymentReceived
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceStatus

object ReceiptUtil {
    fun createReceiptHtml(context: Context, paymentRecord: PaymentRecord): String {
        val txId = paymentRecord.tx.toString()
        val satoshiAmount = paymentRecord.bchAmount
        val fiatAmount = paymentRecord.fiatAmount.toString()
        val time = paymentRecord.timeInSec * 1000

        return createReceiptHtml(context, txId, satoshiAmount, fiatAmount, time)
    }

    fun createReceiptHtml(context: Context, invoiceStatus: InvoiceStatus): String {
        val txId = invoiceStatus.txId.toString()
        val satoshiAmount = invoiceStatus.totalAmountInSatoshi
        val fiatAmount = invoiceStatus.fiatSymbol + AmountUtil(context).formatFiat(invoiceStatus.fiatTotal)
        val time = invoiceStatus.time.time

        return createReceiptHtml(context, txId, satoshiAmount, fiatAmount, time)
    }

    fun createReceiptHtml(context: Context, paymentReceived: PaymentReceived): String {
        val txId = paymentReceived.txHash
        val satoshiAmount = paymentReceived.bchReceived
        val fiatAmount = paymentReceived.fiatExpected
        val time = paymentReceived.timeInSec * 1000

        return createReceiptHtml(context, txId, satoshiAmount, fiatAmount, time)
    }

    private fun createReceiptHtml(context: Context, txId: String, satoshiAmount: Long, fiatAmount: String, timeInMillis: Long) : String {
        val merchantName = Settings.getMerchantName(context)
        val timeString = DateUtil.instance.formatHistorical(timeInMillis)
        val bchAmount = AmountUtil(context).satsToBch(satoshiAmount)

        val builder = StringBuilder()
        builder.append("<html>")
        builder.append("<head><style>\n")
        builder.append("body { margin-top: 2em; font-size: 24pt; }\n")
        builder.append("#merchant-name { font-size: 36pt; }\n")
        builder.append("#purchase-time { font-size: 24pt; color: gray; }\n")
        builder.append("table { margin-top: 3em; width: 100%; font-size: 24pt; }\n")
        builder.append("th, td { padding: 10px; width: calc(50% - 20px); word-wrap: break-word; }\n")
        builder.append("th { text-align: right; }\n")
        builder.append("#transaction-area { margin-top: 2em; font-size: 20pt; }\n")
        builder.append("#transaction-id { display: inline-block; overflow-wrap: break-word; color: gray; max-width: 50%; }\n")
        builder.append("</style></head>")
        builder.append("<body>")
        builder.append("<div style=\"text-align: center;\">")

        // header
        builder.append("<div id=\"merchant-name\">")
        builder.append(merchantName)
        builder.append("</div>")
        builder.append("<div id=\"purchase-time\">")
        builder.append(timeString)
        builder.append("</div>")

        builder.append("<table>")
        builder.append("<tr>")
        builder.append("<th>")
        builder.append("BCH Total:")
        builder.append("</th>")
        builder.append("<td>")
        builder.append(bchAmount)
        builder.append("</td>")
        builder.append("</tr>")

        builder.append("<tr>")
        builder.append("<th>")
        builder.append("Fiat Equivalent:")
        builder.append("</th>")
        builder.append("<td>")
        builder.append(fiatAmount)
        builder.append("</td>")
        builder.append("</tr>")
        builder.append("</table>")

        builder.append("<div id=\"transaction-area\">")
        builder.append("<strong>Transaction ID<strong><br>")
        builder.append("<span id=\"transaction-id\">")
        builder.append(txId)
        builder.append("</span>")
        builder.append("</div>")

        builder.append("</div>")
        builder.append("</body></html>")
        return builder.toString()
    }

    fun printReceipt(activity: Activity, receiptHtml: String, webView: WebView? = null) {
        // Create a WebView object specifically for printing
        val targetWebView = webView ?: WebView(activity)

        targetWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                createReceiptPrintJob(activity, view)
            }
        }

        targetWebView.loadDataWithBaseURL(null, receiptHtml, "text/HTML", "UTF-8", null)
    }

    private fun createReceiptPrintJob(activity: Activity, webView: WebView) {
        // Get a PrintManager instance
        (activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager)?.let { printManager ->
            val jobName = "${activity.getString(R.string.app_name)} Document"

            // Get a print adapter instance
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            // Create a print job with name and adapter instance
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder().build()
            )
        }
    }
}