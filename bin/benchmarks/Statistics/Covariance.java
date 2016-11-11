package magpie;

import java.util.List;

public class Covariance {

    /**
     * covariance
     *                   Works out the fit of the data
     * @param xVariance
     *                   Array of doubles holding the x variance values
     * @param yVariance
     *                   Array of doubles holding the y variance values
     * @param x
     *                  Array of doubles holding the x values
     * @param y
     *                  Array of doubles holding the y values
     *
     * @return
     *                  The covariance as a double giving the fit difference of least squares
     */
    public static double covariance(double xVariance, double yVariance, double[] x, double[] y){
        double covariance=0.0;
        for (int i = 0; i < x.length; i++) covariance += (x[i] - xVariance) * (y[i] - yVariance);
        return covariance;
    }
}
