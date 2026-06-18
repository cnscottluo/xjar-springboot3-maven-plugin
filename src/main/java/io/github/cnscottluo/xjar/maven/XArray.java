package io.github.cnscottluo.xjar.maven;

public final class XArray {
    private XArray() {
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
}
