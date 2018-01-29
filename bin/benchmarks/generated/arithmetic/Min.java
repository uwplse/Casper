package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Min {
	public static int minList(JavaRDD<Integer> rdd_0_0){
		int min = 0;
		min = Integer.MAX_VALUE;
		min = rdd_0_0.reduce((val1, val2) -> (val2 > val1 ? val1 : val2));
		return min;
	}
}