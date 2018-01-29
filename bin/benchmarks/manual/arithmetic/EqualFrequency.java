package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class EqualFrequency {
	public static boolean equalFrequency(JavaRDD<Integer> data){
		Tuple2<Integer,Integer> freq = data.aggregate(
        new Tuple2<Integer, Integer>(0, 0),
        (res, val) -> new Tuple2((val == 100? res._1 + 1 : res._1), (val == 110? res._2 + 1 : res._2)),
        (res1, res2) -> new Tuple2(res1._1 + res2._1, res1._2 + res2._2));
		return freq._1 == freq._2;
	}
}