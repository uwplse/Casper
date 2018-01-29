package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class Delta {
	public static Integer calcDelta(JavaRDD<Integer> rdd_0_0){
		int max = 0;
		max = Integer.MIN_VALUE;
		int min = 0;
		min = Integer.MAX_VALUE;
		Tuple2<Integer,Integer> output_rdd_0_0 = rdd_0_0.map(data_i -> new Tuple2<Integer,Integer>(data_i,data_i)).reduce((val1, val2) -> new Tuple2((val1._1 < val2._1 ? val1._1 : val2._1), (val1._2 < val2._2 ? val2._2 : val1._2)));
		max = output_rdd_0_0._2;
		min = output_rdd_0_0._1;
		int flat$7 = max-min;
		return flat$7;
	}
}