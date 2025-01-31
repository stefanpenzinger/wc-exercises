/**
 * Root mean square of successive differences
 */
public class RMSSD {
    private final double[] heartRateVariabilityInterval;
    private double value;

    /**
     * Constructor
     * @param heartRateVariabilityInterval array of heart rate variability interval in milliseconds
     */
    public RMSSD(double[] heartRateVariabilityInterval) {
        this.heartRateVariabilityInterval = heartRateVariabilityInterval;
        value = Double.MIN_VALUE;
    }

    private double calculateRMSSD() {
        if (heartRateVariabilityInterval.length < 2) {
            throw new IllegalArgumentException("Heart rate variability interval must have at least 2 values");
        }

        var sumOfSquaredDiffs = 0.0;
        for (int i = 1; i < heartRateVariabilityInterval.length; i++) {
            var diff = heartRateVariabilityInterval[i] - heartRateVariabilityInterval[i - 1];
            sumOfSquaredDiffs += diff * diff;
        }

        return Math.sqrt(sumOfSquaredDiffs / (heartRateVariabilityInterval.length - 1));
    }

    /**
     * @return the value of the RMSSD
     */
    public double value() {
        if (value == Double.MIN_VALUE) {
            value = calculateRMSSD();
        }

        return value;
    }
}
