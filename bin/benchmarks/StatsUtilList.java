package magpie;

import java.util.List;

/**
 * StatsUtil.java
 * ==============
 *
 * This file is a part of a program which serves as a utility for data analysis
 * of experimental data
 *
 * Copyright (C) 2012-2014  Magdalen Berns <m.berns@sms.ed.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class StatsUtilList {

    private static final double period = 2 * Math.PI;
    /**
     * mean
     *                   Works out the fit of the data
     * @param data
     *                   Array of doubles holding the x and y values in [i][0] and [j][1] respectively
     *
     * @return
     *                  The covariance as a double giving the fit difference of least squares
     */
    /*public static double mean(List<Double> data){
        double sum = 0.0;
        for(int i=0;i< data.size(); i++) sum += data.get(i);
        return sum / (double) data.size() - 1;
    }

    /**
     * covariance
     *                   Works out the fit of the data
     * @param xVariance
     *                   Array of doubles holding the x variance values
     * @param yVariance
     *                   Array of doubles holding the y variance values
     * @param data
     *                   Array of doubles holding the x and y values in [i][0] and [j][1] respectively
     *
     * @return
     *                  The covariance as a double giving the fit difference of least squares
     */
    /*public static double covariance(double xVariance, double yVariance, List<List<Double>> data){
        double covariance=0.0;
        for (int i = 0; i < data.size(); i++) covariance += (data.get(i).get(0) - xVariance) * (data.get(i).get(1) - yVariance);
        return covariance;
    }

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
    /*public static double variance(List<Double> data, double mean){
        double variance = 0.0;
        for (int i = 0; i < data.size(); i++) variance += Math.pow((data.get(i) - mean),2);
        return variance/data.size() -1;
    }

    /**
     * fit
     *                   Works out the fit of the data
     * @param data
     *                   Array of doubles holding the x and y values in [i][0] and [j][1] respectively
     * @param gradient
     *                  The gradient
     * @param offset
     *                  The offset constant value on the y axis
     * @return
     *                  The the least squares fit as an array of doubles
     */
    /*class Point{
    	double x;
    	double y;
    }
    public static double[] fit(List<Point> data, double gradient, double offset){
        double[] fit=new double[data.size()];
        for(int i=0; i<data.size(); i++)
            fit[i] = gradient*data.get(i).x + offset;
        return fit;
    }

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
   /* public static double[] fit(double gradient, double offset, List<Double> x){
        double[] fit=new double[x.size()];
        for(int i = 0; i < x.size(); i++) fit[i] = gradient*x.get(i) + offset;
        return fit;
    }

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
    /*class RecordError {
    	public double y;
    	public double fit;
    }
    public static double standardError(List<RecordError> records){
        double rss = 0.0;
        for (int i = 0; i < records.size(); i++) 
        	rss += (records.get(i).fit - records.get(i).y) * (records.get(i).fit - records.get(i).y);
        return rss;
    }

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
    public static double regressionSumOfSquares(List<Double> fit, double yMean){
        double ssr = 0.0;
        for (int i = 0; i < fit.size(); i++) ssr += (fit.get(i) - yMean) * (fit.get(i) - yMean);
        return ssr;
    }

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
 	/*public static double[] residuals(List<Double> y, List<Double> fit){
        double[] residuals=new double[y.size()];
        for (int i = 0; i < y.size(); i++)
            residuals[i] = y.get(i) - fit.get(i);
        return residuals;
	}
    
   	class RecordResidual {
    	public double y;
    	public double fit;
    }
    public static double[] residuals(List<RecordResidual> data){
        double[] residuals=new double[data.size()];
        for (int i = 0; i < data.size(); i++)
            residuals[i] = data.get(i).y - data.get(i).fit;
        return residuals;
    }*/
}
