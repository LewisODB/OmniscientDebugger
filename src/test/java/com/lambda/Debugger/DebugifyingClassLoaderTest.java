package com.lambda.Debugger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DebugifyingClassLoaderTest {
    private VectorD originalInstrumentOnlyPackages;
    private VectorD originalDidntInstrument;

    @Before
    public void saveDefaults() {
        originalInstrumentOnlyPackages = Defaults.instrumentOnlyPackages;
        originalDidntInstrument = Defaults.didntInstrument;
        Defaults.instrumentOnlyPackages = new VectorD();
        Defaults.didntInstrument = new VectorD();
    }

    @After
    public void restoreDefaults() {
        Defaults.instrumentOnlyPackages = originalInstrumentOnlyPackages;
        Defaults.didntInstrument = originalDidntInstrument;
    }

    @Test
    public void emptyOnlyInstrumentKeepsLegacyClassloaderBehavior() {
        assertFalse(Defaults.instrumentOnlyExcludes("com.apple.laf.AquaLookAndFeel", true));
    }

    @Test
    public void packageOnlyInstrumentDelegatesClassesOutsidePackage() {
        Defaults.instrumentOnlyPackages.add("java_programs.");

        assertFalse(Defaults.instrumentOnlyExcludes("java_programs.GCDRunner", true));
        assertFalse(Defaults.instrumentOnlyExcludes("java_programs.extra.NESTED_PARENS", true));
        assertTrue(Defaults.instrumentOnlyExcludes("com.apple.laf.AquaLookAndFeel", true));
        assertTrue(Defaults.didntInstrument.contains("com.apple.laf.AquaLookAndFeel"));
    }

    @Test
    public void defaultPackageOnlyInstrumentDelegatesNamedPackages() {
        Defaults.instrumentOnlyPackages.add("");

        assertFalse(Defaults.instrumentOnlyExcludes("Main", true));
        assertTrue(Defaults.instrumentOnlyExcludes("helpers.Main", true));
    }

    @Test
    public void classPrefixOnlyInstrumentKeepsPublicifyBehavior() {
        Defaults.instrumentOnlyPackages.add("java_programs.GCD");

        assertFalse(Defaults.instrumentOnlyExcludes("java_programs.Helper", true));
        assertFalse(Defaults.instrumentOnlyExcludes("other.Helper", true));
        assertTrue(Defaults.instrumentOnlyExcludes("other.Helper", false));
    }

    @Test
    public void mixedOnlyInstrumentKeepsPublicifyBehavior() {
        Defaults.instrumentOnlyPackages.add("java_programs.");
        Defaults.instrumentOnlyPackages.add("other.Specific");

        assertFalse(Defaults.instrumentOnlyExcludes("helpers.Main", true));
    }

    @Test
    public void platformVendorClassesLoadThroughParent() throws Exception {
        DebugifyingClassLoader loader = new DebugifyingClassLoader();

        Class clazz = loader.loadClass("com.sun.java.swing.plaf.motif.MotifLookAndFeel");

        assertNotSame(loader, clazz.getClassLoader());
    }

    @Test
    public void instrumentationFailureKeepsLegacyParentFallback() throws Exception {
        DebugifyingClassLoader loader = new DebugifyingClassLoader() {
            protected Class findClass(String className, boolean instrument) {
                throw new VerifyError("fixture failure");
            }
        };

        Class clazz = loader.loadClass("outside.StrictFallbackTarget");

        assertSame(getClass().getClassLoader(), clazz.getClassLoader());
    }

    @Test
    public void arrayListAllocationUsesOdbRecordingImplementation() throws Exception {
        DebugifyingClassLoader loader = new DebugifyingClassLoader();

        Class target = loader.loadClass("outside.ArrayListRecordingTarget");
        Method mutate = target.getMethod("mutate");
        List values = (List) mutate.invoke(null);

        assertSame(loader, target.getClassLoader());
        assertEquals(MyArrayList.class, values.getClass());
        assertEquals(1, values.size());
        assertEquals("changed", values.get(0));
    }
}
