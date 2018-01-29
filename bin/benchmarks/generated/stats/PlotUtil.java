package generated.stats;

import org.apache.spark.api.java.JavaRDD;

import java.io.PrintWriter;

/**

 *   PlotUtil.java

 *   =============

 *

 *  Copyright (C) 2013-2014  Magdalen Berns <m.berns@sms.ed.ac.uk>

 *

 *  This program is free software: you can redistribute it and/or modify

 *  it under the terms of the GNU General Public License as published by

 *  the Free Software Foundation, either version 3 of the License, or

 *  (at your option) any later version.

 *

 *  This program is distributed in the hope that it will be useful,

 *  but WITHOUT ANY WARRANTY; without even the implied warranty of

 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *  GNU General Public License for more details.



 *  You should have received a copy of the GNU General Public License

 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
public class PlotUtil extends IOUtil {
  private double[] x;
  private double[] y;
  private JavaRDD<double[]> data;

  public PlotUtil(JavaRDD<double[]> rdd_0_0) {
    super();
    this.data = (JavaRDD<double[]>) data;
    x = (double[]) (new double[(int)rdd_0_0.count()]);
    y = (double[]) (new double[(int)rdd_0_0.count()]);
  }

  /**

   * x

   *           Pull out the x column data

   * @return:

   *         The x component of a 1d array of doubles

   */
  public JavaRDD<Double> x(){
    return data.map(data_i -> data_i[0]);
  }

  /**

   * x

   *           Pull out the y column data

   * @return

   *           The y component of a 1d array of doubles

   */
  public JavaRDD<Double> y(){
    return data.map(data_i -> data_i[1]);
  }

  /**

   * removeOffset

   *              Convenience function to remove the y intecept offset value

   * @return

   *              The 2D data array of doubles with offset removed.

   */
  public static JavaRDD<double[]> removeOffset(JavaRDD<double[]> rdd_1_0, double offset){
    return rdd_1_0.map(data_i -> {
      data_i[0] = data_i[0] - offset;
      return data_i;
    });
  }

  public static void writeToFile(double[][] data, PrintWriter fileOut) {
    int i = 0;
    while (i < ((double[][]) data).length) {
      ((PrintWriter) fileOut).
              printf("%2.5f %2.5f",
                      (Object[])
                              (new Object[] { Double.valueOf(((double[])
                                      ((double[][])
                                              data)[i])[0]),
                                      Double.valueOf(((double[])
                                              ((double[][]) data)[i])[1]) }));
      ((PrintWriter) fileOut).println();
      i++;
    }
  }
}