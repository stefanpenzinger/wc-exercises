import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

/**
 * Done by ChatGPT
 */
public class WelchResult {
    private static final double LOW_FREQUENCY_LOWER_BOUND = 0.04;
    private static final double LOW_FREQUENCY_UPPER_BOUND = 0.15;
    private static final double HIGH_FREQUENCY_LOWER_BOUND = 0.15;
    private static final double HIGH_FREQUENCY_UPPER_BOUND = 0.4;

    private final double[] freqs;
    private final double[] psd;
    private final List<Double> lfFreq = new ArrayList<>();
    private final List<Double> hfFreq = new ArrayList<>();
    private final List<Double> lfPsd = new ArrayList<>();
    private final List<Double> hfPsd = new ArrayList<>();


    private WelchResult() {
        this.freqs = new double[0];
        this.psd = new double[0];
    }

    private WelchResult(double[] frequencies, double[] psd) {
        this.freqs = frequencies;
        this.psd = psd;
        calculateFrequencyAndPower();
    }

    /**
     * Calculates the welch result from the given HRV data.
     *
     * @param hrvData HRV data
     * @return WelchResult
     */
    public static WelchResult calculateWelchResult(double[] hrvData) {
        double[] t = new double[hrvData.length];
        var cum = 0.0;
        for (int i = 0; i < hrvData.length; i++) {
            cum += hrvData[i];
            t[i] = cum;
        }
        // Shift so that time starts at 0
        var t0 = t[0];
        for (int i = 0; i < t.length; i++) {
            t[i] -= t0;
        }

        // 2) Interpolate onto a uniform timeline
        //    Python code uses fs=4 Hz => 250 ms per sample
        //    So let's do the same for demonstration
        var fsHz = 4.0; // sampling freq
        var dtMs = 1000.0 / fsHz; // 250 ms

        var tEnd = t[t.length - 1];
        var tUniformList = new ArrayList<Double>();
        for (double tau = 0.0; tau < tEnd; tau += dtMs) {
            tUniformList.add(tau);
        }
        var tUniform = tUniformList.stream().mapToDouble(Double::doubleValue).toArray();

        // Interpolate the HRV signal
        // We'll do simple linear interpolation
        var hrvUniform = new double[tUniform.length];
        for (int i = 0; i < tUniform.length; i++) {
            hrvUniform[i] = linearInterpolate(t, hrvData, tUniform[i]);
        }

        return welch(hrvUniform, fsHz);
    }

    /**
     * Linear interpolation helper.
     * x[] = sorted domain
     * y[] = values
     * xi = point to interpolate
     * returns yi
     */
    private static double linearInterpolate(double[] x, double[] y, double xi) {
        // If xi is out of bounds, clamp
        if (xi <= x[0]) {
            return y[0];
        }
        if (xi >= x[x.length - 1]) {
            return y[x.length - 1];
        }
        // find i s.t. x[i] <= xi < x[i+1]
        var i = 0;
        while (i < x.length - 1 && x[i + 1] < xi) {
            i++;
        }
        var x0 = x[i];
        var x1 = x[i + 1];
        var y0 = y[i];
        var y1 = y[i + 1];
        if (x1 == x0) {
            return y0;
        }
        var frac = (xi - x0) / (x1 - x0);
        return y0 + frac * (y1 - y0);
    }

    /**
     * Basic implementation of Welch's method using a Hamming window and FFT.
     * Done by ChatGPT
     *
     * @param data time-series data
     * @param fs   sampling frequency in Hz
     * @return WelchResult with frequencies (Hz) and PSD values
     */
    private static WelchResult welch(double[] data, double fs) {
        // Typically you also choose a segment size (e.g. nperseg) and overlap,
        // average the resulting PSDs, etc. For brevity, we do a single window approach.

        // Make a copy of data up to nfft if needed
        var nfft = 4096;

        double[] x;
        if (data.length < nfft) {
            // zero-pad
            x = new double[nfft];
            System.arraycopy(data, 0, x, 0, data.length);
        } else if (data.length > nfft) {
            // just truncate
            x = new double[nfft];
            System.arraycopy(data, 0, x, 0, nfft);
        } else {
            x = data.clone();
        }

        // Apply a Hamming window
        for (int i = 0; i < x.length; i++) {
            x[i] = x[i] * hamming(i, x.length);
        }

        // Perform FFT
        var fft = new FastFourierTransformer(DftNormalization.STANDARD);
        // real input => forward transform
        // we get complex results
        org.apache.commons.math3.complex.Complex[] fftResult = fft.transform(x, TransformType.FORWARD);

        int half = nfft / 2;
        double[] freqs = new double[half];
        double[] psd = new double[half];

        // Frequency resolution
        double df = fs / nfft;

        // Compute power for each bin
        for (int i = 0; i < half; i++) {
            double re = fftResult[i].getReal();
            double im = fftResult[i].getImaginary();
            double mag = (re * re + im * im);
            // Scale factor for windowing function, etc.
            // Typically: PSD = (2 * mag / (fs * sum(window^2))) for i in [1..half-1]
            // The factor of 2 is because we use single-sided PSD
            // For a Hamming window, sum(window^2) can be computed. We'll approximate:

            var isNotZero = fs * windowPowerHamming(x.length);
            if (isNotZero == 0) {
                throw new IllegalArgumentException("Window power is zero");
            }

            psd[i] = 2.0 * mag / isNotZero;
            freqs[i] = i * df; // in Hz
        }

        return new WelchResult(freqs, psd);
    }

    /**
     * Hamming window function: w(n) = 0.54 - 0.46 * cos(2πn/(N-1))
     */
    private static double hamming(int smallN, int bigN) {
        if (bigN <= 1) return 1.0;
        return 0.54 - 0.46 * Math.cos((2.0 * Math.PI * smallN) / (bigN - 1));
    }

    /**
     * Approximate sum of squares of the Hamming window.
     * A more accurate approach is to precompute sum_{n=0..N-1} [hamming(n,N)^2].
     */
    private static double windowPowerHamming(int n) {
        // We can do a quick numeric sum for demonstration
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            double w = hamming(i, n);
            sum += w * w;
        }
        return sum;
    }

    public List<Double> getLfFreq() {
        return lfFreq;
    }

    public List<Double> getHfFreq() {
        return hfFreq;
    }

    public List<Double> getLfPsd() {
        return lfPsd;
    }

    public List<Double> getHfPsd() {
        return hfPsd;
    }

    private void calculateFrequencyAndPower() {
        for (int i = 0; i < freqs.length; i++) {
            double f = freqs[i];
            double p = psd[i];
            if (f >= LOW_FREQUENCY_LOWER_BOUND && f <= LOW_FREQUENCY_UPPER_BOUND) {
                lfFreq.add(f);
                lfPsd.add(p);
            } else if (f > HIGH_FREQUENCY_LOWER_BOUND && f <= HIGH_FREQUENCY_UPPER_BOUND) {
                hfFreq.add(f);
                hfPsd.add(p);
            }
        }
    }


}
