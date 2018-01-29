package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Max {
	public static int maxList(JavaRDD<Integer> rdd_0_0){
		int max = 0;
		max = Integer.MIN_VALUE;
		max = rdd_0_0.reduce((val1, val2) -> (val2 > val1 ? val2 : val1));
		return max;
	}
}