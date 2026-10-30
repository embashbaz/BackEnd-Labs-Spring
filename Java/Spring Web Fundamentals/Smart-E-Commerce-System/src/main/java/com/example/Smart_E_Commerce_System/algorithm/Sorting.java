package com.example.Smart_E_Commerce_System.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merge sort: O(n log n) time, O(n) extra space, stable.
 * Splits the list in half, sorts each half, then merges the two sorted halves.
 */
public final class Sorting {

    private Sorting() {}

    public static <T> List<T> mergeSort(List<T> list, Comparator<? super T> cmp) {
        if (list.size() <= 1) {
            return new ArrayList<>(list);
        }
        int mid = list.size() / 2;
        List<T> left = mergeSort(list.subList(0, mid), cmp);
        List<T> right = mergeSort(list.subList(mid, list.size()), cmp);
        return merge(left, right, cmp);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<? super T> cmp) {
        List<T> result = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            // "<=" takes from the left on ties, which keeps the sort stable
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        while (i < left.size()) result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        return result;
    }
}
