package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class MaxAbs {
	public static int maxAbsList(JavaRDD<Integer> rdd_0_0){
		int max = 0;
		max = Integer.MIN_VALUE;
		max = rdd_0_0.reduce((val1, val2) -> (Math.abs(val1) > Math.abs(val2) ? val1 : val2));
		return max;
	}
}