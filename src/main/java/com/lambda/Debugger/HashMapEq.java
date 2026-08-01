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
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** ODB's identity-based map compatibility type. */
public final class HashMapEq extends AbstractMap implements Map, Cloneable,
        Serializable {
    private static final long serialVersionUID = 362498820763181265L;
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("loadFactor", Float.TYPE),
        new ObjectStreamField("threshold", Integer.TYPE)
    };
    private static final int DEFAULT_CAPACITY = 101;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private IdentityHashMap delegate;
    private int initialCapacity;
    private float loadFactor;

    public HashMapEq(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(
                    "Illegal Initial Capacity: " + initialCapacity);
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException(
                    "Illegal Load factor: " + loadFactor);
        }
        this.initialCapacity = initialCapacity == 0 ? 1 : initialCapacity;
        this.loadFactor = loadFactor;
        delegate = new IdentityHashMap(initialCapacity);
    }

    public HashMapEq(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public HashMapEq() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public HashMapEq(Map source) {
        this(Math.max(2 * source.size(), 11), DEFAULT_LOAD_FACTOR);
        putAll(source);
    }

    public int size() { return delegate.size(); }
    public boolean isEmpty() { return delegate.isEmpty(); }
    public boolean containsValue(Object value) { return delegate.containsValue(value); }
    public boolean containsKey(Object key) { return delegate.containsKey(key); }
    public Object get(Object key) { return delegate.get(key); }
    public Object put(Object key, Object value) {
        if (!delegate.containsKey(key)
                && delegate.size() >= (int) (initialCapacity * loadFactor)) {
            initialCapacity = 2 * initialCapacity + 1;
        }
        return delegate.put(key, value);
    }
    public Object remove(Object key) { return delegate.remove(key); }
    public void putAll(Map source) {
        for (Object entryObject : source.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            put(entry.getKey(), entry.getValue());
        }
    }
    public void clear() { delegate.clear(); }

    public Object clone() {
        HashMapEq copy = new HashMapEq(initialCapacity, loadFactor);
        copy.delegate.putAll(delegate);
        return copy;
    }

    public Set keySet() { return delegate.keySet(); }
    public Collection values() { return delegate.values(); }
    public Set entrySet() { return delegate.entrySet(); }

    int capacity() { return initialCapacity; }
    float loadFactor() { return loadFactor; }

    public synchronized String toString() { return delegate.toString(); }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("loadFactor", loadFactor);
        fields.put("threshold", (int) (initialCapacity * loadFactor));
        stream.writeFields();
        stream.writeInt(initialCapacity);
        stream.writeInt(delegate.size());
        for (Object entryObject : delegate.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        loadFactor = fields.get("loadFactor", DEFAULT_LOAD_FACTOR);
        initialCapacity = stream.readInt();
        int size = stream.readInt();
        delegate = new IdentityHashMap(initialCapacity);
        for (int i = 0; i < size; i++) {
            put(stream.readObject(), stream.readObject());
        }
    }
}
