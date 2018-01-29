package generated.stats;

import com.google.common.primitives.Doubles;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

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
public class StatsUtil {
    private static final double period = 2 * Math.PI;

    /**

     * mean

     *                   Works out the fit of the data

     * @param data

     *                   Array of doubles holding the x and y values in [i][0]
    and [j][1] respectively

     *

     * @return

     *                  The covariance as a double giving the fit difference of
    least squares

     */
    public static double mean(JavaRDD<Double> rdd_0_0) {
        double sum = 0.0;
        sum = rdd_0_0.reduce((v1,v2) -> v2 + v1);
        return sum / (double) rdd_0_0.count() - 1;
    }

    /**

     * covariance

     *                   Works out the fit of the data

     * @param xVariance

     *                   Array of doubles holding the x variance values

     * @param yVariance

     *                   Array of doubles holding the y variance values

     * @param data

     *                   Array of doubles holding the x and y values in [i][0]
    and [j][1] respectively

     *

     * @return

     *                  The covariance as a double giving the fit difference of
    least squares

     */
    public static double covariance(double xVariance, double yVariance,
                                    JavaRDD<double[]> rdd_1_0){
        double covariance=0.0;
        covariance =  rdd_1_0.map(data_i -> (data_i[0] - xVariance) * (data_i[1] - yVariance)).reduce((v1,v2) -> v1 + v2);
        return covariance;
    }

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

     *                  The covariance as a double giving the fit difference of
    least squares

