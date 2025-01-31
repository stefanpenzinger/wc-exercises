import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    private static final String PATH_PREFIX = "src/main/resources/";
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var patientInfoPath = PATH_PREFIX + "patient-info.csv";
        var outputCsvPath = "analysis_results.csv";
        var results = new ArrayList<String[]>();

        try (CSVReader reader = new CSVReader(new FileReader(patientInfoPath))) {
            var header = reader.readNext();
            if (header == null) {
                LOGGER.error("Empty CSV file {}", patientInfoPath);
                return;
            }

            var idxFile = -1;
            var idxAge = -1;
            var idxGender = -1;
            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase("File")) {
                    idxFile = i;
                } else if (header[i].equalsIgnoreCase("Age (years)")) {
                    idxAge = i;
                } else if (header[i].equalsIgnoreCase("Gender")) {
                    idxGender = i;
                }
            }

            if (idxFile < 0 || idxAge < 0) {
                LOGGER.error("Could not find 'File' or 'Age (years)' in CSV header.");
                return;
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                var result = parseLine(line, idxFile, idxAge, idxGender);

                if (result != null) {
                    results.add(result.toStringArray());
                }
            }

            // Write out results to CSV
            try (CSVWriter writer = new CSVWriter(new FileWriter(outputCsvPath))) {
                writer.writeAll(results);
            }

            LOGGER.info("Analysis complete. Results written to {}", outputCsvPath);

        } catch (IOException e) {
            LOGGER.error("IO Exception occurred {}", e.getMessage());
        } catch (CsvValidationException e) {
            LOGGER.error("CSV could not be validated {}", e.getMessage());
        }
    }

    private static AnalysisResult parseLine(String[] line, int idxFile, int idxAge, int idxGender) throws IOException {
        try {
            // Parse fields
            var fileId = line[idxFile];
            var age = convertToValidAgeOrThrow(line[idxAge]);
            var gender = Gender.fromValue(line[idxGender]);

            // Load HRV file
            var hrvFile = PATH_PREFIX + fileId + ".txt";
            pathExistsOrThrow(hrvFile);
            // Read RR data
            var hrvData = readHearRateVariabilityFile(hrvFile);

            // Compute metrics
            var rmssd = new RMSSD(hrvData);
            var welchResult = WelchResult.calculateWelchResult(hrvData);
            var rr = new RR(welchResult);
            var lfHf = new LFHF(welchResult);

            return new AnalysisResult(fileId, age, gender, rmssd.value(), rr.calculateRespirationRate(), lfHf.calculateLFHF());
        } catch (IllegalArgumentException iae) {
            LOGGER.debug("Skipping line. {}", iae.getMessage());
            return null;
        } catch (FileNotFoundException e) {
            LOGGER.info("Skipping line. {}", e.getMessage());
            return null;
        }
    }

    /**
     * Read HRV intervals from a plain text file (one value per line).
     */
    private static double[] readHearRateVariabilityFile(String filePath) throws IOException {
        var lines = Files.readAllLines(Paths.get(filePath));
        var hrv = new double[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (!line.isBlank()) {
                hrv[i] = Double.parseDouble(line.trim());
            }
        }
        return hrv;
    }

    private static double convertToValidAgeOrThrow(String ageString) throws IllegalArgumentException {
        try {
            double age = Double.parseDouble(ageString);
            if (age < 18) {
                throw new IllegalArgumentException("Age must be at least 18 years");
            }

            return age;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid age: " + ageString);
        }
    }

    private static void pathExistsOrThrow(String path) throws FileNotFoundException {
        if (!Files.exists(Paths.get(path))) {
            throw new FileNotFoundException("File not found: " + path);
        }
    }
}
