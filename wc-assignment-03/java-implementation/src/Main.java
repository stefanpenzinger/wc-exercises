public class Main {
    public static void main(String[] args) {
        // Suppose you have real values from somewhere:
        double age = 37.0;
        double vo2max = 50.13;
        double weight = 69.0;
        double sex = 1.0;
        double hr = 120.0;

        // Put them in the correct order:
        double[] features = new double[] {age, vo2max, weight, sex, hr};

        // Call the generated model’s score(...)
        double prediction = Model.score(features);

        System.out.println("Predicted Energy Expenditure (EE): " + prediction);
    }
}
