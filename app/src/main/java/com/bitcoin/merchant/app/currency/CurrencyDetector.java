package com.bitcoin.merchant.app.currency;

import android.content.Context;
import android.util.Log;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.util.AppUtil;
import com.crashlytics.android.Crashlytics;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CurrencyDetector {
    private static final String TAG = MainActivity.TAG;

    /**
     * It will return an empty string when not found or when currency is unknown.
     */
    public static String findCurrencyFromLocale(Context context) {
        Locale locale = Locale.getDefault();
        String currencyCode = "";
        String countryCode = "";
        try {
            countryCode = locale.getCountry();
            Log.i(TAG, "Currency Locale.country: " + countryCode);
            Currency currency = Currency.getInstance(locale);
            currencyCode = currency.getCurrencyCode();
            Log.i(TAG, "Currency Code: " + currencyCode + " for locale: " + locale.getDisplayName());
            Log.i(TAG, "Currency Symbol: " + currency.getSymbol());
            Log.i(TAG, "Currency Default Fraction Digits: " + currency.getDefaultFractionDigits());
        } catch (Exception e) {
            Log.e(TAG, "Currency", e);
            Crashlytics.logException(e);
            // check if currency can be determined from the country code
            if ((countryCode != null) && (countryCode.length() >= 2)) {
                Map<String, String> countryToCurrency = AppUtil.readFromJsonFile(context, "country_to_currency.json", HashMap.class);
                currencyCode = countryToCurrency.get(countryCode.trim().substring(0, 2).toUpperCase());
            }
        }
        return CurrencyExchange.getInstance(context).isTickerSupported(currencyCode) ? currencyCode : "";
    }
}
