package magpie;

import java.util.List;

public class Fit {

    /**
     * fit
     *                   Works out the fit of the data
     * @param x
     *                   Array of doubles holding the x values
     * @param gradient
     *                  The gradient
     * @param offset
     *                  The offset constant value on the y axis
     * @return
     *                  The the least squares fit as an array of doubles
     */
    public static double[] fit(double[] x, double gradient, double offset){
        double[] fit=new double[x.length];
        for(int i = 0; i < x.length; i++) fit[i] = gradient*x[i] + offset;
        return fit;
    }
}
