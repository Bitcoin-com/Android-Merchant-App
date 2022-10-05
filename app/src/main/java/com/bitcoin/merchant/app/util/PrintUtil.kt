package com.bitcoin.merchant.app.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bitcoin.merchant.app.R
import com.bitcoin.merchant.app.database.PaymentRecord
import com.bitcoin.merchant.app.database.toPaymentRecord
import com.bitcoin.merchant.app.network.PaymentReceived
import org.bitcoindotcom.bchprocessor.bip70.model.InvoiceStatus
import java.util.concurrent.TimeUnit

object PrintUtil {
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

    fun createReportHtml(context: Context, startTime: Long, endTime: Long, reportTransactions: MutableList<ContentValues>): String {
        val merchantName = Settings.getMerchantName(context)
        val reportTime = DateUtil.instance.formatHistorical(System.currentTimeMillis())
        var dateRangeString = "All Historical Sales"
        if (startTime != 0L || endTime != 0L) {
            val endDateString = if (endTime == 0L) reportTime else DateUtil.instance.formatHistorical(endTime)
            if (startTime == 0L) {
                dateRangeString = "All transactions before $endDateString"
            }
            else {
                val startDateString = DateUtil.instance.formatHistorical(startTime)
                dateRangeString = "$startDateString to $endDateString"
            }
        }

        val builder = StringBuilder()
        builder.append("<html>")
        builder.append("<head><style>\n")
        builder.append("body { margin-top: 2em; font-size: 24pt; }\n")
        builder.append("#merchant-name { font-size: 36pt; }\n")
        builder.append("#report-time { font-size: 24pt; color: gray; }\n")
        builder.append("#report-range { font-size: 24pt; margin-top: 2em; }\n")
        builder.append("#fiat-warning { font-size: 20pt; margin: auto; width: 75%; }\n")
        builder.append("table { width: 100%; font-size: 24pt; margin-top: 1em; margin-bottom: 1em; }\n")
        builder.append("th, td { padding: 10px; overflow-wrap: break-word; }\n")
        builder.append("th { text-align: left; }\n")
        builder.append("</style></head>")
        builder.append("<body>")
        builder.append("<div style=\"text-align: center;\">")

        // header
        builder.append("<div id=\"merchant-name\">")
        builder.append(merchantName).append(" Sales Report")
        builder.append("</div>")
        builder.append("<div id=\"report-time\">")
        builder.append("Generated on ").append(reportTime)
        builder.append("</div>")

        builder.append("<div id=\"transaction-area\">")
        builder.append("<div id=\"report-range\">").append(dateRangeString).append("</div>")
        builder.append("<table>")
        builder.append("<tr><th>Sale Time</th><th>BCH Amount</th><th>Fiat Equiv.</th></tr>")
        var totalBch = 0L
        var totalFiat = 0.0
        for (transaction in reportTransactions) {
            val paymentRecord = transaction.toPaymentRecord()
            val txTimeInMillis = TimeUnit.SECONDS.toMillis(paymentRecord.timeInSec)
            builder.append("<tr>")
            builder.append("<td>")
            builder.append(DateUtil.instance.formatHistorical(txTimeInMillis))
            builder.append("</td>")
            builder.append("<td>")
            builder.append(AmountUtil(context).satsToBch(paymentRecord.bchAmount))
            builder.append("</td>")
            builder.append("<td>")
            builder.append(paymentRecord.fiatAmount)
            builder.append("</td>")
            builder.append("</tr>")
            totalBch += paymentRecord.bchAmount
            val fiatValueOnly = paymentRecord.fiatAmount?.replace(Regex("[^0-9,.]"), "")
            totalFiat += fiatValueOnly?.toDoubleOrNull() ?: 0.0
        }
        val totalBchString = AmountUtil(context).satsToBch(totalBch)
        val totalFiatString = AmountUtil(context).formatFiat(totalFiat)
        builder.append("<tr><th>Total</th><th>$totalBchString</th><th>$totalFiatString*</th></tr>")
        builder.append("</table>")
        builder.append("<p id=\"fiat-warning\">* fiat total may be inaccurate if multiple currencies are present</p>")
        builder.append("</div>")

        builder.append("</div>")
        builder.append("</body></html>")
        return builder.toString()
    }

    fun printHtml(activity: Activity, receiptHtml: String, webView: WebView? = null) {
        // Create a WebView object specifically for printing
        val targetWebView = webView ?: WebView(activity)

        targetWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                createPrintJob(activity, view)
            }
        }

        targetWebView.loadDataWithBaseURL(null, receiptHtml, "text/HTML", "UTF-8", null)
    }

    private fun createPrintJob(activity: Activity, webView: WebView) {
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