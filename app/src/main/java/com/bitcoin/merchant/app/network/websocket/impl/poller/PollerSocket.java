package com.bitcoin.merchant.app.network.websocket.impl.poller;

import android.util.Log;

import com.bitcoin.merchant.app.BuildConfig;
import com.bitcoin.merchant.app.network.ExpectedAmounts;
import com.bitcoin.merchant.app.network.ExpectedPayments;
import com.bitcoin.merchant.app.network.PaymentReceived;
import com.bitcoin.merchant.app.network.websocket.TxWebSocketHandler;
import com.bitcoin.merchant.app.network.websocket.WebSocketListener;
import com.bitcoin.merchant.app.network.websocket.impl.TxWebSocketHandlerImpl;
import com.google.gson.JsonObject;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PollerSocket implements TxWebSocketHandler {

    private WebSocketListener webSocketListener;
    private final OkHttpClient okHttpClient;

    private String BASE_URL = "https://bchrest.api.wombat.systems/v2/address/utxo/";

    private ScheduledExecutorService executorService;
    private volatile boolean polling = false;

    private final Set<String> subscribedAddresses = new HashSet<>();

    private final Set<String> alreadyHandled = new HashSet<>();

    public PollerSocket(WebSocketListener webSocketListener, OkHttpClient okHttpClient) {
        this.webSocketListener = webSocketListener;
        this.okHttpClient = okHttpClient;
    }

    @Override
    public void setListener(WebSocketListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }

    @Override
    public void subscribeToAddress(String address) {
        subscribedAddresses.add(address);
    }

    @Override
    public void start() {
        if (executorService != null && !executorService.isShutdown()) {
            return;
        }
        Log.i("Poller Task", "Starting poller task");
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(createPollerTask(), 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (executorService == null) {
            return;
        }
        Log.i("Poller Task", "Stopping poller task");
        executorService.shutdownNow();
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    /*{
      "height": 653632,
      "txid": "78ffb00ae72702b0a37f7c2e85cc40caca7fde3086637f18d29e4a208e2bbfb5",
      "vout": 0,
      "satoshis": 8673,
      "amount": 0.00008673,
      "confirmations": 192489
    }*/

    private Runnable createPollerTask() {
        return () -> {
            if (polling) {
                Log.i("Poller Task", "Previous poll still in progress, skipping");
                return;
            }
            polling = true;
            Log.i("Poller Task", "Polling for new transactions");

            try {
                for (String subscribedAddress : subscribedAddresses) {
                    String query = BASE_URL + subscribedAddress;
                    Request request = new Request.Builder()
                            .get()
                            .url(query)
                            .header("Authorization", BuildConfig.API_KEY)
                            .build();
                    try (Response execute = okHttpClient.newCall(request).execute()) {
                        JSONObject jsonObject = new JSONObject(execute.body().string());
                        JSONArray utxos = jsonObject.getJSONArray("utxos");
                        Log.i("Poller Task", "Found " + utxos.length() + " utxos for address " + subscribedAddress);
                        searchUtxos(subscribedAddress, utxos);
                    }
                }
            } catch (Exception e) {
                Log.e("Poller Task", "Error while polling for new transactions", e);
            } finally {
                polling = false;
            }
        };
    }

    private void searchUtxos(String subscribedAddress, JSONArray utxos) throws JSONException {
        for (int i = 0; i < utxos.length(); i++) {
            JSONObject utxo = utxos.getJSONObject(i);
            long satoshis = utxo.getLong("satoshis");
            String txid = utxo.getString("txid");
            int confirmations = utxo.getInt("confirmations");
            if (confirmations == 0) {
                Log.i("Poller Task", "Found unconfirmed transaction for address " + subscribedAddress + " with txid " + txid + " and amount " + satoshis + " satoshis");
                triggerPaymentReceived(subscribedAddress, satoshis, txid);
            }
        }
    }

    private void triggerPaymentReceived(String subscribedAddress, long satoshis, String txid) {
        ExpectedAmounts expected = ExpectedPayments.getInstance().getExpectedAmounts(subscribedAddress);
        if (alreadyHandled.contains(txid)) {
            return;
        }

        webSocketListener.onIncomingPayment(new PaymentReceived(
                subscribedAddress,
                satoshis,
                txid,
                System.currentTimeMillis() / 1000, 0,
                expected
        ));
        alreadyHandled.add(txid);
    }
}
