package com.urlshortener.service;

final class FeistelCodec {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final int bits, halfBits, rounds, width;
    private final long key, halfMask, domainMask;

    FeistelCodec(int bits, int rounds, long key) {
        if (bits <= 0 || bits > 62 || (bits & 1) != 0)
            throw new IllegalArgumentException("bits must be positive, even, <= 62");
        if (rounds <= 0) throw new IllegalArgumentException("rounds must be positive");
        this.bits = bits;
        this.halfBits = bits / 2;
        this.rounds = rounds;
        this.key = key;
        this.halfMask = (1L << halfBits) - 1;
        this.domainMask = (1L << bits) - 1;
        long domain = 1L << bits, p = 1;
        int w = 0;
        while (p < domain) {
            if (p > Long.MAX_VALUE / 62) {
                w++;
                break;
            }
            p *= 62;
            w++;
        }
        this.width = w;
    }

    long permute(long value) {
        if (value < 0 || value > domainMask) throw new IllegalArgumentException("outside domain");
        long left = (value >>> halfBits) & halfMask, right = value & halfMask;
        for (int round = 0; round < rounds; round++) {
            long nextLeft = right;
            long nextRight = left ^ f(right, round);
            left = nextLeft;
            right = nextRight;
        }
        return ((left & halfMask) << halfBits) | (right & halfMask);
    }

    String encode(long value) {
        char[] out = new char[width];
        long v = value;
        for (int i = width - 1; i >= 0; i--) {
            out[i] = ALPHABET.charAt((int) (v % 62));
            v /= 62;
        }
        return new String(out);
    }

    int width() {
        return width;
    }

    private long f(long r, int round) {
        long x = r + key + ((long) round << 32);
        x *= 0x9E3779B97F4A7C15L;
        x ^= x >>> 29;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 32;
        return x & halfMask;
    }
}
