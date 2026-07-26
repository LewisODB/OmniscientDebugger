 /*
 * Copyright 2003, Bil Lewis
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any later
 * version.
 */
package com.lambda.Debugger;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/** Retained ODB compatibility facade over {@link Collections}. */
public class MyCollections {
    public static final Set EMPTY_SET = Collections.EMPTY_SET;
    public static final List EMPTY_LIST = Collections.EMPTY_LIST;

    private MyCollections() {
    }

    public static void sort(List list) { Collections.sort(list); }
    public static void sort(List list, Comparator comparator) {
        Collections.sort(list, comparator);
    }
    public static int binarySearch(List list, Object key) {
        return Collections.binarySearch(list, key);
    }
    public static int binarySearch(List list, Object key,
            Comparator comparator) {
        return Collections.binarySearch(list, key, comparator);
    }
    public static void reverse(List list) { Collections.reverse(list); }
    public static void shuffle(List list) { Collections.shuffle(list); }
    public static void shuffle(List list, Random random) {
        Collections.shuffle(list, random);
    }
    public static void fill(List list, Object value) {
        Collections.fill(list, value);
    }
    public static void copy(List destination, List source) {
        Collections.copy(destination, source);
    }
    public static Object min(Collection collection) {
        return Collections.min(collection);
    }
    public static Object min(Collection collection, Comparator comparator) {
        return Collections.min(collection, comparator);
    }
    public static Object max(Collection collection) {
        return Collections.max(collection);
    }
    public static Object max(Collection collection, Comparator comparator) {
        return Collections.max(collection, comparator);
    }
    public static Collection unmodifiableCollection(Collection collection) {
        return Collections.unmodifiableCollection(collection);
    }
    public static Set unmodifiableSet(Set set) {
        return Collections.unmodifiableSet(set);
    }
    public static SortedSet unmodifiableSortedSet(SortedSet set) {
        return Collections.unmodifiableSortedSet(set);
    }
    public static List unmodifiableList(List list) {
        return Collections.unmodifiableList(list);
    }
    public static Map unmodifiableMap(Map map) {
        return Collections.unmodifiableMap(map);
    }
    public static SortedMap unmodifiableSortedMap(SortedMap map) {
        return Collections.unmodifiableSortedMap(map);
    }
    public static Collection synchronizedCollection(Collection collection) {
        return Collections.synchronizedCollection(collection);
    }
    public static Set synchronizedSet(Set set) {
        return Collections.synchronizedSet(set);
    }
    public static SortedSet synchronizedSortedSet(SortedSet set) {
        return Collections.synchronizedSortedSet(set);
    }
    public static List synchronizedList(List list) {
        return Collections.synchronizedList(list);
    }
    public static Map synchronizedMap(Map map) {
        return Collections.synchronizedMap(map);
    }
    public static SortedMap synchronizedSortedMap(SortedMap map) {
        return Collections.synchronizedSortedMap(map);
    }
    public static Set singleton(Object value) {
        return Collections.singleton(value);
    }
    public static List nCopies(int count, Object value) {
        return Collections.nCopies(count, value);
    }
    public static Comparator reverseOrder() {
        return Collections.reverseOrder();
    }
    public static Enumeration enumeration(Collection collection) {
        return Collections.enumeration(collection);
    }
}
