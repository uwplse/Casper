package magpie;

import java.util.List;

public class RegressionSumOfSquares {

    /**
     * regressionSOS
     *                  Regression sum of squares
     * @param fit
     *                  The the least squares fit as an array of doubles
     * @param yMean
     *                  Array of doubles holding the mean y values
     * @return
     *                  The regression sum of squares as a double
     */
    public static double regressionSumOfSquares(double[] fit, double yMean){
        double ssr = 0.0;
        for (int i = 0; i < fit.length; i++) ssr += (fit[i] - yMean) * (fit[i] - yMean);
        return ssr;
    }
}
