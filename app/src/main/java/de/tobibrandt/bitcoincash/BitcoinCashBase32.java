package de.tobibrandt.bitcoincash;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2018 Tobias Brandt
 * <p>
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
public class BitcoinCashBase32 {

    public static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    private static final char[] CHARS = CHARSET.toCharArray();

    private static Map<Character, Integer> charPositionMap;

    static {
        charPositionMap = new HashMap<>();
        for (int i = 0; i < CHARS.length; i++) {
            charPositionMap.put(CHARS[i], i);
        }
        if (charPositionMap.size() != 32) {
            throw new RuntimeException("The charset must contain 32 unique characters.");
        }
    }

    /**
     * Decode a base32 string back to the byte array representation
     *
     * @param base32String
     * @return
     */
    public static byte[] decode(String base32String) {
        byte[] bytes = new byte[base32String.length()];
        char[] charArray = base32String.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            Integer position = charPositionMap.get(charArray[i]);
            if (position == null) {
                throw new RuntimeException("There seems to be an invalid char: " + charArray[i]);
            }
            bytes[i] = (byte) ((int) position);
        }
        return bytes;
    }
}
