package magpie;

import java.util.List;

public class Variance {

    /**
     * variance
     *          Works out the difference of least squares fit
     * @param data
     *          The data being analysed
     * @param mean
     *          The mean of the data
     *
     * @return
     *          The sum of all the variances
     */
    public static double variance(double[] data, double mean){
        double variance = 0.0;
        for (int i = 0; i < data.length; i++) variance += Math.pow((data[i] - mean),2);
        return variance/data.length -1;
    }
}