     */
    public static double covariance(double xVariance, double yVariance,
                                    JavaRDD<Double> rdd_2_0, JavaRDD<Double> rdd_2_1){
        double covariance=0.0;
        JavaPairRDD<Double,Double> rdd_2_2 = rdd_2_0.zip(rdd_2_1);
        covariance = rdd_2_2.map(casper_dataset_i -> (casper_dataset_i._1 - xVariance) * (casper_dataset_i._2 - yVariance)).reduce((v1,v2) -> v1 + v2);
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
    public static double variance(JavaRDD<Double> rdd_3_0, double mean) {
        double variance = 0.0;
        variance = rdd_3_0.map(data_i -> Math.pow(data_i - mean, 2)).reduce((v1,v2) -> v2 + v1);
        return variance / rdd_3_0.count() - 1;
    }

    /**

     * standardDeviation

     *                   Works out the standard deviation of least squares fit

     * @param variance

     *                   The variance of the data being analysed

     * @param n

     *                   The integer length of the data array

     *

     * @return

     *                   The the standard deviation of least squares fit as a
    double

     */
    public static double standardDeviation(double variance, int n) {
        double stdDev = 0.0;
        if (n > 0) stdDev = Math.sqrt(variance / n);
        return stdDev;
    }

    /**

     * gradient

     *                   Works out the standard deviation of least squares fit

     * @param covariance

     *                   The covariance of the data being analysed

     * @param xVariance

     *                   The integer length of the data array

     * @return

     *          The the standard deviation of least squares fit as a double

     */
    public static double gradient(double covariance, double xVariance) {
        return covariance / xVariance;
    }

    /**

     * yIntercept

     *                   Works out the offset of the data (i.e the constant
     value by which y is offset)

     * @xMean

     *                   The mean value of the x coordinate of the data being
    analysed

     * @yMean

     *                   The mean value of the y coordinate of the data being
    analysed

     * @gradient

     *                  The gradient

     * @return

     *          The the standard deviation of least squares fit as a double

     */
    public static double yIntercept(double xMean, double yMean,
                                    double gradient) {
        return yMean - gradient * xMean;
    }

    /**

     * fit

     *                   Works out the fit of the data

     * @param data

     *                   Array of doubles holding the x and y values in [i][0]
    and [j][1] respectively

     * @param gradient

     *                  The gradient

     * @param offset

     *                  The offset constant value on the y axis

     * @return

     *                  The the least squares fit as an array of doubles

     */
    public static JavaRDD<Double> fit(JavaRDD<double[]> rdd_4_0, double gradient,
                                      double offset){
        return rdd_4_0.map(data_i -> gradient*data_i[0]+offset);
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
    public static JavaRDD<Double> fit(JavaRDD<Double> rdd_5_0, double gradient,
                                      double offset){
        return rdd_5_0.map(x_i -> offset + x_i * gradient);
    }

    /**

     * standardError

     *                  Gives the residual sum of squares.

     * @param data

     *                  Array of doubles holding the x and y values in [i][0]
    and [j][1] respectively

     * @param fit

     *                  The the least squares fit as an array of doubles

     * @return

     *                  Standard error in mean of y as a double value

     */
    public static double standardError(JavaRDD<double[]> rdd_6_0, JavaRDD<Double> rdd_6_1){
        double rss = 0.0;
        JavaPairRDD<double[],Double> rdd_6_2 = rdd_6_0.zip(rdd_6_1);
        rss = rdd_6_2.map(casper_dataset_i -> (casper_dataset_i._1[1] - casper_dataset_i._2) * (casper_dataset_i._1[1] - casper_dataset_i._2)).reduce((v1,v2) -> v1 + v2);
        return rss;
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
    public static double standardError(JavaRDD<Double> rdd_7_0, JavaRDD<Double> rdd_7_1){
        double rss = 0.0;
        JavaPairRDD<Double,Double> rdd_7_2 = rdd_7_0.zip(rdd_7_1);
        rss = rdd_7_2.map(casper_dataset_i -> (casper_dataset_i._2 - casper_dataset_i._1) * (casper_dataset_i._2 - casper_dataset_i._1)).reduce((v1,v2) -> v1 + v2);
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
    public static double regressionSumOfSquares(JavaRDD<Double> rdd_8_0, double yMean) {
        double ssr = 0.0;
        ssr = rdd_8_0.map(fit_i -> (yMean - fit_i) * (yMean - fit_i)).reduce((v1,v2) -> v2 + v1);
        return ssr;
    }

    /**

     * residuals

     *                  Difference between y from the least squares fit and the
     actual data

     * @param y

     *                  Array of doubles holding the y values

     * @param fit

     *                  The the least squares fit as an array of doubles

     * @return

     *                  Array of doubles holding the data's residual points

     */
    public static JavaRDD<Double> residuals(JavaRDD<Double> rdd_9_0, JavaRDD<Double> rdd_9_1) {
        JavaPairRDD<Double, Double> rdd_9_2 = rdd_9_0.zip(rdd_9_1);
        return rdd_9_2.map(casper_dataset_i -> casper_dataset_i._1 - casper_dataset_i._2);
    }

    /**

     * linearCorrelationCoefficient

     *                              Linear correlation coefficient found when we
     calculate the regression sum of

     *                              squares over the variance given in y

     * @param regressionSumOfSquares

     *                              Standard error of mean from having
    calculated the regression sum of squares

     * @param yVariance

     *                              Variance in y as an array of double values



     * @return

     *                              Linear correlation coefficient of data as a
    double

     */
    public static double linearCorrelationCoefficient(
            double regressionSumOfSquares, double yVariance) {
        return regressionSumOfSquares / yVariance;
    }

    /**

     * errorOffset

     *              Gives the error in the offset

     * @param n

     *              Integer length of the array of data doubles

     * @param xVariance

     *              Variance in x as an array of double values

     * @param xMean

     *              Array of doubles holding the mean x values

     * @param rss

     *              The regression sum of squares as a double

     * @return

     *              Error in the y offset constant value of the line of best fit
    as a double

     */
    public static double errorOffset(double n, double xVariance, double xMean,
                                     double rss) {
        double degreesFreedom = n - 2;
        double sigma = rss / degreesFreedom;
        double sVariance = sigma / xVariance;
        return Math.sqrt(sigma / n + Math.pow(xMean, 2) * sVariance);
    }

    /**

     * errorGradient

     *                  Gives the error in the gradient

     * @param xVariance

     *                  Double holding the value of the variance in x

     * @param rss

     *                  Standard error in the mean of x

     * @return

     *                  Error in the line of best fit's gradient calculation as
    a double

     */
    public static double errorGradient(double xVariance, double rss, int n) {
        double degreesFreedom = n - 2;
        double stdVariance = rss / degreesFreedom;
        return Math.sqrt(stdVariance / xVariance);
    }

    public static double errorFit(double stdFit) { return Math.sqrt(stdFit); }

    /**

     * gaussian

     *                         a normalised gaussian to reflect the distribution
     of the data

     * @param numberOfSamples

     *                         sample number appropriate to size of original
    data array

     * @param variance

     *                         variance of data

     * @param mean

     *                         mean of data

     * @return

     *                         normalised gaussian in the form of 1D array of
    doubles for y axis

     */
    public static JavaRDD<Double> gaussian(int numberOfSamples, double variance,
                                    double mean) {
        double[] gaussian = (double[]) (new double[numberOfSamples]);
        double tempGaussian = 0.0;
        int i = 0;
        while (i < numberOfSamples) {
            ((double[]) gaussian)[i] = Math.sqrt(1 / period * variance) *
                    Math.exp(-(i - mean) * (i - mean) /
                            (2 * variance));
            tempGaussian += ((double[]) gaussian)[i];
            i++;
        }
        i = 0;
        JavaSparkContext sc = new JavaSparkContext();
        JavaRDD<Double> rdd_10_0 = sc.parallelize(Doubles.asList(gaussian));
        final double tempGaussian_final = tempGaussian;
        return rdd_10_0.map(gaussian_i -> gaussian_i / tempGaussian_final);
    }

    /**

     * convolve

     *                        convolve data with a normalised gaussian in order
     to smooth the output after

     *                        reducing sample rate

     * @param data

     *                         2D data array to be smoothed

     * @param gaussian

     *                         normalised gaussian in the form of 1D array of
    doubles for y axis

     * @param numberOfSamples

     *                         sample number appropriate to size of original
    data array

     * @return

     *                        Smoothed data as array of doubles

     */
    public static double[] convolve(int[] data, double[] gaussian,
                                    int numberOfSamples) {
        double[] convolved = (double[])
                (new double[((int[]) data).length -
                        (numberOfSamples + 1)]);
        int i = 0;
        while (i < ((double[]) convolved).length) {
            ((double[]) convolved)[i] = 0.0;
            int j = i;
            int k = 0;
            while (j < i + numberOfSamples) {
                ((double[]) convolved)[i] += ((int[]) data)[j] *
                        ((double[]) gaussian)[k];
                j++;
                k++;
            }
            i++;
        }
        return (double[]) convolved;
    }

    public StatsUtil() { super(); }
}