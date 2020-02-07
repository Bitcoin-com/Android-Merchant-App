package org.bitcoindotcom.bchprocessor.bip70.model

import com.google.gson.annotations.SerializedName
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper
import java.util.*

data class InvoiceRequest @JvmOverloads constructor(
        @SerializedName("fiatAmount") var amount: String,
        @SerializedName("fiat") var fiat: String,
        @SerializedName("webhook") var webhook: String = "http://127.0.0.1/unused/webhook",
        @SerializedName("memo") var memo: String = UUID.randomUUID().toString(),
        @SerializedName("apiKey") var apiKey: String? = null,
        @SerializedName("address") var address: String? = null) {
    companion object {
        @JvmStatic
        fun fromJson(message: String): InvoiceRequest {
            return GsonHelper.gson.fromJson(message, InvoiceRequest::class.java)
        }
    }
}