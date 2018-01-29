package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class ConditionalSum {
	public static Integer sumList(JavaRDD<Integer> data){
		return data.aggregate(
				0,
				(sum, a) -> (a < 100 ? sum + a : sum),
				(s1, s2) -> s1 + s2
		);
	}
}