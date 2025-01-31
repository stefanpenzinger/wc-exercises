import java.io.Serializable;

public enum Gender implements Serializable {
    MALE("M"), FEMALE("F"), NOT_AVAILABLE("");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    public static Gender fromValue(String value) {
        return switch (value) {
            case "M" -> MALE;
            case "F" -> FEMALE;
            default -> NOT_AVAILABLE;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
