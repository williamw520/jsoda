

package wwutil.jsoda;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.math.BigInteger;



/**
 * Encoder for BaseX
 */
public class BaseXUtil
{
    // Base62
    public static final String DIGITS_62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    // Base58 Base62 minus the 0 O 1 l
    public static final String DIGITS_58 = "23456789ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";


    /**
     * Encodes a number using BaseX encoding.
     *
     * @param number a positive integer
     * @param alphabets is the set of alphabet for encoding.
     * @return a BaseX string
     * @throws IllegalArgumentException if <code>number</code> is negative
     */
    public static String encode(BigInteger number, String alphabets) {
        if (number.compareTo(BigInteger.ZERO) == -1)
            throw new IllegalArgumentException("Number cannot be negative");

        StringBuilder   sb = new StringBuilder();
        BigInteger      base = BigInteger.valueOf(alphabets.length());
        BigInteger[]    divrem = new BigInteger[] {number, BigInteger.ZERO};

        while (divrem[0].compareTo(BigInteger.ZERO) == 1) {
            divrem = divrem[0].divideAndRemainder(base);
            sb.append(alphabets.charAt(divrem[1].intValue()));
        }
        return (sb.length() == 0) ? alphabets.substring(0, 1) : sb.reverse().toString();
    }

    /**
     * Decodes a string using BaseX encoding.
     *
     * @param str a BaseX String
     * @param alphabets is the set of alphabet for encoding.
     * @return a positive number
     * @throws IllegalArgumentException if <code>str</code> is empty
     */
    public static BigInteger decode(final String str, String alphabets) {
        if (str.length() == 0)
            throw new IllegalArgumentException("Str cannot be empty");

        BigInteger  result = BigInteger.ZERO;
        BigInteger  base = BigInteger.valueOf(alphabets.length());
        int         digits = str.length();
        for (int i = 0; i < digits; i++) {
            int digit = alphabets.indexOf(str.charAt(digits - i - 1));
            result = result.add(BigInteger.valueOf(digit).multiply(base.pow(i)));
        }
        return result;
    }

    public static String encode58(BigInteger number) {
        return encode(number, DIGITS_58);
    }

    public static BigInteger decode58(final String str) {
        return decode(str, DIGITS_58);
    }

    public static String encode62(BigInteger number) {
        return encode(number, DIGITS_62);
    }

    public static BigInteger decode62(final String str) {
        return decode(str, DIGITS_62);
    }

    public static String uuidToBase58(UUID uuid) {
        ByteBuffer  bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        BigInteger  number = new BigInteger(bb.array()).abs();
        return encode58(number);
    }

    public static String uuid16() {
        UUID        uuid = UUID.randomUUID();
        ByteBuffer  bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        BigInteger  number = new BigInteger(bb.array()).abs();
        return encode58(number);
    }

    public static String uuid8() {
        UUID        uuid = UUID.randomUUID();
        long        val = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        BigInteger  number = BigInteger.valueOf(val).abs();
        return encode58(number);
    }

}

