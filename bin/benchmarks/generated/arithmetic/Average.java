package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

public class Average {
	
	public static double avgList(JavaRDD<Integer> rdd_0_0){
		int sum = 0;
		sum = 0;
		int count = 0;
		count = 0;
		Tuple2<Integer, Integer> output_rdd_0_0 = rdd_0_0.map(data_i -> new Tuple2<Integer,Integer>(data_i,1)).reduce((val1, val2) -> new Tuple2((val1._1+val2._1),(val1._2+val2._2)));
		count = output_rdd_0_0._2;
		sum = output_rdd_0_0._1;
		int flat$6 = sum / count;
		return flat$6;
	}
}