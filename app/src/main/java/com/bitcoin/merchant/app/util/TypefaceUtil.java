package com.bitcoin.merchant.app.util;

import android.content.Context;
import android.graphics.Typeface;

public class TypefaceUtil {
    private static Typeface btc_font = null;
    private static TypefaceUtil instance = null;

    private TypefaceUtil() {
    }

    public static TypefaceUtil getInstance(Context ctx, String ttfFilePath) {
        if (instance == null) {
            instance = new TypefaceUtil();
            btc_font = Typeface.createFromAsset(ctx.getAssets(), ttfFilePath);
        }
        return instance;
    }

    public Typeface getTypeface() {
        return btc_font;
    }
}
