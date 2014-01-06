package se.kth.id2212.chord.dht;

import java.io.Serializable;
import java.math.BigInteger;

public class KeyImpl implements IKey, Serializable {

    private static final long serialVersionUID = 6931375038737859789L;

    public static int KEY_LENGTH = 5;

    // LOG_BASE is k: search arity
    public static BigInteger LOG_BASE = new BigInteger("2");

    // the maximum key is LOG_BASE ^ KEY_LENGTH
    public static BigInteger KEY_MAX = LOG_BASE.pow(KEY_LENGTH);

    // the key itself
    private BigInteger key;

    public KeyImpl(BigInteger _key) {
        key = _key;
    }

    @Override
    public byte[] getBytes() {
        return key.toByteArray();
    }

    @Override
    public boolean equals(IKey another) {
        return key.equals(another.getKey());
    }

    @Override
    public BigInteger getKey() {
        return key;
    }

    @Override
    public String toHexString() {
        return key.toString(16);
    }

    @Override
    public String toDecString() {
        return key.toString(10);
    }

    @Override
    public boolean isBetween(IKey first, IKey last) {
        first = new KeyImpl(first.getKey().add(BigInteger.ONE).mod(KEY_MAX));
        if (first.equals(this) || last.equals(this))
            return true;

        if (first.comparedTo(last) < 0) {
            if (first.comparedTo(this) < 0 && this.comparedTo(last) < 0)
                return true;
        }

        if (first.comparedTo(last) > 0) {
            if (this.comparedTo(first) > 0 || this.comparedTo(last) < 0)
                return true;
        }

        return false;
    }

    @Override
    public int comparedTo(IKey another) {
        return this.key.compareTo(another.getKey());
    }

    @Override
    public IKey increase(BigInteger amount) {
        return new KeyImpl(this.key.add(amount).mod(KEY_MAX));
    }

    @Override
    public IKey decrease(BigInteger amount) {
        return new KeyImpl(this.key.subtract(amount).mod(KEY_MAX));
    }

    @Override
    public IKey increaseOne() {
        return this.increase(BigInteger.ONE);
    }

    @Override
    public IKey decreaseOne() {
        return this.decrease(BigInteger.ONE);
    }

}
