package generated.arithmetic;

import org.apache.spark.api.java.JavaRDD;

public class Count {
	public static Integer countList(JavaRDD<Integer> rdd_0_0){
		int count = 0;
		count = 0;
		count = rdd_0_0.map(data_i -> 1).reduce((val1, val2) -> (val1+val2));
		return count;
	}
}
