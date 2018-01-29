package generated.phoenix;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple5;

public class LinearRegression {
    public static class Point {
        public int x;
        public int y;
        
        public Point(int x, int y) {
            super();
            this.x = x;
            this.y = y;
        }
    }
    
    public static int[] regress(JavaRDD<Point> rdd_0_0) {
        int SX_ll = 0;
        int SY_ll = 0;
        int SXX_ll = 0;
        int SYY_ll = 0;
        int SXY_ll = 0;
        int i = 0;
        Tuple5<Integer,Integer,Integer,Integer,Integer> rdd_0_0_output = rdd_0_0.map(points_i -> new Tuple5<Integer,Integer,Integer,Integer,Integer>(points_i.x*points_i.y,points_i.x*points_i.x,points_i.y,points_i.y*points_i.y,points_i.x)).reduce((v1,v2) -> new Tuple5<Integer,Integer,Integer,Integer,Integer>(v1._1()+v2._1(),v1._2()+v2._2(),v1._3()+v2._3(),v1._4()+v2._4(),v1._5()+v2._5()));
        SXY_ll = rdd_0_0_output._1();
        SXX_ll = rdd_0_0_output._2();
        SY_ll = rdd_0_0_output._3();
        SYY_ll = rdd_0_0_output._4();
        SX_ll = rdd_0_0_output._5();
        int[] result = (int[]) (new int[5]);
        ((int[]) result)[0] = SX_ll;
        ((int[]) result)[1] = SXX_ll;
        ((int[]) result)[2] = SY_ll;
        ((int[]) result)[3] = SYY_ll;
        ((int[]) result)[4] = SXY_ll;
        return (int[]) result;
    }
    
    public LinearRegression() { super(); }
}
