package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class MaxAbs {
	public static int maxAbsList(JavaRDD<Integer> data){
		return data.reduce((a, b) -> Math.max(Math.abs(a), Math.abs(b)));
	}
}