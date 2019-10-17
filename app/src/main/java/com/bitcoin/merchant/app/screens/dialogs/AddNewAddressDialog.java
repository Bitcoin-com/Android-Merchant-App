package com.bitcoin.merchant.app.screens.dialogs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.TextView;

import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.screens.SettingsActivity;
import com.bitcoin.merchant.app.util.AppUtil;

public class AddNewAddressDialog {
    private final SettingsActivity ctx;
    private static final boolean ENTERING_ADDRESS_BYPASSED = false;

    public AddNewAddressDialog(SettingsActivity ctx) {
        this.ctx = ctx;
    }

    public void show() {
        final TextView tvReceiverHelp = new TextView(ctx);
        tvReceiverHelp.setText(ctx.getText(R.string.options_add_payment_address_text));
        tvReceiverHelp.setPadding(50, 10, 50, 10);
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.options_add_payment_address)
                .setView(tvReceiverHelp)
                .setCancelable(true)
                .setPositiveButton(R.string.paste, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        enterAddressUsingInputField();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        ctx.requestToOpenCamera();
                    }
                }).show();
    }


    private void showDialogToEnterAddress(final EditText etReceiver) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.options_add_payment_address)
                .setView(etReceiver)
                .setCancelable(false)
                .setPositiveButton(R.string.prompt_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ctx.validateThenSetNewAddress(etReceiver.getText().toString().trim());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.prompt_ko, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void enterAddressUsingInputField() {
        if (ENTERING_ADDRESS_BYPASSED) {
            ctx.setNewAddress("1MxRuANd5CmHWcveTwQaAJ36sStEQ5QM5k");
        } else {
            final EditText etReceiver = new EditText(ctx);
            etReceiver.setSingleLine(true);
            etReceiver.setText(AppUtil.convertToBitcoinCash(AppUtil.getReceivingAddress(ctx)));
            showDialogToEnterAddress(etReceiver);
        }
    }
}
