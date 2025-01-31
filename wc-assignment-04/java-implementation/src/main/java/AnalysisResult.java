import java.io.Serializable;

public record AnalysisResult(String fileId, double age, Gender gender, double rmssd, double respirationRateBpm,
                             double lfHfRatio) implements Serializable {

    public String[] toStringArray() {
        return new String[]{
                fileId, String.valueOf(age),
                gender.toString(),
                String.valueOf(rmssd),
                String.valueOf(respirationRateBpm),
                String.valueOf(lfHfRatio)
        };
    }
}
