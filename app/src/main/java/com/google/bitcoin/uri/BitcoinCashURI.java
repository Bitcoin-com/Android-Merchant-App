/*
 * Copyright 2012 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.google.bitcoin.uri;

import com.github.kiulian.converter.AddressConverter;

import org.bitcoinj.core.Coin;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * <p>Provides a standard implementation of a Bitcoin URI with support for the
 * following:</p>
 *
 * <ul>
 * <li>URLEncoded URIs (as passed in by IE on the command line)</li>
 * <li>BIP21 names (including the "req-" prefix handling requirements)</li>
 * </ul>
 *
 * <h2>Accepted formats</h2>
 *
 * <p>The following input forms are accepted:</p>
 *
 * <ul>
 * <li>{@code bitcoin:<address>}</li>
 * <li>{@code bitcoin:<address>?<name1>=<value1>&<name2>=<value2>} with multiple
 * additional name/value pairs</li>
 * </ul>
 *
 * <p>The name/value pairs are processed as follows.</p>
 * <ol>
 * <li>URL encoding is stripped and treated as UTF-8</li>
 * <li>names prefixed with {@code req-} are treated as required and if unknown
 * or conflicting cause a parse exception</li>
 * <li>Unknown names not prefixed with {@code req-} are added to a Map, accessible
 * by parameter name</li>
 * <li>Known names not prefixed with {@code req-} are processed unless they are
 * malformed</li>
 * </ol>
 *
 * <p>The following names are known and have the following formats</p>
 * <ul>
 * <li>{@code amount} decimal value to 8 dp (e.g. 0.12345678) <b>Note that the
 * exponent notation is not supported any more</b></li>
 * <li>{@code label} any URL encoded alphanumeric</li>
 * <li>{@code message} any URL encoded alphanumeric</li>
 * </ul>
 *
 * @author Andreas Schildbach (initial code)
 * @author Jim Burton (enhancements for MultiBit)
 * @author Gary Rowe (BIP21 support)
 * @see <a href="https://en.bitcoin.it/wiki/BIP_0021">BIP 0021</a>
 */
public class BitcoinCashURI {
    /**
     * Provides logging for this class
     */
//    private static final Logger log = LoggerFactory.getLogger(BitcoinURI.class);
    // Not worth turning into an enum
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_LABEL = "label";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_ADDRESS = "address";
    public static final String BITCOIN_SCHEME = "bitcoincash";
    /**
     * How many "nanocoins" there are in a BitCoin.
     * <p/>
     * A nanocoin is the smallest unit that can be transferred using BitCoin.
     * The term nanocoin is very misleading, though, because there are only 100 million
     * of them in a coin (whereas one would expect 1 billion.
     */
    public static final BigInteger COIN = new BigInteger("100000000", 10);
    /**
     * How many "nanocoins" there are in 0.01 BitCoins.
     * <p/>
     * A nanocoin is the smallest unit that can be transferred using BitCoin.
     * The term nanocoin is very misleading, though, because there are only 100 million
     * of them in a coin (whereas one would expect 1 billion).
     */
    public static final BigInteger CENT = new BigInteger("1000000", 10);
    private static final String ENCODED_SPACE_CHARACTER = "%20";
    private static final String AMPERSAND_SEPARATOR = "&";
    private static final String QUESTION_MARK_SEPARATOR = "?";

    /**
     * Convert an amount expressed in the way humans are used to into nanocoins.
     */
    public static BigInteger toNanoCoins(int coins, int cents) {
        //       checkArgument(cents < 100);
        BigInteger bi = BigInteger.valueOf(coins).multiply(COIN);
        bi = bi.add(BigInteger.valueOf(cents).multiply(CENT));
        return bi;
    }

    /**
     * Simple Bitcoin URI builder using known good fields.
     *
     * @param legacy  The Bitcoin address
     * @param amount  The amount in nanocoins (decimal)
     * @param label   A label
     * @param message A message
     * @return A String containing the Bitcoin URI
     */
    public static String toURI(String legacy, Coin amount, String label, String message) {
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("Coin must be positive");
        } else {
            StringBuilder builder = new StringBuilder();
            String bchAddress = AddressConverter.toCashAddress(legacy);
            builder.append(bchAddress);
            boolean questionMarkHasBeenOutput = false;
            if (amount != null) {
                builder.append("?").append("amount").append("=");
                builder.append(amount.toPlainString());
                questionMarkHasBeenOutput = true;
            }
            if (label != null && !"".equals(label)) {
                if (questionMarkHasBeenOutput) {
                    builder.append("&");
                } else {
                    builder.append("?");
                    questionMarkHasBeenOutput = true;
                }
                builder.append("label").append("=").append(encodeURLString(label));
            }
            if (message != null && !"".equals(message)) {
                if (questionMarkHasBeenOutput) {
                    builder.append("&");
                } else {
                    builder.append("?");
                }
                builder.append("message").append("=").append(encodeURLString(message));
            }
            return builder.toString();
        }
    }

    /**
     * <p>
     * Returns the given value as a plain string denominated in BTC.
     * The result is unformatted with no trailing zeroes.
     * For instance, an input value of BigInteger.valueOf(150000) nanocoin gives an output string of "0.0015" BTC
     * </p>
     *
     * @param value The value in nanocoins to convert to a string (denominated in BTC)
     * @throws IllegalArgumentException If the input value is null
     */
    public static String bitcoinValueToPlainString(BigInteger value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        BigDecimal valueInBTC = new BigDecimal(value).divide(new BigDecimal(COIN));
        return valueInBTC.toPlainString();
    }

    /**
     * Encode a string using URL encoding
     *
     * @param stringToEncode The string to URL encode
     */
    static String encodeURLString(String stringToEncode) {
        try {
            return java.net.URLEncoder.encode(stringToEncode, "UTF-8").replace("+", ENCODED_SPACE_CHARACTER);
        } catch (UnsupportedEncodingException e) {
            // should not happen - UTF-8 is a valid encoding
            throw new RuntimeException(e);
        }
    }

    public static String toLegacyAddress(String address) {
        if (address != null) {
            address = address.trim();
            // convert from bitcoincash_address BCH TO legacy BTC address
            String bchAddress = address.toLowerCase();
            if (bchAddress.startsWith("q") && bchAddress.indexOf(':') == -1) {
                bchAddress = BITCOIN_SCHEME + ":" + bchAddress;
            }
            if (bchAddress.startsWith(BITCOIN_SCHEME + ":")) {
                try {
                    address = AddressConverter.toLegacyAddress(bchAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                    address = null;
                }
            }
        }
        return address;
    }
}
