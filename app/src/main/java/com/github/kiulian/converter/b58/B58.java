package com.github.kiulian.converter.b58;

/*-
 * -----------------------LICENSE_START-----------------------
 * Bitcoincash address converter
 * %%
 * Copyright (C) 2018 Igor Kiulian
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------LICENSE_END-----------------------
 */

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class B58 {
    private static int[] mIndexes;
    private static char[] mAlphabet;

    static {
        mAlphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
        mIndexes = new int[128];
        for (int i = 0; i < mIndexes.length; i++) {
            mIndexes[i] = -1;
        }
        for (int i = 0; i < mAlphabet.length; i++) {
            mIndexes[mAlphabet[i]] = i;
        }
    }

    private B58() {
    }

    public static byte[] findPrefix(int payLoadLength, String desiredPrefix) {
        int totalLength = payLoadLength + 4; // for the checksum
        double chars = Math.log(Math.pow(256, totalLength)) / Math.log(58);
        int requiredChars = (int) Math.ceil(chars + 0.2D);
        // Mess with this to see stability tests fail
        int charPos = (mAlphabet.length / 2) - 1;
        char padding = mAlphabet[(charPos)];
        String template = desiredPrefix + repeat(requiredChars, padding);
        byte[] decoded = decode(template);
        return copyOfRange(decoded, 0, decoded.length - totalLength);
    }

    private static String repeat(int times, char repeated) {
        char[] chars = new char[times];
        Arrays.fill(chars, repeated);
        return new String(chars);
    }

    public static String encodeToStringChecked(byte[] input, int version) {
        return encodeToStringChecked(input, new byte[]{(byte) version});
    }

    public static String encodeToStringChecked(byte[] input, byte[] version) {
        return new String(encodeToBytesChecked(input, version), StandardCharsets.US_ASCII);
    }

    public static byte[] encodeToBytesChecked(byte[] input, int version) {
        return encodeToBytesChecked(input, new byte[]{(byte) version});
    }

    public static byte[] encodeToBytesChecked(byte[] input, byte[] version) {
        byte[] buffer = new byte[input.length + version.length];
        System.arraycopy(version, 0, buffer, 0, version.length);
        System.arraycopy(input, 0, buffer, version.length, input.length);
        byte[] checkSum = copyOfRange(HashUtils.doubleDigest(buffer), 0, 4);
        byte[] output = new byte[buffer.length + checkSum.length];
        System.arraycopy(buffer, 0, output, 0, buffer.length);
        System.arraycopy(checkSum, 0, output, buffer.length, checkSum.length);
        return encodeToBytes(output);
    }

    public static String encodeToString(byte[] input) {
        byte[] output = encodeToBytes(input);
        return new String(output, StandardCharsets.US_ASCII);
    }

    public static byte[] encodeToBytes(byte[] input) {
        if (input.length == 0) {
            return new byte[0];
        }
        input = copyOfRange(input, 0, input.length);
        // Count leading zeroes.
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0) {
            ++zeroCount;
        }
        // The actual encoding.
        byte[] temp = new byte[input.length * 2];
        int j = temp.length;
        int startAt = zeroCount;
        while (startAt < input.length) {
            byte mod = divmod58(input, startAt);
            if (input[startAt] == 0) {
                ++startAt;
            }
            temp[--j] = (byte) mAlphabet[mod];
        }
        // Strip extra '1' if there are some after decoding.
        while (j < temp.length && temp[j] == mAlphabet[0]) {
            ++j;
        }
        // Add as many leading '1' as there were leading zeros.
        while (--zeroCount >= 0) {
            temp[--j] = (byte) mAlphabet[0];
        }
        byte[] output;
        output = copyOfRange(temp, j, temp.length);
        return output;
    }

    public static byte[] decode(String input) throws EncodingFormatException {
        if (input.length() == 0) {
            return new byte[0];
        }
        byte[] input58 = new byte[input.length()];
        // Transform the String to a base58 byte sequence
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int digit58 = -1;
            if (c >= 0 && c < 128) {
                digit58 = mIndexes[c];
            }
            if (digit58 < 0) {
                throw new EncodingFormatException("Illegal character " + c + " at " + i);
            }
            input58[i] = (byte) digit58;
        }
        // Count leading zeroes
        int zeroCount = 0;
        while (zeroCount < input58.length && input58[zeroCount] == 0) {
            ++zeroCount;
        }
        // The encoding
        byte[] temp = new byte[input.length()];
        int j = temp.length;
        int startAt = zeroCount;
        while (startAt < input58.length) {
            byte mod = divmod256(input58, startAt);
            if (input58[startAt] == 0) {
                ++startAt;
            }
            temp[--j] = mod;
        }
        // Do no add extra leading zeroes, move j to first non null byte.
        while (j < temp.length && temp[j] == 0) {
            ++j;
        }
        return copyOfRange(temp, j - zeroCount, temp.length);
    }

    public static BigInteger decodeToBigInteger(String input) throws EncodingFormatException {
        return new BigInteger(1, decode(input));
    }

    public static byte[] decodeChecked(String input, int version) throws EncodingFormatException {
        byte[] buffer = decodeAndCheck(input);
        byte actualVersion = buffer[0];
        if (actualVersion != version) {
            throw new EncodingFormatException("Bro, version is wrong yo");
        }
        return copyOfRange(buffer, 1, buffer.length - 4);
    }

    public static Decoded decodeMulti(String input,
                                      int expectedLength,
                                      byte[]... possibleVersions) throws EncodingFormatException {
        byte[] buffer = decodeAndCheck(input);
        int versionLength = buffer.length - 4 - expectedLength;
        byte[] versionBytes = copyOfRange(buffer, 0, versionLength);
        byte[] foundVersion = null;
        for (byte[] possible : possibleVersions) {
            if (Arrays.equals(possible, versionBytes)) {
                foundVersion = possible;
                break;
            }
        }
        if (foundVersion == null) {
            throw new EncodingFormatException("Bro, version is wrong yo");
        }
        byte[] bytes = copyOfRange(buffer, versionLength, buffer.length - 4);
        return new Decoded(foundVersion, bytes);
    }

    public static byte[] decodeAndCheck(String input) {
        byte[] buffer = decode(input);
        if (buffer.length < 4)
            throw new EncodingFormatException("Input too short");
        byte[] toHash = copyOfRange(buffer, 0, buffer.length - 4);
        byte[] hashed = copyOfRange(HashUtils.doubleDigest(toHash), 0, 4);
        byte[] checksum = copyOfRange(buffer, buffer.length - 4, buffer.length);
        if (!Arrays.equals(checksum, hashed))
            throw new EncodingFormatException("Checksum does not validate");
        return buffer;
    }

    //
    // number -> number / 58, returns number % 58
    //
    private static byte divmod58(byte[] number, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++) {
            int digit256 = (int) number[i] & 0xFF;
            int temp = remainder * 256 + digit256;
            number[i] = (byte) (temp / 58);
            remainder = temp % 58;
        }
        return (byte) remainder;
    }

    //
    // number -> number / 256, returns number % 256
    //
    private static byte divmod256(byte[] number58, int startAt) {
        int remainder = 0;
        for (int i = startAt; i < number58.length; i++) {
            int digit58 = (int) number58[i] & 0xFF;
            int temp = remainder * 58 + digit58;
            number58[i] = (byte) (temp / 256);
            remainder = temp % 256;
        }
        return (byte) remainder;
    }

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }

    public static class Decoded {
        public final byte[] version;
        public final byte[] payload;

        public Decoded(byte[] version, byte[] payload) {
            this.version = version;
            this.payload = payload;
        }
    }
}
