package at.hagenberg.fh.wc.helper;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class PolynomialHelper {

    private PolynomialHelper() {
        throw new IllegalStateException("Utility class");
    }

    private static double polynomialValue(double[] coefficients, double x) {
        double result = 0;
        int degree = coefficients.length - 1;

        for (int i = 0; i <= degree; i++) {
            result += coefficients[i] * Math.pow(x, degree - i);
        }
        return result;
    }

    private static double negativePolynomialValue(double[] coefficients, double x) {
        return -polynomialValue(coefficients, x);
    }

    // Golden Section Search for minimization in the interval [a, b]
    public static double goldenSectionSearch(double[] coefficients, double a, double b, double tol) {
        final var gr = (Math.sqrt(5) + 1) / 2;

        var c = b - (b - a) / gr;
        var d = a + (b - a) / gr;

        while (Math.abs(c - d) > tol) {
            if (negativePolynomialValue(coefficients, c) < negativePolynomialValue(coefficients, d)) {
                b = d;
            } else {
                a = c;
            }
            c = b - (b - a) / gr;
            d = a + (b - a) / gr;
        }

        // Return the midpoint which is the best approximation of the minimum
        return (b + a) / 2;
    }

    /**
     * Returns the coefficients to fit a polynomial curve based on the magnitude
     *
     * @param magnitude The magnitude of the signal
     * @return The coefficients of the polynomial curve
     */
    public static double[] fit(double[] magnitude) {
        double[] xValues = {1, 2, 3, 4, 5};
        double[] yValues = {magnitude[2], magnitude[3], magnitude[4], magnitude[5], magnitude[6]};
        var degree = 4;

        var obs = new WeightedObservedPoints();
        for (int i = 0; i < xValues.length; i++) {
            obs.add(xValues[i], yValues[i]);
        }
        var fitter = PolynomialCurveFitter.create(degree);

        return reverseArray(fitter.fit(obs.toList()));
    }

    /**
     * Reverses an array
     *
     * @param array The array to be reversed
     * @return The reversed array
     */
    private static double[] reverseArray(double[] array) {
        var left = 0;
        var right = array.length - 1;

        // Swap elements from the start and the end, moving towards the center
        while (left < right) {
            double temp = array[left];
            array[left] = array[right];
            array[right] = temp;

            // Move the pointers
            left++;
            right--;
        }

        return array;
    }
}
