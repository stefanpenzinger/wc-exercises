public record RR(WelchResult welchResult) {
    public double calculateRespirationRate() {
        var hfFreq = welchResult.getHfFreq();
        var hfPsd = welchResult.getHfPsd();

        var peakFreq = 0.0;
        var maxPower = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < hfFreq.size(); i++) {
            if (hfPsd.get(i) > maxPower) {
                maxPower = hfPsd.get(i);
                peakFreq = hfFreq.get(i);
            }
        }

        return peakFreq * 60.0;
    }
}
