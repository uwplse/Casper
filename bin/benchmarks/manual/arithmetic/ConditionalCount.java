package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class ConditionalCount {
	public static Integer countList(JavaRDD<Integer> data){
		return data.aggregate(
				0,
				(count, a) -> (a < 100 ? count + 1 : count),
				(c1, c2) -> c1 + c2
		);
	}
}