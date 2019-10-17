package com.bitcoin.merchant.app.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.model.rest.Block;
import com.bitcoin.merchant.app.model.rest.Utxo;
import com.bitcoin.merchant.app.screens.PaymentReceived;

import java.util.List;

public class QueryTxTimeTask extends DownloadTask<Block> {
    private final Context context;
    private final boolean checkExpectedPayment;
    private final String address;
    private final Utxo utxo;
    private final List<Utxo> remainingUtxos;
    private final QueryUtxoType originalQuery;

    public QueryTxTimeTask(Context context, QueryUtxoType query, boolean checkExpectedPayment, String address, List<Utxo> utxos) {
        super(context);
        this.context = context;
        this.originalQuery = query;
        this.checkExpectedPayment = checkExpectedPayment;
        this.address = address;
        this.remainingUtxos = utxos.subList(1, utxos.size());
        this.utxo = utxos.get(0);
    }

    @Override
    protected Class<Block> getReturnClass() {
        return Block.class;
    }

    @Override
    protected String getUrl() {
        // 8MB block would be a 1,8MB request
        // https://rest.bitcoin.com/v2/block/detailsByHeight/479469
        // Using this method is clearly not efficient network wise
        // but it should be rare and only be needed when a UTXO has been missed by the sockets
        return "https://rest.bitcoin.com/v2/block/detailsByHeight/" + utxo.height;
    }

    @Override
    protected void onDownloaded(Block block) {
        if ((block != null) && (block.time != 0)) {
            utxo.ts = block.time;
            utxo.confirmations = block.confirmations;
            PaymentReceived payment = QueryUtxoTask.createPayment(utxo, checkExpectedPayment, address);
            updatePayment(payment);
        } else {
            Log.e("QueryTxTimeTask", "Block is null from " + getUrl());
        }
        if (remainingUtxos.size() > 0) {
            new QueryTxTimeTask(context, originalQuery, checkExpectedPayment, address, remainingUtxos).execute();
        } else {
            if (originalQuery == QueryUtxoType.ALL) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.ACTION_QUERY_ALL_UXTO_FINISHED));
            }
        }
    }

    private void updatePayment(PaymentReceived payment) {
        Intent intent = new Intent(MainActivity.ACTION_INTENT_UPDATE_TX);
        payment.toIntent(intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
