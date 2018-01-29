package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Max {
	public static int maxList(JavaRDD<Integer> data){
		return data.reduce((a, b) -> Math.max(a, b));
	}
}