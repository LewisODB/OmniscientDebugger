package com.lambda.Debugger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JList;

import org.junit.Test;

public class CollectionCompatibilityTest {
    private static final String LEGACY_VECTOR_STREAM =
            "rO0ABXNyABtjb20ubGFtYmRhLkRlYnVnZ2VyLlZlY3RvckTZl31bgDuvAQIABUkA"
            + "EWNhcGFjaXR5SW5jcmVtZW50SQAMZWxlbWVudENvdW50SQACaWRJAAlpZENvdW50"
            + "ZXJbAAtlbGVtZW50RGF0YXQAE1tMamF2YS9sYW5nL09iamVjdDt4cgAQamF2YS51"
            + "dGlsLlZlY3RvctmXfVuAO68BAwADSQARY2FwYWNpdHlJbmNyZW1lbnRJAAxlbGVt"
            + "ZW50Q291bnRbAAtlbGVtZW50RGF0YXEAfgABeHAAAAAAAAAAAHVyABNbTGphdmEu"
            + "bGFuZy5PYmplY3Q7kM5YnxBzKWwCAAB4cAAAAApwcHBwcHBwcHBweAAAAAAAAAAC"
            + "AAAAAAAAAAF1cQB+AAQAAAAKdAABYXBwcHBwcHBwcA==";
    private static final String LEGACY_HASH_MAP_STREAM =
            "rO0ABXNyAB1jb20ubGFtYmRhLkRlYnVnZ2VyLkhhc2hNYXBFcQUH2sHDFmDRAwAC"
            + "RgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAEt3CAAAAGUAAAACdAAB"
            + "a3QAAXZwcHg=";

    @Test
    public void hashMapEqUsesReferenceIdentityForKeysAndValues() {
        HashMapEq map = new HashMapEq(1);
        String firstKey = new String("key");
        String secondKey = new String("key");
        String firstValue = new String("value");
        String secondValue = new String("value");

        assertNull(map.put(firstKey, firstValue));
        assertNull(map.put(secondKey, secondValue));

        assertEquals(2, map.size());
        assertSame(firstValue, map.get(firstKey));
        assertSame(secondValue, map.get(secondKey));
        assertNull(map.get(new String("key")));
        assertTrue(map.containsValue(firstValue));
        assertFalse(map.containsValue(new String("value")));
    }

    @Test
    public void hashMapEqSupportsNullBackedViewsAndCloneIsolation() {
        HashMapEq map = new HashMapEq();
        Object key = new Object();
        Object value = new Object();
        map.put(null, null);
        map.put(key, value);

        assertTrue(map.keySet().contains(null));
        assertTrue(map.values().contains(value));

        Iterator entries = map.entrySet().iterator();
        entries.next();
        entries.remove();
        assertEquals(1, map.size());

        HashMapEq clone = (HashMapEq) map.clone();
        Object cloneOnly = new Object();
        clone.put(cloneOnly, cloneOnly);
        assertEquals(1, map.size());
        assertEquals(2, clone.size());
        assertNull(map.get(cloneOnly));
    }

    @Test
    public void hashMapEqRetainsLegacyNanLoadFactorAcceptance() {
        HashMapEq map = new HashMapEq(1, Float.NaN);
        Object key = new Object();
        map.put(key, key);
        assertSame(key, map.get(key));
    }

    @Test(expected = ConcurrentModificationException.class)
    public void hashMapEqViewsRemainFailFast() {
        HashMapEq map = new HashMapEq();
        map.put(new Object(), new Object());
        Iterator keys = map.keySet().iterator();

        map.put(new Object(), new Object());

        keys.next();
    }

