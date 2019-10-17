package com.bitcoin.merchant.app.util;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import com.bitcoin.merchant.app.currency.CurrencyDetector;
import com.github.kiulian.converter.AddressConverter;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import info.blockchain.wallet.util.FormatsUtil;

public class AppUtil {
    public static final Gson GSON = new Gson();
    public static final String PACKAGE_BITCOIN_DOT_COM_WALLET = "com.bitcoin.mwallet";
    public static final String DEFAULT_CURRENCY_FIAT = "USD";
    private static final boolean MISSING_WALLET_SIMULATED = false;
    private static Context context = null;
    private static AppUtil instance = null;
    private volatile WalletUtil walletUtil;

    private AppUtil() {
    }

    public static AppUtil getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new AppUtil();
        }
        return instance;
    }

    /**
     * For performance reasons, we cache the wallet (reported in May 2019 on Lenovo Tab E8)
     */
    public synchronized WalletUtil getWallet() throws Exception {
        String xPub = AppUtil.getReceivingAddress(context);
        if (walletUtil == null || !walletUtil.isSameXPub(xPub)) {
            walletUtil = new WalletUtil(xPub, context);
        }
        return walletUtil;
    }

    public static boolean isValidAddress(String address) {
        return (address != null && address.length() > 0) && (FormatsUtil.getInstance().isValidXpub(address) || AddressUtil.isValidCashAddr(address) || AddressUtil.isValidLegacy(address));
    }

    public static String getCurrency(Context context) {
        String currency = PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "");
        if (currency.length() == 0) {
            // auto-detect currency
            currency = CurrencyDetector.findCurrencyFromLocale(context);
            if (currency.length() == 0) {
                currency = DEFAULT_CURRENCY_FIAT;
            }
            // save to avoid further auto-detection
            PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, currency);
        }
        return currency;
    }

    public static String getCountry(Context context) {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_COUNTRY, null);
    }

    public static String getLocale(Context context) {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_LOCALE, null);
    }

    public static <T> T readFromJsonFile(Context ctx, String fileName, Class<T> classOfT) {
        return GSON.fromJson(readFromfile(fileName, ctx), classOfT);
    }

    public static String readFromfile(String fileName, Context context) {
        StringBuilder b = new StringBuilder();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader(context.getResources().getAssets().open(fileName)));
            String line = "";
            while ((line = input.readLine()) != null) {
                b.append(line);
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (Exception e2) {
                e2.getMessage();
            }
        }
        return b.toString();
    }

    public static boolean isReceivingAddressAvailable(Context ctx) {
        return getReceivingAddress(ctx).length() != 0;
    }

    /**
     * Gets pubKey or extendedPubKey
     */
    public static String getReceivingAddress(Context context) {
        return PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");
    }

    public static void setReceivingAddress(Context context, String receiver) {
        /*
        We keep the storage format as legacy for compatibility purposes.
         */
        if (AddressUtil.isValidCashAddr(receiver)) {
            try {
                receiver = AddressConverter.toLegacyAddress(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, receiver);
    }

    public static String convertToBitcoinCash(String address) {
        FormatsUtil f = FormatsUtil.getInstance();
        if (address != null && address.length() > 0 &&
                (!f.isValidXpub(address) && AddressUtil.isValidLegacy(address))) {
            try {
                address = AddressUtil.toCashAddress(address);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*
        If it's not a valid legacy address from the if statement above, just return as the text we are sending.
         */
        return address;
    }

    public boolean isValidXPub() {
        String receiver = getReceivingAddress(context);
        return FormatsUtil.getInstance().isValidXpub(receiver);
    }

    public static void setStatusBarColor(Activity activity, int color) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(activity.getResources().getColor(color));
    }

    public boolean hasValidReceiver() {
        String receiver = getReceivingAddress(context);
        return AddressUtil.isValidLegacy(receiver) || FormatsUtil.getInstance().isValidXpub(receiver);
    }
}
