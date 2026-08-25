package com.mg.machineelementspro;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CalculationEngineTest {

    private static double numberAfter(String body, String label) {
        int i = body.indexOf(label + ":");
        if (i < 0) throw new AssertionError("Missing label: " + label + " in " + body);
        int start = i + label.length() + 1;
        while (start < body.length() && body.charAt(start) == ' ') start++;
        int end = start;
        while (end < body.length()) {
            char c = body.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') end++;
            else break;
        }
        return Double.parseDouble(body.substring(start, end));
    }

    private static void near(double expected, double actual, double relTol) {
        double scale = Math.max(1.0, Math.abs(expected));
        assertTrue("expected=" + expected + " actual=" + actual,
                Math.abs(expected - actual) <= relTol * scale);
    }

    @Test public void boltPureTensionMatchesAreaStress() {
        CalculationEngine.Result r = CalculationEngine.calculate(0, new double[]{10000, 0, 10, 250, 0, 0});
        near(127.32395, numberAfter(r.body, "Çekme gerilmesi"), 1e-4);
        near(127.32395, numberAfter(r.body, "von Mises"), 1e-4);
    }

    @Test public void shaftBendingMatchesClosedForm() {
        CalculationEngine.Result r = CalculationEngine.calculate(1, new double[]{100, 0, 20, 300, 0, 0});
        near(127.32395, numberAfter(r.body, "Eğilme gerilmesi"), 1e-4);
    }

    @Test public void bearingL10MatchesPowerLaw() {
        CalculationEngine.Result r = CalculationEngine.calculate(2, new double[]{10000, 5000, 1000, 3, 0, 0});
        near(8.0, numberAfter(r.body, "L10"), 1e-5);
        near(133.3333, numberAfter(r.body, "L10h"), 1e-4);
    }

    @Test public void gearLewisMatchesDefinition() {
        CalculationEngine.Result r = CalculationEngine.calculate(3, new double[]{1000, 20, 2, 0.3, 300, 0});
        near(83.333333, numberAfter(r.body, "Diş dibi eğilme"), 1e-4);
    }

    @Test public void springRateAndDeflectionAreConsistent() {
        CalculationEngine.Result r = CalculationEngine.calculate(4, new double[]{100, 30, 3, 8, 80000, 700});
        double k = numberAfter(r.body, "Yay katsayısı k");
        double delta = numberAfter(r.body, "Sehim");
        near(100.0, k * delta, 2e-3);
    }

    @Test public void keyShearAndCrushArePositive() {
        CalculationEngine.Result r = CalculationEngine.calculate(5, new double[]{100, 20, 6, 6, 30, 120});
        assertTrue(numberAfter(r.body, "Kesme gerilmesi") > 0);
        assertTrue(numberAfter(r.body, "Ezilme gerilmesi") > 0);
    }

    @Test public void filletWeldUsesEffectiveThroat() {
        CalculationEngine.Result r = CalculationEngine.calculate(6, new double[]{10000, 5, 100, 100, 0, 0});
        near(3.53553, numberAfter(r.body, "Etkin boğaz"), 1e-4);
        near(28.2843, numberAfter(r.body, "Kayma gerilmesi"), 1e-4);
    }

    @Test public void goodmanMatchesLinearRelation() {
        CalculationEngine.Result r = CalculationEngine.calculate(7, new double[]{100, 50, 200, 500, 0, 0});
        near(1.6666667, numberAfter(r.body, "Emniyet katsayısı"), 1e-4);
    }

    @Test public void pinDoubleShearHalvesShearStress() {
        CalculationEngine.Result single = CalculationEngine.calculate(8, new double[]{10000, 10, 5, 1, 200, 300});
        CalculationEngine.Result dbl = CalculationEngine.calculate(8, new double[]{10000, 10, 5, 2, 200, 300});
        near(numberAfter(single.body, "Pim kesme") / 2.0, numberAfter(dbl.body, "Pim kesme"), 1e-4);
    }

    @Test public void eulerCriticalLoadMatchesClosedForm() {
        CalculationEngine.Result r = CalculationEngine.calculate(9, new double[]{10000, 200000, 100000, 1000, 1, 2});
        double expected = Math.PI * Math.PI * 200000.0 * 100000.0 / 1000000.0;
        near(expected, numberAfter(r.body, "Kritik yük Pcr"), 2e-4);
    }

    @Test public void simplySupportedCenterLoadMatchesClosedForm() {
        CalculationEngine.Result r = CalculationEngine.calculate(10, new double[]{1000, 1000, 200000, 1000000, 10000, 250});
        near(250.0, numberAfter(r.body, "Maks. moment"), 1e-5);
        near(25.0, numberAfter(r.body, "Maks. eğilme"), 1e-5);
        near(0.1041667, numberAfter(r.body, "Orta nokta sehim"), 2e-4);
    }

    @Test public void torsionAngleMatchesClosedForm() {
        CalculationEngine.Result r = CalculationEngine.calculate(11, new double[]{100, 1000, 20, 80000, 5, 0});
        double j = Math.PI * Math.pow(20, 4) / 32.0;
        double expectedDeg = Math.toDegrees(100000.0 * 1000.0 / (j * 80000.0));
        near(expectedDeg, numberAfter(r.body, "Burulma açısı"), 2e-4);
    }

    @Test public void powerScrewProducesPositiveTorque() {
        CalculationEngine.Result r = CalculationEngine.calculate(12, new double[]{10000, 20, 4, 0.15, 30, 0.1});
        assertTrue(numberAfter(r.body, "Toplam tork") > 0);
        assertTrue(numberAfter(r.body, "Verim") > 0);
    }

    @Test public void threadStripAreaScalesWithEngagement() {
        CalculationEngine.Result a = CalculationEngine.calculate(13, new double[]{10000, 10, 10, 0.5, 100, 1.5});
        CalculationEngine.Result b = CalculationEngine.calculate(13, new double[]{10000, 10, 20, 0.5, 100, 1.5});
        near(2.0 * numberAfter(a.body, "Yaklaşık kesme alanı"), numberAfter(b.body, "Yaklaşık kesme alanı"), 1e-4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidPinPlaneCount() {
        CalculationEngine.calculate(8, new double[]{1000, 10, 5, 3, 100, 200});
    }
}
