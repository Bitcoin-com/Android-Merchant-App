package com.bitcoin.merchant.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import info.blockchain.wallet.util.PersistantPrefs;

public class PrefsUtil implements PersistantPrefs {
    public static final String MERCHANT_KEY_PIN = "pin";
    public static final String MERCHANT_KEY_CURRENCY = "currency";
    public static final String MERCHANT_KEY_COUNTRY = "country";
    public static final String MERCHANT_KEY_LOCALE = "locale";
    public static final String MERCHANT_KEY_MERCHANT_NAME = "receiving_name";
    public static final String MERCHANT_KEY_MERCHANT_RECEIVER = "receiving_address";
    public static final String MERCHANT_KEY_SCANNED_ALL_MISSING_FUNDS = "scanned_for_missing_funds";
    public static final String MERCHANT_KEY_XPUB_INDEX = "xpub_index";
    // unused public static final String MERCHANT_KEY_PUSH_NOTIFS = "push_notifications";
    public static final String MERCHANT_KEY_ACCOUNT_INDEX = "account_idx";
    private static Context context = null;
    private static PrefsUtil instance = null;

    private PrefsUtil() {
    }

    public static PrefsUtil getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new PrefsUtil();
        }
        return instance;
    }

    public String getValue(String name, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
    }

    public boolean setValue(String name, String value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(name, (value == null || value.length() < 1) ? "" : value);
        return editor.commit();
    }

    public int getValue(String name, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(name, 0);
    }

    public boolean setValue(String name, int value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        return editor.commit();
    }

    public boolean setValue(String name, long value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    public long getValue(String name, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long result;
        try {
            result = prefs.getLong(name, 0L);
        } catch (Exception e) {
            result = (long) prefs.getInt(name, 0);
        }
        return result;
    }

    public boolean getValue(String name, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(name, value);
    }

    public boolean setValue(String name, boolean value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    public boolean has(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(name);
    }

    public boolean removeValue(String name) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(name);
        return editor.commit();
    }

    public boolean clear() {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.clear();
        return editor.commit();
    }
}
