package de.tobibrandt.bitcoincash;

import java.math.BigInteger;

/**
 * Copyright (c) 2018 Tobias Brandt
 * <p>
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
public class BitcoinCashAddressFormatter {

    public static final String SEPARATOR = ":";

    public static final String MAIN_NET_PREFIX = "bitcoincash";

    private static final BigInteger[] POLYMOD_GENERATORS = new BigInteger[]{new BigInteger("98f2bc8e61", 16),
            new BigInteger("79b76d99e2", 16), new BigInteger("f33e5fb3c4", 16), new BigInteger("ae2eabe2a8", 16),
            new BigInteger("1e4f43e470", 16)};

    private static final BigInteger POLYMOD_AND_CONSTANT = new BigInteger("07ffffffff", 16);

    public static boolean isValidCashAddress(String bitcoinCashAddress) {
        try {
            if (bitcoinCashAddress == null || bitcoinCashAddress.length() == 0) {
                return false;
            }
            String prefix;
            if (bitcoinCashAddress.contains(SEPARATOR)) {
                String[] split = bitcoinCashAddress.split(SEPARATOR);
                if (split.length != 2) {
                    return false;
                }
                prefix = split[0];
                bitcoinCashAddress = split[1];
                if (!MAIN_NET_PREFIX.equals(prefix.toLowerCase())) {
                    return false;
                }
                if (!isSingleCase(prefix)) {
                    return false;
                }
            } else {
                prefix = MAIN_NET_PREFIX;
            }
            if (!isSingleCase(bitcoinCashAddress))
                return false;
            bitcoinCashAddress = bitcoinCashAddress.toLowerCase();
            byte[] checksumData = concatenateByteArrays(
                    concatenateByteArrays(getPrefixBytes(prefix), new byte[]{0x00}),
                    BitcoinCashBase32.decode(bitcoinCashAddress));
            byte[] calculateChecksumBytesPolymod = calculateChecksumBytesPolymod(checksumData);
            return new BigInteger(calculateChecksumBytesPolymod).compareTo(BigInteger.ZERO) == 0;
        } catch (RuntimeException re) {
            return false;
        }
    }

    private static boolean isSingleCase(String bitcoinCashAddress) {
        if (bitcoinCashAddress.equals(bitcoinCashAddress.toLowerCase())) {
            return true;
        }
        return bitcoinCashAddress.equals(bitcoinCashAddress.toUpperCase());
    }

    /**
     * @param checksumInput
     * @return Returns a 40 bits checksum in form of 5 8-bit arrays. This still has
     * to me mapped to 5-bit array representation
     */
    private static byte[] calculateChecksumBytesPolymod(byte[] checksumInput) {
        BigInteger c = BigInteger.ONE;
        for (int i = 0; i < checksumInput.length; i++) {
            byte c0 = c.shiftRight(35).byteValue();
            c = c.and(POLYMOD_AND_CONSTANT).shiftLeft(5)
                    .xor(new BigInteger(String.format("%02x", checksumInput[i]), 16));
            if ((c0 & 0x01) != 0)
                c = c.xor(POLYMOD_GENERATORS[0]);
            if ((c0 & 0x02) != 0)
                c = c.xor(POLYMOD_GENERATORS[1]);
            if ((c0 & 0x04) != 0)
                c = c.xor(POLYMOD_GENERATORS[2]);
            if ((c0 & 0x08) != 0)
                c = c.xor(POLYMOD_GENERATORS[3]);
            if ((c0 & 0x10) != 0)
                c = c.xor(POLYMOD_GENERATORS[4]);
        }
        byte[] checksum = c.xor(BigInteger.ONE).toByteArray();
        if (checksum.length == 5) {
            return checksum;
        } else {
            byte[] newChecksumArray = new byte[5];
            System.arraycopy(checksum, Math.max(0, checksum.length - 5), newChecksumArray,
                    Math.max(0, 5 - checksum.length), Math.min(5, checksum.length));
            return newChecksumArray;
        }

    }

    private static byte[] getPrefixBytes(String prefixString) {
        byte[] prefixBytes = new byte[prefixString.length()];
        char[] charArray = prefixString.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            prefixBytes[i] = (byte) (charArray[i] & 0x1f);
        }
        return prefixBytes;
    }

    /**
     * Concatenates the two given byte arrays and returns the combined byte
     * array.
     *
     * @param first
     * @param second
     * @return
     */
    private static byte[] concatenateByteArrays(byte[] first, byte[] second) {
        byte[] concatenatedBytes = new byte[first.length + second.length];
        System.arraycopy(first, 0, concatenatedBytes, 0, first.length);
        System.arraycopy(second, 0, concatenatedBytes, first.length, second.length);
        return concatenatedBytes;
    }
}
