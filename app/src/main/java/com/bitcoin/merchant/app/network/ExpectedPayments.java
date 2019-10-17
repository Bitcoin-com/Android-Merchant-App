package com.bitcoin.merchant.app.network;

import android.util.Log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ExpectedPayments {
    private static final ExpectedPayments instance = new ExpectedPayments();
    private final Map<String, ExpectedAmounts> addressToAmounts;

    private ExpectedPayments() {
        Map<String, ExpectedAmounts> cache = new LinkedHashMap() {
            @Override
            protected boolean removeEldestEntry(final Map.Entry eldest) {
                return size() > 8;
            }
        };
        addressToAmounts = Collections.synchronizedMap(cache);
    }

    public static ExpectedPayments getInstance() {
        return instance;
    }

    public void addExpectedPayment(String receivingAddress, long bchAmount, String fiatAmount) {
        addressToAmounts.put(receivingAddress, new ExpectedAmounts(bchAmount, fiatAmount));
        Log.i(ExpectedPayments.class.getSimpleName(), addressToAmounts.size() + " Pending payments: " + addressToAmounts.toString());
    }

    public void removePayment(String receivingAddress) {
        addressToAmounts.remove(receivingAddress);
        Log.i(ExpectedPayments.class.getSimpleName(), addressToAmounts.size() + " Pending payments: " + addressToAmounts.toString());
    }

    public ExpectedAmounts getExpectedAmounts(String receivingAddress) {
        ExpectedAmounts amounts = addressToAmounts.get(receivingAddress);
        return (amounts == null) ? ExpectedAmounts.UNDEFINED : amounts;
    }

    public boolean isValidAddress(String receivingAddress) {
        return addressToAmounts.containsKey(receivingAddress);
    }

    public Set<String> getAddresses() {
        return new TreeSet<>(addressToAmounts.keySet());
    }
}