    @Test
    public void hashMapEqReadsLegacySerializedState() throws Exception {
        HashMapEq map = (HashMapEq) readLegacyStream(LEGACY_HASH_MAP_STREAM);

        assertEquals(2, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        for (Object key : map.keySet()) {
            if (key != null) {
                assertEquals("k", key);
                assertEquals("v", map.get(key));
            }
        }
    }

    @Test
    public void rewrittenCollectionsRoundTripSerializedState() throws Exception {
        Object key = new String("key");
        Object value = new String("value");
        HashMapEq map = new HashMapEq(1, 0.75f);
        map.put(key, value);
        map.put(null, null);
        assertEquals(3, map.capacity());
        assertEquals(0.75f, map.loadFactor(), 0.0f);

        HashMapEq mapCopy = (HashMapEq) roundTrip(map);
        assertEquals(2, mapCopy.size());
        assertTrue(mapCopy.containsKey(null));
        assertTrue(mapCopy.containsValue(null));

        VectorD vector = new VectorD(1, 3);
        vector.add("value");
        vector.add(null);
        VectorD vectorCopy = (VectorD) roundTrip(vector);
        assertEquals(2, vectorCopy.size());
        assertEquals("value", vectorCopy.elementAt(0));
        assertNull(vectorCopy.elementAt(1));
    }

    @Test
    public void vectorDPreservesLegacyOperationsAndIdentitySearch() {
        VectorD vector = new VectorD(1, 1);
        String first = new String("same");
        String equalButDistinct = new String("same");

        vector.add(first);
        vector.insertElementAt(equalButDistinct, 0);
        vector.add(1, null);

        assertEquals(3, vector.size());
        assertSame(equalButDistinct, vector.firstElement());
        assertSame(first, vector.lastElement());
        assertEquals(2, vector.indexOf(first));
        assertEquals(0, vector.indexOf(equalButDistinct));
        assertEquals(-1, vector.indexOf(new String("same")));
        assertFalse(vector.contains(new String("same")));

        vector.setElementAt(first, 1);
        assertSame(first, vector.elementAt(1));
        assertTrue(vector.removeElement(equalButDistinct));
        assertEquals(2, vector.size());
        assertSame(first, vector.remove(0));
        assertEquals(1, vector.size());
        vector.removeAllElements();
        assertTrue(vector.isEmpty());
    }

    @Test
    public void vectorDEnumerationAndStringDoNotTraverseElementStrings() {
        Object explosive = new Object() {
            public String toString() {
                throw new AssertionError("element toString must not be called");
            }
        };
        VectorD vector = new VectorD(Arrays.asList(explosive));

        Enumeration elements = vector.elements();
        assertTrue(elements.hasMoreElements());
        assertSame(explosive, elements.nextElement());
        assertFalse(elements.hasMoreElements());
        assertEquals("<Vector_0>", vector.toString());
    }

    @Test
    public void vectorDRepairUnifiesStandardViewsWithLegacyStorage() {
        Object first = new Object();
        Object second = new Object();
        VectorD vector = new VectorD(Arrays.asList(first, second));

        assertArrayEquals(new Object[] {first, second}, vector.toArray());
        assertSame(first, vector.iterator().next());
        assertSame(second, vector.listIterator(1).next());
        assertEquals(Arrays.asList(first, second), vector.subList(0, 2));
    }

    @Test
    public void vectorDRepairMakesCloneStorageIndependent() {
        Object originalValue = new Object();
        Object replacement = new Object();
        VectorD original = new VectorD(Arrays.asList(originalValue));

        VectorD clone = (VectorD) original.clone();
        original.setElementAt(replacement, 0);

        assertNotSame(original, clone);
        assertSame(originalValue, clone.elementAt(0));
        assertSame(replacement, original.elementAt(0));
    }

    @Test
    public void vectorDRepairMakesBulkOperationsUsePopulatedStorage() {
        String retained = new String("same");
        VectorD vector = new VectorD(Arrays.asList(retained));

        assertTrue(vector.containsAll(Arrays.asList(new String("same"))));
        assertFalse(vector.remove(new String("same")));
        assertTrue(vector.removeAll(Arrays.asList(new String("same"))));
        assertTrue(vector.isEmpty());

        vector.add(retained);
        assertFalse(vector.retainAll(Arrays.asList(new String("same"))));
        assertSame(retained, vector.firstElement());
    }

    @Test
    public void vectorDReadsLegacySerializedState() throws Exception {
        VectorD vector = (VectorD) readLegacyStream(LEGACY_VECTOR_STREAM);

        assertEquals(2, vector.size());
        assertEquals("a", vector.elementAt(0));
        assertNull(vector.elementAt(1));
    }

    @Test
    public void retainedAbstractCollectionsProvideStandardOperations() {
        MutableCollection collection = new MutableCollection();
        collection.add("a");
        collection.add(null);
        assertTrue(collection.contains("a"));
        assertArrayEquals(new Object[] {"a", null}, collection.toArray());
        assertTrue(collection.remove(null));

        MutableList list = new MutableList();
        list.add("b");
        list.add(0, "a");
        list.add("c");
        assertEquals(Arrays.asList("a", "b", "c"), list);
        assertEquals(1, list.indexOf("b"));
        assertEquals("b", list.set(1, "B"));
        assertEquals(Arrays.asList("a", "B", "c"), list.subList(0, 3));
        assertEquals("B", list.remove(1));
        assertEquals(Arrays.asList("a", "c"), list);
    }

    @Test
    public void retainedCollectionsFacadeMatchesJdkAlgorithmsAndWrappers() {
        List values = new ArrayList(Arrays.asList(3, 1, 2));
        MyCollections.sort(values);
        assertEquals(Arrays.asList(1, 2, 3), values);
        assertEquals(1, MyCollections.binarySearch(values, 2));
        MyCollections.reverse(values);
        assertEquals(Arrays.asList(3, 2, 1), values);

        List immutable = MyCollections.unmodifiableList(values);
        assertEquals(values, immutable);
        assertEquals(Arrays.asList("x", "x"), MyCollections.nCopies(2, "x"));
        assertTrue(MyCollections.singleton("x").contains("x"));

        Enumeration enumeration = MyCollections.enumeration(values);
        assertEquals(3, enumeration.nextElement());
        assertEquals(2, enumeration.nextElement());
        assertEquals(1, enumeration.nextElement());
        assertFalse(enumeration.hasMoreElements());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void retainedAbstractListIteratorDetectsDirectStructuralChange() {
        MutableList list = new MutableList();
        list.add("first");
        Iterator iterator = list.iterator();

        list.add("second");

        iterator.next();
    }

    @Test(expected = ConcurrentModificationException.class)
    public void retainedSubListDetectsParentStructuralChange() {
        MutableList list = new MutableList();
        list.add("first");
        List subList = list.subList(0, 1);

        list.add("second");

        subList.size();
    }

    @Test
    public void vectorDBacksRealStackAndSwingListModels() {
        Object frame = new Object();
        StackList stack = new StackList();
        stack.displayList.add(frame);
        assertEquals(1, stack.getSize());
        assertSame(frame, stack.getElementAt(0));

        VectorD values = new VectorD(Arrays.asList("first", "second"));
        JList list = new JList(values);
        assertEquals(2, list.getModel().getSize());
        assertEquals("first", list.getModel().getElementAt(0));
        assertEquals("second", list.getModel().getElementAt(1));
    }

    private static Object readLegacyStream(String encoded) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ObjectInputStream input =
                new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return input.readObject();
        } finally {
            input.close();
        }
    }

    private static Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);
        output.writeObject(value);
        output.close();
        ObjectInputStream input =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        try {
            return input.readObject();
        } finally {
            input.close();
        }
    }

    private static final class MutableCollection extends MyAbstractCollection {
        private final Collection delegate = new ArrayList();

        public Iterator iterator() {
            return delegate.iterator();
        }

        public int size() {
            return delegate.size();
        }

        public boolean add(Object value) {
            return delegate.add(value);
        }
    }

    private static final class MutableList extends MyAbstractList {
        private final List delegate = new ArrayList();

        public Object get(int index) {
            return delegate.get(index);
        }

        public int size() {
            return delegate.size();
        }

        public Object set(int index, Object value) {
            return delegate.set(index, value);
        }

        public void add(int index, Object value) {
            delegate.add(index, value);
            modCount++;
        }

        public Object remove(int index) {
            Object removed = delegate.remove(index);
            modCount++;
            return removed;
        }
    }
}
