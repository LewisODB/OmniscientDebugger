package com.lambda.Debugger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

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
}
