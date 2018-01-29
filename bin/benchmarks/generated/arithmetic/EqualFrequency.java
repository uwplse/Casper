package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EqualFrequency {
	public static boolean equalFrequency(JavaRDD<Integer> rdd_0_0) {
		int first = 0;
		first = 0;
		int second = 0;
		second = 0;
		Map<Integer, Tuple2<Integer, Integer>> output_rdd_0_0 = rdd_0_0.flatMapToPair(data_i -> {
      List<Tuple2<Integer, Tuple2<Integer, Integer>>> emits = new ArrayList<Tuple2<Integer, Tuple2<Integer, Integer>>>();
      if (data_i == 110) emits.add(new Tuple2(2, new Tuple2(2, 1)));
      if (data_i == 100) emits.add(new Tuple2(1, new Tuple2(1, 1)));
      return emits.iterator();
    }).reduceByKey((val1, val2) -> {
      if (val1._1 == 1) return new Tuple2(val1._1, (val2._2 + val1._2));
      else if (val1._1 == 2) return new Tuple2(val1._1, (val1._2 + val2._2));
      else return null;
    }).collectAsMap();
		second = output_rdd_0_0.get(2)._2;
		first = output_rdd_0_0.get(1)._2;
		boolean flat$7 = first == second;
		return flat$7;
	}
}