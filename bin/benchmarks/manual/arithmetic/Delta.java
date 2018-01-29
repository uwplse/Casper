package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class Delta {
	public static int deltaList(JavaRDD<Integer> data){
    Tuple2<Integer,Integer> minmax = data.aggregate(
        new Tuple2<Integer,Integer>(Integer.MIN_VALUE,Integer.MAX_VALUE),
        (res, val) -> new Tuple2(Math.max(res._1,val), Math.min(res._2,val)),
        (res1, res2) -> new Tuple2(Math.max(res1._1,res2._1), Math.min(res1._2, res2._2)));
    return minmax._1 - minmax._2;
	}
}