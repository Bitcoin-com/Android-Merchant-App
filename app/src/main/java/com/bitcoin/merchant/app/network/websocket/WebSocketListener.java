package com.bitcoin.merchant.app.network.websocket;

import com.bitcoin.merchant.app.screens.PaymentReceived;

public interface WebSocketListener {
    void onIncomingPayment(PaymentReceived payment);

    void onIncomingBIP70Payment(PaymentReceived payment);

    void cancelBIP70Payment();
}
