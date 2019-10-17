package com.bitcoin.merchant.app.util;

import android.content.Context;
import android.util.Log;

import com.bitcoin.merchant.app.currency.CountryCurrency;
import com.bitcoin.merchant.app.currency.CurrencyExchange;
import com.bitcoin.merchant.app.screens.PaymentInputFragment;

import java.text.NumberFormat;
import java.util.Currency;

public class AmountUtil {
    public static final String TAG = "AmountUtil";
    private final Context context;
    private final CurrencyExchange exchange;

    public AmountUtil(Context context) {
        this.context = context;
        this.exchange = CurrencyExchange.getInstance(context);
    }

    public String getSymbolOrCurrencyCode(String currency) {
        String currencySymbol = exchange.getCurrencySymbol(currency);
        return currencySymbol != null ? currencySymbol : currency;
    }

    public String formatFiat(double amountFiat) {
        String currency = AppUtil.getCurrency(context);
        String country = AppUtil.getCountry(context);
        String locale = AppUtil.getLocale(context);
        CountryCurrency cc = CurrencyExchange.getInstance(context).getCountryCurrency(currency, country, locale);
        String currencySymbol = getSymbolOrCurrencyCode(currency);
        String fiat = null;
        try {
            if (cc != null) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance(cc.countryLocales.getLocale());
                Currency instance = Currency.getInstance(currency);
                formatter.setCurrency(instance);
                formatter.setMaximumFractionDigits(instance.getDefaultFractionDigits());
                fiat = formatter.format(amountFiat);
            }
        } catch (Exception e) {
            Log.d(TAG, "Locale not supported for " + currency + " failed to format to fiat: " + amountFiat);
        }
        if (fiat != null) {
            // replace Currency Sign
            fiat = fiat.replace("\u00a4", currencySymbol);
        } else {
            fiat = currencySymbol + " " + MonetaryUtil.getInstance().getFiatDecimalFormat().format(amountFiat);
        }
        return fiat;
    }

    public String formatBch(double amountBch) {
        return MonetaryUtil.getInstance().getBchDecimalFormat().format(amountBch) + " " + PaymentInputFragment.DEFAULT_CURRENCY_BCH;
    }
}
