package com.bitcoin.merchant.app.network.websocket.impl.paybitcoincom;

import android.util.Log;

import com.bitcoin.merchant.app.network.ExpectedAmounts;
import com.bitcoin.merchant.app.network.ExpectedPayments;
import com.bitcoin.merchant.app.network.websocket.WebSocketListener;
import com.bitcoin.merchant.app.screens.PaymentReceived;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PayBitcoinComSocketHandler {
    public static final String TAG = "PayBitcoinComSH";

    private static final String serverUrl = "wss://pay.bitcoin.com/s/";
    private WebSocket ws;
    private WebSocketListener webSocketListener;

    public PayBitcoinComSocketHandler() {
    }

    public void setListener(WebSocketListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }

    public void startListeningForInvoice(final String invoiceId) {
        new Thread() {
            @Override
            public void run() {
                long doubleBackOff = 1000;
                while (true) {
                    try {
                        ws = connect(invoiceId);
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Connect", e);
                        try {
                            Thread.sleep(doubleBackOff);
                        } catch (InterruptedException ex) {
                            // fail silently
                        }
                        doubleBackOff *= 2;
                    }
                }
            }
        }.start();
    }

    private WebSocket connect(final String invoiceId) throws Exception {
        return new WebSocketFactory().createSocket(serverUrl + invoiceId).addListener(new WebSocketAdapter() {
            public void onTextMessage(WebSocket websocket, String message) {
                try {
                    JSONObject json = new JSONObject(message);
                    String status = json.getString("status");
                    if (status.equals("paid")) {
                        if (webSocketListener != null) {
                            JSONArray outputs = json.getJSONArray("outputs");
                            long invoiceAmount = 0;
                            for (int x = 0; x < outputs.length(); x++) {
                                long outputAmount = outputs.getJSONObject(0).getLong("amount");
                                invoiceAmount += outputAmount;
                            }
                            long timeInSec = System.currentTimeMillis() / 1000;
                            /*
                            Technically here we do set the PaymentReceived's bchReceived and the bchExpected to the same amount. I didn't want to rewrite all of this and potentially break stuff.
                            We can safely assume that the bchReceived is at least equal to the bchExpected due to the JSON status from the invoice itself being set as "paid" from pay.bitcoin.com
                             */
                            if (ExpectedPayments.getInstance().isValidAddress(invoiceId)) {
                                ExpectedAmounts expected = ExpectedPayments.getInstance().getExpectedAmounts(invoiceId);
                                PaymentReceived payment = new PaymentReceived(json.getString("paymentId"), invoiceAmount, json.getString("txId"), timeInSec, 0, expected);
                                Log.i("PayBitcoinComSocket", "expected payment:" + payment);
                                webSocketListener.onIncomingBIP70Payment(payment);
                            }
                        }
                    } else if (status.equals("expired")) {
                        if (webSocketListener != null) {
                            webSocketListener.cancelBIP70Payment();
                        }
                    }
                    //System.out.println("pay.bitcoin.com invoice(" + invoiceId + ") status: " + json.getString("status"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).connect();
    }

    public void disconnect() {
        if (ws != null)
            ws.disconnect();
    }
}