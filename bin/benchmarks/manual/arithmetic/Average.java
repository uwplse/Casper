package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class Average {
	
	public static double avgList(JavaRDD<Integer> data){
		Tuple2<Integer,Integer> sumcount = data.aggregate(
				new Tuple2<Integer,Integer>(0,0),
				(res, val) -> new Tuple2(res._1 + val, res._2 + 1),
				(res1, res2) -> new Tuple2(res1._1 + res2._1, res1._2 + res2._2));
		return sumcount._1 / sumcount._2;
	}
}