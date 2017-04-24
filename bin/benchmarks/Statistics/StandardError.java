package magpie;

import java.util.List;

public class StandardError {

    /**
     * standardError
     *                  Gives the residual sum of squares.
     * @param y
     *                  Array of doubles holding the y values
     *
     * @param fit
     *                  The the least squares fit as an array of doubles
     * @return
     *                  Standard error in mean i.e. residual sum of squares
     *                  as a double
     */
    public static double standardError(double[] y, double[] fit){
        double rss = 0.0;
        for (int i = 0; i < y.length; i++)
            rss += (fit[i] - y[i]) * (fit[i] - y[i]);
        return rss;
    }
}
