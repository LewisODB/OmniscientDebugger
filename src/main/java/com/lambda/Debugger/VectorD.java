 /*
 * Copyright 2003, Bil Lewis
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any later
 * version.
 */
package com.lambda.Debugger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.Collection;
import java.util.Vector;

/**
 * ODB vector compatibility type.
 *
 * Java's Vector owns storage and synchronization. ODB's historic reference-
 * identity search/removal and non-traversing debugger string are retained.
 */
public final class VectorD extends Vector {
    private static final long serialVersionUID = -2767605614048989439L;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("capacityIncrement", Integer.TYPE),
        new ObjectStreamField("elementCount", Integer.TYPE),
        new ObjectStreamField("id", Integer.TYPE),
        new ObjectStreamField("idCounter", Integer.TYPE),
        new ObjectStreamField("elementData", Object[].class)
    };

    protected int idCounter = 0;
    protected int id = idCounter++;
    private transient int legacyCapacityIncrement;

    public VectorD(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
        legacyCapacityIncrement = capacityIncrement;
    }

    public VectorD(int initialCapacity) {
        super(initialCapacity);
    }

    public VectorD() {
        super();
    }

    public VectorD(Collection collection) {
        super(collection);
    }

    public synchronized boolean contains(Object value) {
        return indexOf(value, 0) >= 0;
    }

    public synchronized int indexOf(Object value) {
        return indexOf(value, 0);
    }

    public synchronized int indexOf(Object value, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        for (int i = index; i < size(); i++) {
            if (elementAt(i) == value) {
                return i;
            }
        }
        return -1;
    }

    public synchronized int lastIndexOf(Object value) {
        return lastIndexOf(value, size() - 1);
    }

    public synchronized int lastIndexOf(Object value, int index) {
        if (index >= size()) {
            throw new IndexOutOfBoundsException(index + " >= " + size());
        }
        for (int i = index; i >= 0; i--) {
            if (elementAt(i) == value) {
                return i;
            }
        }
        return -1;
    }

    public synchronized boolean removeElement(Object value) {
        int index = indexOf(value);
        if (index < 0) {
            return false;
        }
        removeElementAt(index);
        return true;
    }

    public synchronized boolean remove(Object value) {
        return removeElement(value);
    }

    public synchronized boolean containsAll(Collection collection) {
        for (Object value : collection) {
            if (!containsEqual(value)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean removeAll(Collection collection) {
        boolean changed = false;
        for (int i = size() - 1; i >= 0; i--) {
            if (collection.contains(elementAt(i))) {
                removeElementAt(i);
                changed = true;
            }
        }
        return changed;
    }

    public synchronized boolean retainAll(Collection collection) {
        boolean changed = false;
        for (int i = size() - 1; i >= 0; i--) {
            if (!collection.contains(elementAt(i))) {
                removeElementAt(i);
                changed = true;
            }
        }
        return changed;
    }

    private boolean containsEqual(Object target) {
        for (Object value : this) {
            if (target == null ? value == null : target.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "<Vector_" + id + ">";
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        Object[] elements = new Object[capacity()];
        copyInto(elements);
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("capacityIncrement", legacyCapacityIncrement);
        fields.put("elementCount", size());
        fields.put("id", id);
        fields.put("idCounter", idCounter);
        fields.put("elementData", elements);
        stream.writeFields();
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        legacyCapacityIncrement = fields.get("capacityIncrement", 0);
        int elementCount = fields.get("elementCount", 0);
        id = fields.get("id", 0);
        idCounter = fields.get("idCounter", 0);
        Object[] elements = (Object[]) fields.get("elementData", null);
        if (elements == null) {
            return;
        }
        clear();
        ensureCapacity(elements.length);
        for (int i = 0; i < elementCount; i++) {
            add(elements[i]);
        }
    }
}
