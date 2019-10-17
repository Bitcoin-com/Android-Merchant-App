package com.bitcoin.merchant.app.screens;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.bitcoin.merchant.app.R;
import com.bitcoin.merchant.app.util.AppUtil;
import com.bitcoin.merchant.app.util.OSUtil;
import com.bitcoin.merchant.app.util.PrefsUtil;
import com.bitcoin.merchant.app.util.ToastCustom;

import java.util.Timer;
import java.util.TimerTask;
//import android.util.Log;

public class PinActivity extends Activity implements View.OnClickListener {
    String userEnteredPIN = "";
    String userEnteredPINConfirm = null;
    private TextView titleView = null;
    private TextView[] pinBoxArray = null;
    private boolean doCreate = false;

    public static boolean isPinMissing(Context ctx) {
        String pin = PrefsUtil.getInstance(ctx).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
        return pin.equals("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pin);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTitle(R.string.action_pincode);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean("create")) {
                doCreate = true;
            }
        }
        titleView = findViewById(R.id.titleBox);
        Button button0 = findViewById(R.id.button0);
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
        Button button4 = findViewById(R.id.button4);
        Button button5 = findViewById(R.id.button5);
        Button button6 = findViewById(R.id.button6);
        Button button7 = findViewById(R.id.button7);
        Button button8 = findViewById(R.id.button8);
        Button button9 = findViewById(R.id.button9);
        Button buttonDeleteBack = findViewById(R.id.buttonDeleteBack);
        button0.setOnClickListener(this);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);
        button6.setOnClickListener(this);
        button7.setOnClickListener(this);
        button8.setOnClickListener(this);
        button9.setOnClickListener(this);
        buttonDeleteBack.setOnClickListener(this);
        if (doCreate) {
            titleView.setText(R.string.create_pin);
        } else {
            titleView.setText(R.string.enter_pin);
        }
        pinBoxArray = new TextView[]{
                findViewById(R.id.pinBox0),
                findViewById(R.id.pinBox1),
                findViewById(R.id.pinBox2),
                findViewById(R.id.pinBox3)
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && !doCreate) {
            finish();
            return true;
        }
        return false;
    }

    public void padClicked(String num) {
        if (userEnteredPIN.length() == pinBoxArray.length) {
            return;
        }
        // Append tapped #
        userEnteredPIN = userEnteredPIN + num;
        pinBoxArray[userEnteredPIN.length() - 1].setBackgroundResource(R.drawable.passcode_blob);
        // Perform appropriate action if PIN_LENGTH has been reached
        if (userEnteredPIN.length() == pinBoxArray.length) {
            if (!doCreate) {
                validatePin();
            } else if (userEnteredPINConfirm == null) {
                createNewPin();
            } else if (userEnteredPINConfirm.equals(userEnteredPIN)) {
                confirmNewPin();
            } else {
                pinCreationError();
            }
        }
    }

    private void confirmNewPin() {
        // End of Confirm - Pin is confirmed
        String hashed = OSUtil.getSha256(userEnteredPIN);
        PrefsUtil.getInstance(PinActivity.this).setValue(PrefsUtil.MERCHANT_KEY_PIN, hashed);
        PrefsUtil.getInstance(PinActivity.this).setValue(PrefsUtil.MERCHANT_KEY_ACCOUNT_INDEX, 0);
        setResult(RESULT_OK);
        finish();
    }

    private void validatePin() {
        // Validate
        String hashed = OSUtil.getSha256(userEnteredPIN);
        String stored = PrefsUtil.getInstance(PinActivity.this).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
        if (stored.equals(hashed)) {
            setResult(RESULT_OK);
            finish();
        } else {
            delayAction(new Runnable() {
                @Override
                public void run() {
                    ToastCustom.makeText(PinActivity.this, getString(R.string.pin_code_enter_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    clearPinBoxes();
                    userEnteredPIN = "";
                    userEnteredPINConfirm = null;
                }
            }, 750);
        }
    }

    private void createNewPin() {
        // End of Create -  Change to Confirm
        Runnable action = new Runnable() {
            @Override
            public void run() {
                titleView.setText(R.string.confirm_pin);
                clearPinBoxes();
                userEnteredPINConfirm = userEnteredPIN;
                userEnteredPIN = "";
            }
        };
        delayAction(action, 200);
    }

    private void delayAction(final Runnable action, int delay) {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                PinActivity.this.runOnUiThread(action);
                timer.cancel();
            }
        }, delay);
    }

    private void pinCreationError() {
        //End of Confirm - Pin Mismatch
        ToastCustom.makeText(PinActivity.this, getString(R.string.pin_code_create_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        clearPinBoxes();
        userEnteredPIN = "";
        userEnteredPINConfirm = null;
        titleView.setText(R.string.create_pin);
    }

    public void cancelClicked() {
        clearPinBoxes();
        userEnteredPIN = "";
    }

    private void clearPinBoxes() {
        if (userEnteredPIN.length() > 0) {
            for (TextView aPinBoxArray : pinBoxArray) {
                aPinBoxArray.setBackground(null);//reset pin buttons blank
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (AppUtil.isReceivingAddressAvailable(this))
            this.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        Button viewButton = (Button) view;
        if (view.getId() != R.id.buttonDeleteBack)
            padClicked(viewButton.getText().toString());
        else
            cancelClicked();
    }
}
