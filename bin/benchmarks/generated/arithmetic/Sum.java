package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Sum {
	public static int sumList(JavaRDD<Integer> rdd_0_0){
		int sum = 0;
		sum = 0;
		sum = rdd_0_0.reduce((val1, val2) -> (val1 + val2));
		return sum;
	}
}