package com.example.Smart_E_Commerce_System.algorithm;

import java.util.List;
import java.util.function.Function;

/**
 * Binary search: O(log n). The list must be sorted ascending by the search key.
 * Each step compares with the middle element and discards half of the remaining range.
 */
public final class Searching {

    private Searching() {}

    /** Returns the index of the element whose key equals {@code key}, or -1 if absent. */
    public static <T, K extends Comparable<? super K>> int binarySearch(
            List<T> sorted, K key, Function<? super T, ? extends K> keyOf) {
        int low = 0;
        int high = sorted.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int c = keyOf.apply(sorted.get(mid)).compareTo(key);
            if (c == 0) return mid;
            if (c < 0) low = mid + 1;   // key is in the right half
            else high = mid - 1;        // key is in the left half
        }
        return -1;
    }
}
