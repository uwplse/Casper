package manual.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Sum {
	public static int sumList(JavaRDD<Integer> data){
		return data.reduce((a, b) -> a+b);
	}
}