package com.bitcoin.merchant.app.util;

import android.content.Context;
import android.util.Log;

import com.bitcoin.merchant.app.database.DBControllerV3;
import com.github.kiulian.converter.b58.B58;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONObject;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class WalletUtil {
    public static final String TAG = "WalletUtil";
    private final String xPub;
    private int xpubIndex;
    private final DeterministicKey accountKey;
    private final Context context;
    // For performance reasons, we cache all used addresses (reported in May 2019 on Lenovo Tab E8)
    private final AddressBank addressBank;

    private class AddressBank {
        final Set<String> usedAddresses;

        public AddressBank() {
            Set<String> addresses = new HashSet<>();
            try {
                addresses = new DBControllerV3(context).getAllAddresses();
                Log.d(TAG, "loaded " + addresses.size() + " addresses from TX history: " + addresses);
            } catch (Exception e) {
                Log.e(TAG, "Unable to load addresses from TX history");
            }
            usedAddresses = Collections.synchronizedSet(addresses);
        }

        public boolean isUsed(String address) {
            return usedAddresses.contains(address);
        }

        public void addUsedAddress(String address) {
            usedAddresses.add(address);
        }
    }

    private static DeterministicKey createMasterPubKeyFromXPub(String xpubstr) throws AddressFormatException {
        byte[] xpubBytes = Base58.decodeChecked(xpubstr);
        ByteBuffer bb = ByteBuffer.wrap(xpubBytes);
        int prefix = bb.getInt();
        if (prefix != 0x0488B21E) {
            throw new AddressFormatException("invalid xpub version");
        }
        byte[] chain = new byte[32];
        byte[] pub = new byte[33];
        bb.get();
        bb.getInt();
        bb.getInt();
        bb.get(chain);
        bb.get(pub);
        return HDKeyDerivation.createMasterPubKeyFromBytes(pub, chain);
    }

    public boolean isSameXPub(String xPub) {
        byte[] b1 = B58.decodeAndCheck(this.xPub);
        byte[] b2 = B58.decodeAndCheck(xPub);
        return Arrays.equals(b1, b2);
    }

    public WalletUtil(String xPub, Context context) throws Exception {
        this.xPub = xPub;
        this.context = context;
        this.xpubIndex = !this.isSameXPub(this.xPub) ? 0 : PrefsUtil.getInstance(context).getValue(PrefsUtil.MERCHANT_KEY_XPUB_INDEX, 0);
        DeterministicKey key = WalletUtil.createMasterPubKeyFromXPub(xPub);
        //This gets the receive chain from the xpub. If you want to generate change addresses, switch to 1 for the childNumber.
        this.accountKey = HDKeyDerivation.deriveChildKey(key, new ChildNumber(0, false));
        addressBank = new AddressBank();
    }

    public void addUsedAddress(String address) {
        addressBank.addUsedAddress(address.trim());
    }

    private void saveWallet(int newIndex) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MERCHANT_KEY_XPUB_INDEX, newIndex);
        Log.d(TAG, "Saving new xpub index " + newIndex);
    }

    public String generateAddressFromXPub() {
        String potentialAddress = getAddressFromXpubKey(this.xpubIndex);
        while (true) {
            if (addressBank.isUsed(potentialAddress)) {
                this.xpubIndex++;
                Log.d(TAG, "Getting next xpub index " + this.xpubIndex);
                saveWallet(this.xpubIndex);
                potentialAddress = getAddressFromXpubKey(this.xpubIndex);
            } else {
                boolean hasHistory = doesAddressHaveHistory(potentialAddress);
                if (hasHistory) {
                    this.xpubIndex++;
                    Log.d(TAG, "Getting next xpub index " + this.xpubIndex);
                    saveWallet(this.xpubIndex);
                    addUsedAddress(potentialAddress);
                    potentialAddress = getAddressFromXpubKey(this.xpubIndex);
                } else {
                    break;
                }
            }
        }
        saveWallet(this.xpubIndex);
        return potentialAddress;
    }

    public boolean syncXpub() {
        String potentialAddress = getAddressFromXpubKey(this.xpubIndex);
        while (true) {
            if (addressBank.isUsed(potentialAddress)) {
                this.xpubIndex++;
                Log.d(TAG, "Getting next xpub index " + this.xpubIndex);
                saveWallet(this.xpubIndex);
                potentialAddress = getAddressFromXpubKey(this.xpubIndex);
            } else {
                boolean hasHistory = doesAddressHaveHistory(potentialAddress);
                if (hasHistory) {
                    this.xpubIndex++;
                    Log.d(TAG, "Getting next xpub index " + this.xpubIndex);
                    saveWallet(this.xpubIndex);
                    addUsedAddress(potentialAddress);
                    potentialAddress = getAddressFromXpubKey(this.xpubIndex);
                } else {
                    break;
                }
            }
        }
        saveWallet(this.xpubIndex);
        return true;
    }

    private boolean doesAddressHaveHistory(String address) {
        try {
            String out = new Scanner(new URL("https://rest.bitcoin.com/v2/address/details/" + address).openStream(), "UTF-8").useDelimiter("\\A").next();
            JSONObject json = new JSONObject(out);
            return json.getInt("txApperances") > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return doesAddressHaveHistory(address);
        }
    }

    private String getAddressFromXpubKey(int index) {
        //This takes the accountKey from earlier, on the receive chain, and generates an address. For example, m/44'/145'/0'/0/{index}
        DeterministicKey dk = HDKeyDerivation.deriveChildKey(this.accountKey, new ChildNumber(index, false));
        ECKey ecKey = ECKey.fromPublicOnly(dk.getPubKey());
        return ecKey.toAddress(MainNetParams.get()).toBase58();
    }

    @Override
    public String toString() {
        return "WalletUtil{" +
                "xPub='" + xPub + '\'' +
                ", index=" + this.xpubIndex +
                '}';
    }
}
