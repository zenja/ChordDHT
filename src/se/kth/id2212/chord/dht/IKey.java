package se.kth.id2212.chord.dht;

import java.math.BigInteger;

public interface IKey {

    public byte[] getBytes();

    public boolean equals(IKey another);

    public BigInteger getKey();

    public String toHexString();

    public String toDecString();

    public int comparedTo(IKey another);

    // return true if key is between (first,last]
    public boolean isBetween(IKey first, IKey last);

    // return key + amount mod KEY_MAX
    public IKey increase(BigInteger amount);

    // return key - amount mod KEY_MAX
    public IKey decrease(BigInteger amount);

    // return key + 1 mod KEY_MAX
    public IKey increaseOne();

    // return key - 1 mod KEY_MAX
    public IKey decreaseOne();
}
