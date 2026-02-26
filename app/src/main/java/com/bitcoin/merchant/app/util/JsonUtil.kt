package com.bitcoin.merchant.app.util

import android.content.Context
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper
import java.io.BufferedReader
import java.io.InputStreamReader

class JsonUtil {
    companion object {
        @JvmStatic
        fun <T> readFromJsonFile(ctx: Context, fileName: String, classOfT: Class<T>): T {
            return GsonHelper.gson.fromJson(readFromfile(fileName, ctx), classOfT)
        }

        private fun readFromfile(fileName: String, context: Context): String {
            BufferedReader(InputStreamReader(context.resources.assets.open(fileName))).use {
                return it.readText()
            }
        }
    }
}
