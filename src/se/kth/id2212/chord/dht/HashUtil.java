package se.kth.id2212.chord.dht;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    public final static BigInteger SHA_MAX = BigInteger.valueOf(1).shiftLeft(160);
    public final static String SHA_ALGO = "SHA-1";

    /*
     * return identifier of the input, corresponding to hash algorithm and
     * modulo to n
     */
    public static IKey hash(byte[] input, String algo, BigInteger n)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] key = md.digest(input);
        // System.out.println(key.length);
        return new KeyImpl(new BigInteger(key).abs().mod(n));
    }

    public static IKey hash(byte[] input) throws NoSuchAlgorithmException {
        return hash(input, SHA_ALGO, SHA_MAX);
    }

    public static IKey hash(byte[] input, BigInteger n) throws NoSuchAlgorithmException {
        return hash(input, SHA_ALGO, n);
    }

}
