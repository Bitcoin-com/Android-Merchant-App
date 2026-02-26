package com.bitcoin.merchant.app.currency;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bitcoin.merchant.app.BuildConfig;
import com.bitcoin.merchant.app.util.JsonUtil;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class CurrencyExchange {
    public static final int MINIMUM_INTERVAL_BETWEEN_UPDATE_IN_MS = 3 * 60 * 1000;
    public static final int RATE_WARNING_THRESHOLD_IN_MS = 120 * 60 * 1000;
    public static final String TAG = "CurrencyExchange";
    private final Handler threadHandler = new Handler();
    private static CurrencyExchange instance;
    private final Context context;
    private final Map<String, ExchangeRate> tickerToRate = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, String> tickerToSymbol;
    private volatile long lastUpdate;

    private CurrencyExchange(Context context) {
        this.context = context;
        tickerToSymbol = JsonUtil.readFromJsonFile(context, "currency_symbols.json", TreeMap.class);
        ExchangeRatesJson bchRates = JsonUtil.readFromJsonFile(context, "example_rates.json", ExchangeRatesJson.class);
        tickerToRate.putAll(ExchangeRate.convertToMap(bchRates.rates, tickerToSymbol));
        loadFromStore();
    }

    public static synchronized CurrencyExchange getInstance(Context ctx) {
        if (instance == null) {
            instance = new CurrencyExchange(ctx);
        }
        instance.requestUpdatedExchangeRates();
        return instance;
    }

    public static <T> T getUrlAsJson(String url, Class<T> c) {
        try {
            InputStream i = new URL(url).openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(i));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append('\n');
            }
            in.close();
            return new Gson().fromJson(response.toString(), c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void requestUpdatedExchangeRates() {
        if (isUpToDate()) return;
        Thread t = new Thread(this::checkCurrencyUpdate);
        t.setDaemon(true);
        t.start();
    }

    public void forceExchangeRateUpdates() {
        checkCurrencyUpdate();
    }

    private boolean isUpToDate() {
        long now = System.currentTimeMillis();
        return (now - lastUpdate) < MINIMUM_INTERVAL_BETWEEN_UPDATE_IN_MS;
    }

    public boolean isSeverelyOutOfDate() {
        long now = System.currentTimeMillis();
        return (now - lastUpdate) >= RATE_WARNING_THRESHOLD_IN_MS;
    }

    private void checkCurrencyUpdate() {
        if (isUpToDate()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    ExchangeRatesJson rates = getUrlAsJson(BuildConfig.PRICES_URL, ExchangeRatesJson.class);
                    threadHandler.post(() -> {
                        tickerToRate.putAll(ExchangeRate.convertToMap(rates.rates, tickerToSymbol));
                        lastUpdate = System.currentTimeMillis();
                        saveToStore();
                        Log.i("CurrencyExchange", "rates updated 1 BCH=$" + tickerToRate.get("USD").rate);
                    });
                } catch (Exception e) {
                    Log.e(TAG, e.toString(), e);
                }
            }
        }.start();
    }

    private ArrayList<String> getTickers() {
        return new ArrayList<>(tickerToRate.keySet());
    }

    private void loadFromStore() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long defaultPrice = Double.doubleToLongBits(0.0);
        for (String ticker : getTickers()) {
            String name = prefs.getString(ticker + "-NAME", null);
            double price = Double.longBitsToDouble(prefs.getLong(ticker, defaultPrice));
            tickerToRate.put(ticker, new ExchangeRate(ticker, price, tickerToSymbol.get(ticker), name));
        }
    }

    private void saveToStore() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        for (String ticker : getTickers()) {
            ExchangeRate er = tickerToRate.get(ticker);
            editor.putLong(ticker, Double.doubleToRawLongBits(er.rate));
            editor.putString(ticker + "-NAME", er.name);
        }
        editor.commit();
    }

    public boolean isTickerSupported(String ticker) {
        return getCurrencyRate(ticker) != null;
    }

    public Double getCurrencyPrice(String ticker) {
        ExchangeRate rate = getCurrencyRate(ticker);
        Double price = (rate == null) ? null : rate.rate;
        return (price == null) ? 0 : price;
    }

    public ExchangeRate getCurrencyRate(String ticker) {
        return (ticker != null) && (ticker.length() > 0) ? tickerToRate.get(ticker) : null;
    }
}
