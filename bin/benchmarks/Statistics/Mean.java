package magpie;

import java.util.List;

public class Mean {

    /**
     * mean
     *                   Works out the fit of the data
     * @param data
     *                   Array of doubles holding the x and y values in [i][0] and [j][1] respectively
     *
     * @return
     *                  The covariance as a double giving the fit difference of least squares
     */
	public static double mean(List<Double> data){
        double sum = 0.0;
        for(int i=0;i< data.size(); i++) sum += data.get(i);
        return sum / (double) data.size() - 1;
    }
}
