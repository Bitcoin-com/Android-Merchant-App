package com.bitcoin.merchant.app.network.websocket.impl;

import android.util.Log;

import com.bitcoin.merchant.app.network.ExpectedPayments;
import com.bitcoin.merchant.app.network.websocket.TxWebSocketHandler;
import com.bitcoin.merchant.app.network.websocket.WebSocketListener;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Socket will reconnect even without calling again webSocketHandler.start()
 * and without ACTION_INTENT_RECONNECT being sent by the ConnectivityManager.
 * The number of Thread will stay constant about 17 or 18 on OS v5.
 */
public abstract class TxWebSocketHandlerImpl implements TxWebSocketHandler {
    private static final long PING_INTERVAL = 20 * 1000L; // ping every 20 seconds
    private final WebSocketFactory webSocketFactory;
    private volatile ConnectionHandler handler;
    protected WebSocketListener webSocketListener;
    protected String TAG = "WebSocketHandler";

    public TxWebSocketHandlerImpl() {
        this.webSocketFactory = new WebSocketFactory();
    }

    @Override
    public void setListener(WebSocketListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }


    @Override
    public void start() {
        try {
            Log.i(TAG, "start threads:" + Thread.activeCount());
            stop();
            new ConnectionThread().start();
        } catch (Exception e) {
            Log.e(TAG, "start", e);
        }
    }

    @Override
    public void stop() {
        if (handler != null) {
            handler.stop();
        }
    }

    @Override
    public boolean isConnected() {
        return handler != null && handler.isConnected() && !handler.isBroken();
    }

    private void send(String message) {
        if (handler != null) {
            handler.send(message);
        }
    }

    @Override
    public synchronized void subscribeToAddress(String address) {
        send(getSubscribeMessage(address));
    }

    private static String getSubscribeMessage(String address) {
        return "{\"op\":\"addr_sub\", \"addr\":\"" + address + "\"}";
    }

    private class ConnectionThread extends Thread {
        public ConnectionThread() {
            setName("ConnectWebSocket");
            setDaemon(true);
        }

        @Override
        public void run() {
            long doubleBackOff = 1000;
            while (true) {
                try {
                    handler = new ConnectionHandler();
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
    }

    abstract protected WebSocket createWebSocket(WebSocketFactory factory) throws IOException;

    abstract protected void parseTx(String message) throws Exception;

    private class ConnectionHandler extends WebSocketAdapter {
        public static final int MINUTE_IN_MS = 60 * 1000;
        private final Set<String> sentMessageSet = new HashSet<>();
        private final WebSocket mConnection;
        private volatile boolean autoReconnect = true;
        private long timeLastAlive;

        public ConnectionHandler() throws Exception {
            timeLastAlive = System.currentTimeMillis();
            mConnection = createWebSocket(TxWebSocketHandlerImpl.this.webSocketFactory)
                    .recreate()
                    .addListener(this);
            mConnection.setPingInterval(PING_INTERVAL);
            mConnection.connect();
            for (String address : ExpectedPayments.getInstance().getAddresses()) {
                directSend(getSubscribeMessage(address));
            }
            timeLastAlive = System.currentTimeMillis();
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onPongFrame(websocket, frame);
            timeLastAlive = System.currentTimeMillis();
            Log.d(TAG, "PongSuccess threads:" + Thread.activeCount());
        }

        @Override
        public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onPingFrame(websocket, frame);
            timeLastAlive = System.currentTimeMillis();
            Log.d(TAG, "PingSuccess threads:" + Thread.activeCount());
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            Log.i(TAG, "onConnected threads:" + Thread.activeCount());
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.e(TAG, "onDisconnected threads:" + Thread.activeCount());
            if (autoReconnect) {
                // reconnect on involuntary disconnection
                start();
            }
        }

        @Override
        public void onTextMessage(WebSocket websocket, String message) {
            try {
                parseTx(message);
            } catch (Exception e) {
                Log.e(TAG, message, e);
            }
        }

        public boolean isConnected() {
            return mConnection != null && mConnection.isOpen();
        }

        public void stop() {
            if (isConnected()) {
                autoReconnect = false;
                mConnection.clearListeners();
                mConnection.disconnect();
            }
        }

        private void send(String message) {
            // Make sure each message is only sent once per socket lifetime
            if (!sentMessageSet.contains(message)) {
                try {
                    if (isConnected()) {
                        directSend(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "send:" + message);
                }
            }
        }

        private void directSend(String message) {
            mConnection.sendText(message);
            sentMessageSet.add(message);
        }

        public boolean isBroken() {
            // considered broken when older than 1 minute and with no ping or pong during that time
            return (timeLastAlive + MINUTE_IN_MS) < System.currentTimeMillis();
        }
    }
}
