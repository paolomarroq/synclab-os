package com.synclabos.producerconsumer;

public record NumberItem(int value, NumberType type) {
    public static NumberItem fromValue(int value) {
        if (isPrime(value)) {
            return new NumberItem(value, NumberType.PRIME);
        }
        if (value % 2 == 0) {
            return new NumberItem(value, NumberType.EVEN);
        }
        return new NumberItem(value, NumberType.ODD);
    }

    private static boolean isPrime(int n) {
        if (n <= 1) {
            return false;
        }
        for (int divisor = 2; divisor * divisor <= n; divisor++) {
            if (n % divisor == 0) {
                return false;
            }
        }
        return true;
    }
}
