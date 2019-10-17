package com.bitcoin.merchant.app.util;

import android.content.Context;
import android.os.Build;

public class OSUtil {
    private static OSUtil instance = null;
    private static Context context = null;

    private OSUtil() {
    }

    public static OSUtil getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new OSUtil();
        }
        return instance;
    }

    public static String getSha256(String userEnteredPIN) {
        // TODO fix this with simple transformation
        return userEnteredPIN;
        // return Sha256Hash.wrap(userEnteredPIN.getBytes(Charset.forName("UTF8"))).toString();
    }

    public String getFootprint() {
        String strFootprint = Build.MANUFACTURER + Build.BRAND + Build.MODEL + Build.DEVICE + Build.PRODUCT + Build.SERIAL;
        return getSha256(strFootprint);
    }
}
