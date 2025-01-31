import java.util.List;

public record LFHF(WelchResult welchResult) {
    public double calculateLFHF() {
        var lfFreq = welchResult.getLfFreq();
        var lfPsd = welchResult.getLfPsd();
        var hfFreq = welchResult.getHfFreq();
        var hfPsd = welchResult.getHfPsd();

        var lfPower = trapz(lfFreq, lfPsd);
        var hfPower = trapz(hfFreq, hfPsd);

        return (hfPower > 0) ? lfPower / hfPower : Double.POSITIVE_INFINITY;
    }

    /**
     * Trapezoidal integration: \int y dx over x,y arrays
     */
    private double trapz(List<Double> xList, List<Double> yList) {
        var area = 0.0;
        for (int i = 0; i < xList.size() - 1; i++) {
            var x0 = xList.get(i);
            var x1 = xList.get(i + 1);
            var y0 = yList.get(i);
            var y1 = yList.get(i + 1);
            var base = x1 - x0;
            var avgHeight = (y0 + y1) / 2.0;
            area += base * avgHeight;
        }
        return area;
    }
}
