package at.hagenberg.fh.wc;

import at.hagenberg.fh.wc.helper.PolynomialHelper;
import at.hagenberg.fh.wc.model.SensorData;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final ClassLoader classLoader = Main.class.getClassLoader();

    public static void main(String[] args) {
        var filename = "data/data2.csv";
        LOGGER.info("Loading data from {}", filename);

        File file = null;
        try {
            file = new File(Objects.requireNonNull(classLoader.getResource(filename)).toURI());
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage());
        }

        var samplingFrequency = 100f;
        var windowSize = 512;
        var slidingWindowDuration = 1.25f;
        var resolution = samplingFrequency / windowSize;
        var slidingWindowLength = slidingWindowDuration * samplingFrequency;
        var fft = new FastFourierTransformer(DftNormalization.STANDARD);

        var values = readCSV(file);
        double stepCount = 0;

        for (int i = 0; (i + windowSize) < values.size(); i += (int) slidingWindowLength) {
            var window = values.subList(i, i + windowSize);
            var idx = mostSensitiveAxisIndex(window, windowSize);

            double[] axisData = new double[windowSize];
            for (int j = 0; j < windowSize; j++) {
                switch (idx) {
                    case 0 -> axisData[j] = window.get(j).wx();  // wx axis
                    case 1 -> axisData[j] = window.get(j).wy();  // wy axis
                    case 2 -> axisData[j] = window.get(j).wz();  // wz axis
                    default -> LOGGER.error("Unexpected value {}", idx);
                }
            }

            var fftResult = fft.transform(axisData, TransformType.FORWARD);
            var magnitudes = magnitudesFromFFT(fftResult);
            var coefficients = PolynomialHelper.fit(magnitudes);

            // Get the first two (w0) and next five (wc) components
            var w0 = (magnitudes[0] + magnitudes[1]) / 2;
            var wc = 0d;
            for (int j = 2; j < 7; j++) {
                wc += magnitudes[j];
            }
            wc /= 5;

            // Step detection logic
            if (wc > w0 && wc > 10) {
                double boundedMinimizationMax = PolynomialHelper.goldenSectionSearch(coefficients, 1, 5, 1e-5);
                double fw = resolution * (boundedMinimizationMax + 1);
                double c = slidingWindowDuration * fw;

                var prevStepCount = stepCount;
                stepCount += c;
                LOGGER.info("Increase step count from {} to {} by {}", prevStepCount, stepCount, c);
            }
        }

        LOGGER.info("{} steps were made.", stepCount);
    }

    private static double[] magnitudesFromFFT(Complex[] fftResult) {
        var magnitudes = new double[fftResult.length];
        for (int j = 0; j < fftResult.length; j++) {
            magnitudes[j] = 2 * fftResult[j].abs();
        }

        return magnitudes;
    }

    private static int mostSensitiveAxisIndex(List<SensorData> window, int windowSize) {
        double[] meanAbs = new double[3];
        for (SensorData entry : window) {
            meanAbs[0] += Math.abs(entry.wx());
            meanAbs[1] += Math.abs(entry.wy());
            meanAbs[2] += Math.abs(entry.wz());
        }
        for (int j = 0; j < 3; j++) {
            meanAbs[j] /= windowSize;
        }

        // Find the axis with the maximum mean absolute value
        int idx = 0;
        double maxMeanAbs = meanAbs[0];
        for (int j = 1; j < 3; j++) {
            if (meanAbs[j] > maxMeanAbs) {
                idx = j;
                maxMeanAbs = meanAbs[j];
            }
        }

        return idx;
    }

    /**
     * Reads in data from a CSV file with the sperator 's'
     *
     * @param file The file defined in the code
     * @return The sensor data
     */
    private static List<SensorData> readCSV(File file) {
        String line;
        String delimiter = ",";
        List<SensorData> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Skip the header line (time, wx, wy, wz)
            var firstLineSkipped = false;

            while ((line = br.readLine()) != null) {
                if (!firstLineSkipped) {
                    firstLineSkipped = true;
                    continue;
                }
                String[] values = line.split(delimiter);

                double time = Double.parseDouble(values[0]);
                double wx = Double.parseDouble(values[1]);
                double wy = Double.parseDouble(values[2]);
                double wz = Double.parseDouble(values[3]);

                SensorData sensorData = new SensorData(time, wx, wy, wz);
                data.add(sensorData);
            }
        } catch (IOException e) {
            LOGGER.error("{}", e.getMessage());
        }

        return data;
    }
}