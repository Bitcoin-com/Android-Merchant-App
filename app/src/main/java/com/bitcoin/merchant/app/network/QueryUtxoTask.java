package com.bitcoin.merchant.app.network;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bitcoin.merchant.app.MainActivity;
import com.bitcoin.merchant.app.database.DBControllerV3;
import com.bitcoin.merchant.app.database.PaymentRecord;
import com.bitcoin.merchant.app.model.rest.Utxo;
import com.bitcoin.merchant.app.model.rest.Utxos;
import com.bitcoin.merchant.app.screens.PaymentReceived;
import com.bitcoin.merchant.app.util.AppUtil;
import com.bitcoin.merchant.app.util.PrefsUtil;

import java.util.ArrayList;
import java.util.List;

public class QueryUtxoTask extends DownloadTask<Utxos> {
    private final Context context;
    private final QueryUtxoType query;
    private final QueryUtxoType nextQuery;
    private final boolean memPool;
    private final boolean checkExpectedPayment;
    private final List<Utxo> utxosWithoutTimeButWithBlockHeight = new ArrayList<>();
    private final List<Utxo> utxosWithUpdatedConfirmations = new ArrayList<>();

    public static PaymentReceived createPayment(Utxo utxo, boolean checkExpectedPayment, String address) {
        ExpectedAmounts expected = checkExpectedPayment
                ? ExpectedPayments.getInstance().getExpectedAmounts(address)
                : ExpectedAmounts.UNDEFINED;
        // use current time for expected payments
        long ts = checkExpectedPayment ? System.currentTimeMillis() / 1000 : utxo.ts;
        return new PaymentReceived(address, utxo.satoshis, utxo.txid, ts, (int) utxo.confirmations, expected);
    }

    public QueryUtxoTask(Context context, QueryUtxoType... queries) {
        super(context);
        this.context = context;
        query = queries[0];
        memPool = query == QueryUtxoType.UNCONFIRMED;
        checkExpectedPayment = memPool;
        nextQuery = queries.length > 1 ? queries[1] : null;
    }

    @Override
    protected Class<Utxos> getReturnClass() {
        return Utxos.class;
    }

    @Override
    protected String getUrl() {
        AppUtil instance = AppUtil.getInstance(context);
        if (!instance.hasValidReceiver() || instance.isValidXPub()) {
            return null;
        }
        String address = PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");
        String url = memPool
                ? "https://rest.bitcoin.com/v2/address/unconfirmed/"
                : "https://rest.bitcoin.com/v2/address/utxo/";
        return url + address;
    }

    private void removeKnownTxAndFindTxWithMissingTime(Utxos utxos) {
        DBControllerV3 db = new DBControllerV3(context);
        List<Utxo> newUtxos = new ArrayList<>();
        for (Utxo utxo : utxos.utxos) {
            try {
                ContentValues values = db.getPaymentFromTx(utxo.txid);
                if (values == null) {
                    newUtxos.add(utxo);
                }
                // when they are in the memPool, we assume that time is now
                if (!memPool) {
                    // are we missing the time & do we have the height to query it ?
                    if ((utxo.ts == 0) && (utxo.height > 0)) {
                        if (values != null) {
                            PaymentRecord record = new PaymentRecord(values);
                            if (record.timeInSec == 0) {
                                // known TX but we miss the correct time
                                utxosWithoutTimeButWithBlockHeight.add(utxo);
                            }
                        } else {
                            // unknown TX but we miss the correct time
                            utxosWithoutTimeButWithBlockHeight.add(utxo);
                        }
                    }
                    if (values != null) {
                        int confirmations = new PaymentRecord(values).confirmations;
                        if (confirmations < 2 && confirmations < utxo.confirmations) {
                            utxosWithUpdatedConfirmations.add(utxo);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        utxos.utxos = newUtxos.toArray(new Utxo[0]);
    }

    @Override
    protected void onDownloaded(Utxos result) {
        if ((result != null) && (result.utxos != null)) {
            Log.i("QueryUtxoTask", result.utxos.length + " TX founds");
            removeKnownTxAndFindTxWithMissingTime(result);
            for (Utxo utxo : result.utxos) {
                recordPayment(createPayment(utxo, checkExpectedPayment, result.legacyAddress));
            }
            for (Utxo utxo : utxosWithUpdatedConfirmations) {
                updatePayment(createPayment(utxo, checkExpectedPayment, result.legacyAddress));
            }
        }
        if (nextQuery != null) {
            new QueryUtxoTask(context, nextQuery).execute();
        }
        if ((utxosWithoutTimeButWithBlockHeight.size() > 0) && (result != null)) {
            new QueryTxTimeTask(context, query, checkExpectedPayment, result.legacyAddress, utxosWithoutTimeButWithBlockHeight).execute();
        } else {
            if (query == QueryUtxoType.ALL) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.ACTION_QUERY_ALL_UXTO_FINISHED));
            }
        }
    }

    private void recordPayment(PaymentReceived payment) {
        Intent intent = new Intent(MainActivity.ACTION_INTENT_RECORD_TX);
        payment.toIntent(intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void updatePayment(PaymentReceived payment) {
        Intent intent = new Intent(MainActivity.ACTION_INTENT_UPDATE_TX);
        payment.toIntent(intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
