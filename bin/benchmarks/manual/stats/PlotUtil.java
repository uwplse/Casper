package manual.stats;

import org.apache.spark.api.java.JavaDoubleRDD;
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
public class PlotUtil extends IOUtil{

    private JavaRDD<Element> data;

    private static class Element {
      double x;
      double y;
      public Element (double a, double b) { x = a; y = b; }
    }

    public PlotUtil(JavaRDD<Element> data){
        this.data=data;
    }

   /**
    * x
    *           Pull out the x column data
    * @return:
    *         The x component of a 1d array of doubles
    */
    public JavaDoubleRDD x(){
        return data.mapToDouble(e -> e.x);
    }

   /**
    * x
    *           Pull out the y column data
    * @return
    *           The y component of a 1d array of doubles
    */
    public JavaDoubleRDD y(){
      return data.mapToDouble(e -> e.y);
    }

   /**
    * removeOffset
    *              Convenience function to remove the y intecept offset value
    * @return
    *              The 2D data array of doubles with offset removed.
    */
    public static JavaRDD<Element> removeOffset(JavaRDD<Element> data, double offset){
        return data.map(e -> new Element(e.x + -offset, e.y));
    }

    public static void writeToFile(double[][] data, PrintWriter fileOut){
        for(int i=0; i<data.length;i++){
            fileOut.printf("%2.5f %2.5f", data[i][0], data[i][1]);
            fileOut.println();
        }
    }
}