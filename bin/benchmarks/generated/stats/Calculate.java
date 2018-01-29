package generated.stats;

import org.apache.spark.api.java.JavaDoubleRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

/**

 * Calculate.java

 * =============

 *

 * Copyright (C) 2013-2014  Magdalen Berns <m.berns@sms.ed.ac.uk>

 *

 * This program is free software: you can redistribute it and/or modify

 * it under the terms of the GNU General Public License as published by

 * the Free Software Foundation, either version 3 of the License, or

 * (at your option) any later version.

 *

 * This program is distributed in the hope that it will be useful,

 * but WITHOUT ANY WARRANTY; without even the implied warranty of

 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 * GNU General Public License for more details.

 *

 *  You should have received a copy of the GNU General Public License

 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
public class Calculate {
  private double num1;
  private double num;

  public Calculate() {
    super();
    num1 = 0.0;
    num = 0.0;
  }

  /**

   * subtract

   *            Method to subtract one double from another

   * @param a

   *            double value to subtract b from

   * @param b

   *            double value to be subtracted from a

   * @return

   *            Double result of subtracting b from a

   */
  public static double subtract(double a, double b) { return a - b; }

  /**

   * add

   *            Method to add one double to another

   * @param a

   *            double value to be added to b

   * @param b

   *            double value to be added to a

   * @return

   *            Double result of adding a to b

   */
  public static double add(double a, double b) { return a + b; }

  /**

   * divide

   *            Method to divide one double over another

   * @param a

   *            double value to be divided by b

   * @param b

   *            double value to divide a by

   * @return

   *            Double result of dividing a from b

   */
  public static double divide(double a, double b) { return a / b; }

  /**

   * multiply

   *           Method to multiply one method to another

   * @param a

   *           Double value to be multiplied by b

   * @param b

   *           Double value to multiplied by a

   * @return

   *           Double result of a * b

   */
  public static double multiply(double a, double b) { return a * b; }

  /**

   * subtract

   *            Instance method to subtract one double from another

   * @param num2

   *            double value to be subtracted from num1

   * @return

   *            Double result of subtracting  num1 to num2

   */
  public double subtract(double num2) { return num1 - num2; }

  /**

   * add

   *            Instance method to add one double to another

   * @param num2

   *            double value to be added to num1

   * @return

   *            Double result of adding num1 to num2

   */
  public double add(double num2) { return num1 + num2; }

  public double divide(double num2) { return num1 / num2; }

  public double multiply(double num2) { return num1 * num2; }

  public static JavaRDD<Double> multiply(JavaDoubleRDD rdd_0_0, double b){
    return rdd_0_0.map(a_i -> a_i * b);
  }

  public static JavaRDD<Double> multiply(JavaDoubleRDD rdd_1_0, JavaDoubleRDD rdd_1_1){
    JavaPairRDD<Double, Double> rdd_1_2 = rdd_1_0.zip(rdd_1_1);
    return rdd_1_2.map(casper_dataset_i -> casper_dataset_i._2 * casper_dataset_i._1);
  }
}
