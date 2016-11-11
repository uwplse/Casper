package magpie;

import java.util.List;

public class Residuals {

    /**
     * residuals
     *                  Difference between y from the least squares fit and the actual data
     * @param y
     *                  Array of doubles holding the y values
     * @param fit
     *                  The the least squares fit as an array of doubles
     * @return
     *                  Array of doubles holding the data's residual points
     */
    public static double[] residuals(double[] y, double[] fit){

        double[] residuals=new double[y.length];
        for (int i = 0; i < y.length; i++)
            residuals[i] = y[i] - fit[i];
        return residuals;
    }
}
