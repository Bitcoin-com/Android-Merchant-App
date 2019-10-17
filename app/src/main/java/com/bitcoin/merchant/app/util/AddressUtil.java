package com.bitcoin.merchant.app.util;

import com.github.kiulian.converter.AddressConverter;
import com.github.kiulian.converter.b58.B58;

import de.tobibrandt.bitcoincash.BitcoinCashAddressFormatter;

public class AddressUtil {

    public static boolean isValidCashAddr(String address) {
        return BitcoinCashAddressFormatter.isValidCashAddress(address);
    }

    public static boolean isValidLegacy(String address) {
        try {
            B58.decodeAndCheck(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String toCashAddress(String legacy) {
        return AddressConverter.toCashAddress(legacy);
    }
}
