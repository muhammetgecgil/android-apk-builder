from pathlib import Path
import re

root = Path('unit-master')
build = root / 'app/build.gradle'
test_dir = root / 'app/src/test/java/com/mg/unitmasterx'

if not build.exists():
    raise SystemExit('unit-master/app/build.gradle not found')

b = build.read_text(encoding='utf-8')
b = re.sub(r'versionCode\s+\d+', 'versionCode 110', b)
b = re.sub(r"versionName\s+'[^']+'", "versionName '1.1.0'", b)
b = b.replace("applicationIdSuffix '.stable2'", "applicationIdSuffix '.rc110'")
b = b.replace("versionNameSuffix '-stable2'", "versionNameSuffix '-rc110'")
b = b.replace("manifestPlaceholders = [appLabel: 'Unit Master X Stable 2']", "manifestPlaceholders = [appLabel: 'Unit Master X RC 1.1']")

if 'minifyEnabled true' not in b:
    raise SystemExit('Release minification unexpectedly disabled')

build.write_text(b, encoding='utf-8')

test_dir.mkdir(parents=True, exist_ok=True)
quality_test = test_dir / 'ProductQualityTest.java'
quality_test.write_text(r'''package com.mg.unitmasterx;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public class ProductQualityTest {
    private static int unitOrMinusOne(ConverterData.Category c, String symbol) {
        if (c == null || c.units == null) return -1;
        for (int i = 0; i < c.units.size(); i++) {
            if (symbol.equals(c.units.get(i).symbol)) return i;
        }
        return -1;
    }

    private static int unit(ConverterData.Category c, String symbol) {
        int i = unitOrMinusOne(c, symbol);
        if (i >= 0) return i;
        fail("Missing unit " + symbol + " in " + (c == null ? "<null>" : c.name));
        return -1;
    }

    private static ConverterData.Category categoryWith(String... symbols) {
        for (ConverterData.Category c : ConverterData.CATEGORIES) {
            boolean all = true;
            for (String symbol : symbols) {
                if (unitOrMinusOne(c, symbol) < 0) { all = false; break; }
            }
            if (all) return c;
        }
        fail("Missing category containing units " + Arrays.toString(symbols));
        return null;
    }

    private static void reference(String from, String to,
                                  double input, double expected, double tolerance) {
        ConverterData.Category c = categoryWith(from, to);
        int f = unit(c, from), t = unit(c, to);
        double actual = ConverterData.convert(c, f, t, input);
        assertEquals(c.name + " " + from + "->" + to, expected, actual, tolerance);
    }

    @Test public void catalogHasProductScale() {
        assertTrue("Expected at least 50 categories", ConverterData.CATEGORIES.size() >= 50);
        int unitCount = 0;
        for (ConverterData.Category c : ConverterData.CATEGORIES) unitCount += c.units.size();
        assertTrue("Expected at least 350 units, got " + unitCount, unitCount >= 350);
    }

    @Test public void catalogMetadataIsComplete() {
        for (ConverterData.Category c : ConverterData.CATEGORIES) {
            assertNotNull(c);
            assertNotNull(c.name);
            assertFalse("Blank category name", c.name.trim().isEmpty());
            assertNotNull("Null unit list in " + c.name, c.units);
            assertFalse("Empty category: " + c.name, c.units.isEmpty());
            for (ConverterData.UnitDef u : c.units) {
                assertNotNull("Null unit in " + c.name, u);
                assertNotNull("Null unit name in " + c.name, u.name);
                assertNotNull("Null symbol in " + c.name, u.symbol);
                assertFalse("Blank unit name in " + c.name, u.name.trim().isEmpty());
                assertFalse("Blank symbol in " + c.name, u.symbol.trim().isEmpty());
            }
        }
    }

    @Test public void everyUnitIdentityConversionIsStable() {
        double[] values = {-1000000.0, -100.0, -1.0, 0.0, 1.0, 100.0, 1000000.0};
        for (ConverterData.Category c : ConverterData.CATEGORIES) {
            for (int i = 0; i < c.units.size(); i++) {
                for (double x : values) {
                    double y = ConverterData.convert(c, i, i, x);
                    assertEquals(c.name + " / " + c.units.get(i).symbol, x, y,
                            Math.max(1e-10, Math.abs(x) * 1e-12));
                }
            }
        }
    }

    @Test public void everyPairRoundTripsAcrossCatalog() {
        double[] values = {0.000001, 0.001, 1.0, 123.456789, 1000.0, 1000000.0};
        long operations = 0;
        for (ConverterData.Category c : ConverterData.CATEGORIES) {
            for (int from = 0; from < c.units.size(); from++) {
                for (int to = 0; to < c.units.size(); to++) {
                    for (double x : values) {
                        double y = ConverterData.convert(c, from, to, x);
                        assertTrue("Non-finite forward result: " + c.name + " " +
                                c.units.get(from).symbol + "->" + c.units.get(to).symbol,
                                Double.isFinite(y));
                        double z = ConverterData.convert(c, to, from, y);
                        assertTrue("Non-finite reverse result: " + c.name, Double.isFinite(z));
                        double tol = Math.max(1e-7, Math.abs(x) * 2e-9);
                        assertEquals("Round-trip: " + c.name + " " +
                                c.units.get(from).symbol + "<->" + c.units.get(to).symbol,
                                x, z, tol);
                        operations += 2;
                    }
                }
            }
        }
        assertTrue("Expected broad conversion coverage", operations > 10000);
        System.out.println("Unit Master X round-trip conversion operations=" + operations);
    }

    @Test public void engineeringReferenceValuesAreCorrect() {
        reference("m", "mm", 1.0, 1000.0, 1e-9);
        reference("N", "kgf", 500.0, 50.98581064889642, 1e-9);
        reference("psi", "bar", 100.0, 6.894757293168, 1e-9);
        reference("°C", "°F", 20.0, 68.0, 1e-9);
        reference("rpm", "rad/s", 60.0, 6.283185307179586, 1e-9);
    }
}
''', encoding='utf-8')

print('Applied Unit Master X v1.1.0 production quality patch')
