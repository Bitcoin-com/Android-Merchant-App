package org.bitcoindotcom.bchprocessor.bip70.model

import com.google.gson.annotations.SerializedName
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper.gson
import java.util.*
import kotlin.collections.ArrayList

data class InvoiceStatus @JvmOverloads constructor(
        @SerializedName("paymentUrl") var paymentUrl: String = "",
        @SerializedName("paymentId") var paymentId: String = "",
        @SerializedName("paymentAsset") var paymentAsset: String? = "",
        @SerializedName("memo") var memo: String? = null,
        @SerializedName("time") var time: Date = Date(),
        @SerializedName("expires") var expires: Date = Date(),
        @SerializedName("status") var status: String = Status.expired.name,
        @SerializedName("network") var network: String? = "",
        @SerializedName("currency") var currency: String? = "",
        @SerializedName("fiatSymbol") var fiatSymbol: String? = "",
        @SerializedName("fiatRate") var fiatRate: Double = 0.0,
        @SerializedName("fiatTotal") var fiatTotal: Double = 0.0,
        @SerializedName("txId") var txId: String? = null,
        @SerializedName("merchantId") var merchantId: String? = null,
        @SerializedName("outputs") var outputs: List<InvoiceStatusOutput> = ArrayList(0)) {

    val isInitialized: Boolean
        get() = paymentUrl.isNotEmpty() && paymentId.isNotEmpty() && fiatTotal != 0.0 && outputs.isNotEmpty()
    val firstAddress: String?
        get() = if (outputs.isNotEmpty()) outputs[0].address else null

    val totalBchAmount: Long
        get() {
            var amount: Long = 0
            for (output in outputs) {
                amount += output.amount
            }
            return amount
        }

    val isPaid: Boolean
        get() = Status.paid.name == status

    val isOpen: Boolean
        get() = Status.open.name == status

    val isExpired: Boolean
        get() = Status.expired.name == status

    val walletUri: String
        get() = "bitcoincash:?r=$paymentUrl"

    internal enum class Status {
        open, paid, expired
    }

    companion object {
        fun fromJson(message: String?): InvoiceStatus {
            return gson.fromJson(message, InvoiceStatus::class.java)
        }
    }
}