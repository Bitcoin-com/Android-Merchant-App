package com.bitcoin.merchant.app.screens.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.currency.CountryCurrency;
import com.bitcoin.merchant.app.currency.CurrencyExchange;
import com.bitcoin.merchant.app.screens.PaymentInputFragment;
import com.bitcoin.merchant.app.screens.SettingsActivity;
import com.bitcoin.merchant.app.util.PrefsUtil;

import java.util.List;

public class CurrencySelectionDialog {
    private final SettingsActivity ctx;

    public CurrencySelectionDialog(SettingsActivity ctx) {
        this.ctx = ctx;
    }

    private class ArrayAdapterWithIcon extends ArrayAdapter<CountryCurrency> {
        private List<CountryCurrency> cc;

        public ArrayAdapterWithIcon(Context context, List<CountryCurrency> items) {
            super(context, R.layout.select_currency_dialog_item, items);
            this.cc = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = view.findViewById(R.id.select_currency_text);
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(cc.get(position).image, 0, 0, 0);
            float dimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getContext().getResources().getDisplayMetrics());
            textView.setCompoundDrawablePadding((int) dimension);
            return view;
        }
    }

    public boolean show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        final List<CountryCurrency> currencies = CurrencyExchange.getInstance(ctx).getCountryCurrencies();
        ListAdapter adapter = new ArrayAdapterWithIcon(ctx, currencies);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CountryCurrency cc = currencies.get(which);
                String locale = cc.countryLocales.getFirstSupportedLocale();
                if (locale == null) {
                    Toast.makeText(ctx, "Not supported", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                save(cc, locale);
            }
        });
        builder.setCancelable(true);
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
        return true;
    }

    private void save(CountryCurrency cc, String locale) {
        PrefsUtil prefsUtil = PrefsUtil.getInstance(ctx);
        prefsUtil.setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, cc.currencyRate.code);
        prefsUtil.setValue(PrefsUtil.MERCHANT_KEY_COUNTRY, cc.countryLocales.country);
        prefsUtil.setValue(PrefsUtil.MERCHANT_KEY_LOCALE, locale);
        Intent intent = new Intent(PaymentInputFragment.ACTION_INTENT_RESET_AMOUNT);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        ctx.setCurrencySummary(cc);
    }
}
