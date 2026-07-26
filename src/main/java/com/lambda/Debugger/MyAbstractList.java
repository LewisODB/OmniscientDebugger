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
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/** Retained ODB compatibility name for an indexed list skeleton. */
public abstract class MyAbstractList extends MyAbstractCollection
        implements List {
    protected transient int modCount = 0;

    protected MyAbstractList() {
    }

    public abstract Object get(int index);

    public boolean add(Object value) {
        add(size(), value);
        return true;
    }

    public Object set(int index, Object value) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object value) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    public int indexOf(Object value) {
        ListIterator iterator = listIterator();
        while (iterator.hasNext()) {
            Object candidate = iterator.next();
            if (value == null ? candidate == null : value.equals(candidate)) {
                return iterator.previousIndex();
            }
        }
        return -1;
    }

    public int lastIndexOf(Object value) {
        ListIterator iterator = listIterator(size());
        while (iterator.hasPrevious()) {
            Object candidate = iterator.previous();
            if (value == null ? candidate == null : value.equals(candidate)) {
                return iterator.nextIndex();
            }
        }
        return -1;
    }

    public void clear() {
        removeRange(0, size());
    }

    public boolean addAll(int index, Collection collection) {
        rangeCheckForAdd(index);
        boolean changed = false;
        for (Object value : collection) {
            add(index++, value);
            changed = true;
        }
        return changed;
    }

    public Iterator iterator() {
        return listIterator();
    }

    public ListIterator listIterator() {
        return listIterator(0);
    }

    public ListIterator listIterator(final int index) {
        rangeCheckForAdd(index);
        return new ListIterator() {
            private int cursor = index;
            private int lastReturned = -1;
            private int expectedModCount = modCount;

            public boolean hasNext() { return cursor < size(); }
            public boolean hasPrevious() { return cursor > 0; }
            public int nextIndex() { return cursor; }
            public int previousIndex() { return cursor - 1; }

            public Object next() {
                checkForComodification();
                if (!hasNext()) throw new NoSuchElementException();
                Object value = get(cursor);
                lastReturned = cursor++;
                return value;
            }

            public Object previous() {
                checkForComodification();
                if (!hasPrevious()) throw new NoSuchElementException();
                Object value = get(--cursor);
                lastReturned = cursor;
                return value;
            }

            public void remove() {
                checkForComodification();
                if (lastReturned < 0) throw new IllegalStateException();
                MyAbstractList.this.remove(lastReturned);
                if (lastReturned < cursor) cursor--;
                lastReturned = -1;
                expectedModCount = modCount;
            }

            public void set(Object value) {
                checkForComodification();
                if (lastReturned < 0) throw new IllegalStateException();
                MyAbstractList.this.set(lastReturned, value);
            }

            public void add(Object value) {
                checkForComodification();
                MyAbstractList.this.add(cursor++, value);
                lastReturned = -1;
                expectedModCount = modCount;
            }

            private void checkForComodification() {
                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        };
    }

    public List subList(int fromIndex, int toIndex) {
        return new SubList(this, fromIndex, toIndex);
    }

    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof List)) return false;
        Iterator left = iterator();
        Iterator right = ((List) other).iterator();
        while (left.hasNext() && right.hasNext()) {
            Object a = left.next();
            Object b = right.next();
            if (!(a == null ? b == null : a.equals(b))) return false;
        }
        return !left.hasNext() && !right.hasNext();
    }

    public int hashCode() {
        int hash = 1;
        for (Object value : this) {
            hash = 31 * hash + (value == null ? 0 : value.hashCode());
        }
        return hash;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator iterator = listIterator(fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            iterator.next();
            iterator.remove();
        }
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException(
                    "Index: " + index + ", Size: " + size());
        }
    }
}

/** Retained package-level compatibility type for historic callers. */
class SubList extends MyAbstractList {
    private final MyAbstractList parent;
    private final int offset;
    private int length;
    private int expectedParentModCount;

    SubList(MyAbstractList list, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > list.size()) {
            throw new IndexOutOfBoundsException();
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        parent = list;
        offset = fromIndex;
        length = toIndex - fromIndex;
        expectedParentModCount = parent.modCount;
    }

    public Object get(int index) {
        rangeCheck(index);
        checkForComodification();
        return parent.get(offset + index);
    }

    public int size() {
        checkForComodification();
        return length;
    }

    public Object set(int index, Object value) {
        rangeCheck(index);
        checkForComodification();
        return parent.set(offset + index, value);
    }

    public void add(int index, Object value) {
        if (index < 0 || index > length) throw new IndexOutOfBoundsException();
        checkForComodification();
        parent.add(offset + index, value);
        length++;
        modified();
    }

    public Object remove(int index) {
        rangeCheck(index);
        checkForComodification();
        Object removed = parent.remove(offset + index);
        length--;
        modified();
        return removed;
    }

    public boolean addAll(Collection collection) {
        return addAll(length, collection);
    }

    public boolean addAll(int index, Collection collection) {
        if (index < 0 || index > length) throw new IndexOutOfBoundsException();
        checkForComodification();
        if (collection.isEmpty()) return false;
        parent.addAll(offset + index, collection);
        length += collection.size();
        modified();
        return true;
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
    }

    private void checkForComodification() {
        if (parent.modCount != expectedParentModCount) {
            throw new ConcurrentModificationException();
        }
    }

    private void modified() {
        expectedParentModCount = parent.modCount;
        modCount++;
    }
}
